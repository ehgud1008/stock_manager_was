package com.example.stockanalysis.unified.report;

import static com.example.stockanalysis.unified.report.ReportModels.*;
import com.example.stockanalysis.unified.scan.UnifiedScanModels.Run;
import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReportStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ObjectMapper mapper;
    private final com.example.stockanalysis.storage.SnapshotRepository snapshots;
    public ReportStore(DataSource source,ObjectMapper mapper){this(source,mapper,null);}
    @org.springframework.beans.factory.annotation.Autowired
    public ReportStore(DataSource source,ObjectMapper mapper,com.example.stockanalysis.storage.SnapshotRepository snapshots){jdbc=new JdbcTemplate(source);tx=new TransactionTemplate(new DataSourceTransactionManager(source));this.mapper=mapper;this.snapshots=snapshots;}
    public record Job(String id,String token,String version,int attempts,long deadline,MarketSnapshot market) {}
    public record Delivery(String id,String destination,String kind,String token,String text,int attempts,long deadline) {}
    private long now(){return jdbc.queryForObject("SELECT CURRENT_TIMESTAMP FROM dual",(r,n)->r.getTimestamp(1).getTime());}
    private <T> T locked(Supplier<T> work){return tx.execute(s->{jdbc.queryForList("SELECT lock_id FROM ua_report_lock WHERE lock_id=1 FOR UPDATE");return work.get();});}
    public void register(Run run,String version,ReportProperties settings,long clockNow,long deadline){
        locked(()->{
            if(jdbc.queryForObject("SELECT COUNT(*) FROM ua_daily_report WHERE run_id=?",Integer.class,run.runId())==0)
                jdbc.update("INSERT INTO ua_daily_report(run_id,status,policy_version,policy_json,created_epoch,updated_epoch,deadline_epoch) VALUES(?,'PENDING',?,?,?,?,?)",
                        run.runId(),version,lob(json(settings)),clockNow,clockNow,deadline);
            return null;
        });
    }
    public List<String> discover(long since,long until){return jdbc.queryForList("SELECT r.run_id FROM ua_scan_run r JOIN ua_eod_run e ON e.run_id=r.run_id "
            +"WHERE r.market='ALL' AND e.eligible_epoch<=? AND e.cutoff_epoch>=? AND NOT EXISTS(SELECT 1 FROM ua_daily_report d WHERE d.run_id=r.run_id) ORDER BY r.created_epoch",String.class,until,since);}
    public Optional<Job> claim(long clockNow){return locked(()->{
        var ids=jdbc.queryForList("SELECT run_id FROM ua_daily_report WHERE status IN ('PENDING','WAITING_SCAN','RETRY_WAIT','WORKING') AND next_try_epoch<=? "
                +"AND (owner_token IS NULL OR lease_epoch<=?) ORDER BY created_epoch,run_id FETCH FIRST 1 ROWS ONLY",String.class,clockNow,now());
        if(ids.isEmpty())return Optional.empty();String id=ids.get(0),token=UUID.randomUUID().toString();
        jdbc.update("UPDATE ua_daily_report SET owner_token=?,lease_epoch=?,status='WORKING',updated_epoch=? WHERE run_id=?",token,now()+300000,clockNow,id);
        return Optional.of(jdbc.queryForObject("SELECT * FROM ua_daily_report WHERE run_id=?",(r,n)->new Job(id,token,r.getString("policy_version"),r.getInt("attempts"),r.getLong("deadline_epoch"),decode(r.getString("market_json"),MarketSnapshot.class)),id));
    });}
    private void owned(Job job,Runnable work){locked(()->{
        assertOwned(job);if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
        work.run();assertOwned(job);return null;
    });}
    private void assertOwned(Job job){if(jdbc.queryForObject("SELECT COUNT(*) FROM ua_daily_report WHERE run_id=? AND owner_token=? AND lease_epoch>? AND status='WORKING'",Integer.class,job.id(),job.token(),now())!=1)throw new LostOwnership();}
    public void attempt(Job job){owned(job,()->jdbc.update("UPDATE ua_daily_report SET attempts=attempts+1 WHERE run_id=?",job.id()));}
    public void heartbeat(Job job){owned(job,()->jdbc.update("UPDATE ua_daily_report SET lease_epoch=? WHERE run_id=?",now()+300000,job.id()));}
    public void market(Job job,MarketSnapshot market){owned(job,()->jdbc.update("UPDATE ua_daily_report SET market_json=?,attempts=0 WHERE run_id=?",lob(json(market)),job.id()));}
    public void defer(Job job,String state,String error,long next){
        locked(()->{assertOwned(job);jdbc.update("UPDATE ua_daily_report SET status=?,error_code=?,next_try_epoch=?,owner_token=NULL,lease_epoch=0 WHERE run_id=?",state,error,next,job.id());return null;});
    }
    public List<Map<String,Object>> stocks(String id){return jdbc.queryForList("SELECT stock_code,market,status FROM ua_scan_item WHERE run_id=? ORDER BY stock_code",id);}
    private record Input(String reference,String inline){}
    public JsonNode input(String id,String code){
        var saved=jdbc.queryForObject("SELECT input_json,input_gzip,input_sha256,input_utf8_bytes"+(snapshots==null?"":",input_snapshot_id")+" FROM ua_eod_item WHERE run_id=? AND stock_code=?",
            (r,n)->{String reference=snapshots==null?null:r.getString("input_snapshot_id");return new Input(reference,reference==null?com.example.stockanalysis.unified.eod.EodSnapshotCodec.read(r):null);},id,code);
        return tree(saved.reference()==null?saved.inline():snapshots.read(saved.reference()));
    }
    public void complete(Job job,DailyReport report,List<StockDecision> evidence,String error,String message,TelegramProperties telegram,long deliveryDeadline){
        locked(()->{
            assertOwned(job);if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
            int index=0;
            for(var item:evidence) {
                if(index++%25==0){assertOwned(job);jdbc.update("UPDATE ua_daily_report SET lease_epoch=? WHERE run_id=?",now()+300000,job.id());}
                jdbc.update("INSERT INTO ua_candidate_evidence(run_id,stock_code,disposition,strategy,detail_json) VALUES(?,?,?,?,?)",job.id(),item.code(),item.disposition(),item.strategy(),lob(json(item)));
            }
            jdbc.update("INSERT INTO ua_report_delivery(run_id,destination,notification_kind,status,message_text,deadline_epoch) VALUES(?,?,'DAILY_REPORT',?,?,?)",
                    job.id(),telegram.enabled()?telegram.chatId():"DISABLED",telegram.enabled()?"PENDING":"DISABLED",lob(message),deliveryDeadline);
            assertOwned(job);
            jdbc.update("UPDATE ua_daily_report SET status=?,report_json=?,error_code=?,owner_token=NULL,lease_epoch=0,updated_epoch=? WHERE run_id=?",
                    report.status(),lob(json(report)),error,report.finalizedAt().toEpochMilli(),job.id());return null;
        });
    }
    public View view(String id){
        return jdbc.query("SELECT d.status,d.error_code,d.report_json,(SELECT MAX(n.status) FROM ua_report_delivery n WHERE n.run_id=d.run_id) notification_status FROM ua_daily_report d WHERE d.run_id=?",
                (r,n)->new View(r.getString(1),r.getString(2),decode(r.getString(3),DailyReport.class),r.getString(4)),id).stream().findFirst().orElse(new View("NOT_CREATED",null,null,null));
    }
    /** Lightweight navigation, independent of the optional performance/portfolio features. */
    public List<Summary> catalog(int offset,int limit,boolean readyOnly){
        return jdbc.query("SELECT r.run_id,r.base_date,r.market,r.status,COALESCE(d.status,'NOT_CREATED') report_state "
                +"FROM ua_scan_run r JOIN ua_eod_run e ON e.run_id=r.run_id LEFT JOIN ua_daily_report d ON d.run_id=r.run_id "
                +"WHERE r.market='ALL' "+(readyOnly?"AND d.status='READY' AND d.report_json IS NOT NULL ":"")
                +"ORDER BY r.base_date DESC,r.created_epoch DESC,r.run_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                (r,n)->new Summary(r.getString(1),r.getDate(2).toLocalDate(),r.getString(3),r.getString(4),r.getString(5)),offset,limit);
    }
    public Optional<StockDecision> evidence(String id,String code){return jdbc.query("SELECT detail_json FROM ua_candidate_evidence WHERE run_id=? AND stock_code=?",(r,n)->decode(r.getString(1),StockDecision.class),id,code).stream().findFirst();}
    public Optional<Delivery> claimDelivery(long clockNow,String configuredDestination){return locked(()->{
        // A process may die after remote success. An expired SENDING claim is ambiguous, never auto-resend it.
        jdbc.update("UPDATE ua_report_delivery SET status='UNKNOWN',error_code='RESPONSE_OR_ACK_LOST',owner_token=NULL WHERE status='SENDING' AND lease_epoch<=?",now());
        jdbc.update("UPDATE ua_report_delivery SET status='EXPIRED',error_code='DELIVERY_DEADLINE' WHERE status IN ('PENDING','RETRY_WAIT') AND deadline_epoch<=?",clockNow);
        var ids=jdbc.queryForList("SELECT run_id FROM ua_report_delivery WHERE destination=? AND status IN ('PENDING','RETRY_WAIT') AND next_try_epoch<=? ORDER BY next_try_epoch,run_id FETCH FIRST 1 ROWS ONLY",String.class,configuredDestination,clockNow);
        if(ids.isEmpty())return Optional.empty();String id=ids.get(0),token=UUID.randomUUID().toString();
        jdbc.update("UPDATE ua_report_delivery SET status='SENDING',owner_token=?,lease_epoch=?,attempts=attempts+1 WHERE run_id=? AND destination=? AND notification_kind='DAILY_REPORT'",token,now()+120000,id,configuredDestination);
        return Optional.of(jdbc.queryForObject("SELECT * FROM ua_report_delivery WHERE run_id=? AND destination=? AND notification_kind='DAILY_REPORT'",
                (r,n)->new Delivery(id,configuredDestination,"DAILY_REPORT",token,r.getString("message_text"),r.getInt("attempts"),r.getLong("deadline_epoch")),id,configuredDestination));
    });}
    public void delivered(Delivery d,String state,String messageId,String error,long next){
        locked(()->{if(jdbc.update("UPDATE ua_report_delivery SET status=?,message_id=?,error_code=?,next_try_epoch=?,owner_token=NULL,lease_epoch=0 WHERE run_id=? AND destination=? AND notification_kind=? AND status='SENDING' AND owner_token=? AND lease_epoch>?",
                state,messageId,error,next,d.id(),d.destination(),d.kind(),d.token(),now())!=1)throw new LostOwnership();return null;});
    }
    public String json(Object value){try{return mapper.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException("REPORT_SERIALIZATION_FAILED");}}
    public <T>T decode(String json,Class<T> type){if(json==null)return null;try{return mapper.readValue(json,type);}catch(Exception e){throw new IllegalArgumentException("REPORT_SNAPSHOT_INVALID");}}
    private JsonNode tree(String json){try{return mapper.readTree(json);}catch(Exception e){throw new IllegalArgumentException("REPORT_INPUT_MISSING");}}
    private SqlParameterValue lob(String value){return new SqlParameterValue(java.sql.Types.CLOB,new SqlLobValue(value));}
    public static final class LostOwnership extends RuntimeException {}
}
