package com.example.stockanalysis.unified.performance;

import static com.example.stockanalysis.unified.performance.PerformanceModels.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Types;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class PerformanceStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final TransactionTemplate tx;
    public record Job(String id,String token,String version,Input input,int attempts,long deadline) {}
    public PerformanceStore(DataSource source,ObjectMapper mapper) {
        this.jdbc=new JdbcTemplate(source);this.mapper=mapper;this.tx=new TransactionTemplate(new DataSourceTransactionManager(source));
    }
    private long dbNow(){return jdbc.queryForObject("SELECT CURRENT_TIMESTAMP FROM dual",(rs,n)->rs.getTimestamp(1).getTime());}
    private <T>T locked(Supplier<T> work){return tx.execute(status->{jdbc.queryForList("SELECT lock_id FROM ua_eval_lock WHERE lock_id=1 FOR UPDATE");return work.get();});}
    public List<String> discover(long now){return jdbc.queryForList("SELECT d.run_id FROM ua_daily_report d LEFT JOIN ua_eval_run e ON e.run_id=d.run_id WHERE d.status='READY' AND (e.run_id IS NULL OR (e.status='FAILED' AND e.error_code='REGISTRATION_DATA_OR_CALENDAR_INVALID' AND e.next_try_epoch<=?)) ORDER BY COALESCE(e.next_try_epoch,0),d.created_epoch FETCH FIRST 10 ROWS ONLY",String.class,now);}
    public void reject(String id,String error,long now){locked(()->{
        if(jdbc.queryForObject("SELECT COUNT(*) FROM ua_eval_run WHERE run_id=?",Integer.class,id)==0)
            jdbc.update("INSERT INTO ua_eval_run(run_id,status,engine_version,input_json,created_epoch,updated_epoch,deadline_epoch,error_code,next_try_epoch) VALUES(?,'FAILED',?,'{}',?,?,?,?,?)",id,PerformanceEngine.VERSION,now,now,now,error,now+900000);
        else jdbc.update("UPDATE ua_eval_run SET next_try_epoch=?,updated_epoch=? WHERE run_id=? AND error_code='REGISTRATION_DATA_OR_CALENDAR_INVALID' AND revision=0",now+900000,now,id);
        return null;
    });}
    public void register(Input input,long deadline){locked(()->{
        if(jdbc.queryForObject("SELECT COUNT(*) FROM ua_eval_run WHERE run_id=?",Integer.class,input.report().runId())==0)
            jdbc.update("INSERT INTO ua_eval_run(run_id,status,engine_version,input_json,created_epoch,updated_epoch,deadline_epoch) VALUES(?,'PENDING',?,?,?,?,?)",
                    input.report().runId(),input.policy().version(),lob(json(input)),input.registeredAt().toEpochMilli(),input.registeredAt().toEpochMilli(),deadline);
        else jdbc.update("UPDATE ua_eval_run SET status='PENDING',engine_version=?,input_json=?,deadline_epoch=?,next_try_epoch=0,error_code=NULL,created_epoch=?,updated_epoch=? WHERE run_id=? AND status='FAILED' AND error_code='REGISTRATION_DATA_OR_CALENDAR_INVALID' AND revision=0",
                input.policy().version(),lob(json(input)),deadline,input.registeredAt().toEpochMilli(),input.registeredAt().toEpochMilli(),input.report().runId());
        return null;
    });}
    public View view(String id){return jdbc.query("SELECT status,error_code,revision,result_json FROM ua_eval_run WHERE run_id=?",
            (r,n)->new View(r.getString(1),r.getString(2),r.getInt(3),decode(r.getString(4),Result.class)),id).stream().findFirst().orElse(new View("NOT_CREATED",null,0,null));}
    public Optional<Job> claim(long now){return locked(()->{
        var ids=jdbc.queryForList("SELECT run_id FROM ua_eval_run WHERE status IN ('PENDING','WAITING_DATA','RETRY_WAIT','WORKING') AND next_try_epoch<=? AND (owner_token IS NULL OR lease_epoch<=?) ORDER BY next_try_epoch,created_epoch FETCH FIRST 1 ROWS ONLY",String.class,now,dbNow());
        if(ids.isEmpty())return Optional.empty();var id=ids.get(0);var token=UUID.randomUUID().toString();
        jdbc.update("UPDATE ua_eval_run SET status='WORKING',owner_token=?,lease_epoch=?,attempts=attempts+1 WHERE run_id=?",token,dbNow()+300000,id);
        return Optional.of(jdbc.queryForObject("SELECT * FROM ua_eval_run WHERE run_id=?",(r,n)->new Job(id,token,r.getString("engine_version"),decode(r.getString("input_json"),Input.class),r.getInt("attempts"),r.getLong("deadline_epoch")),id));
    });}
    private void owned(Job job){
        if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
        if(jdbc.queryForObject("SELECT COUNT(*) FROM ua_eval_run WHERE run_id=? AND owner_token=? AND status='WORKING' AND lease_epoch>?",Integer.class,job.id(),job.token(),dbNow())!=1)throw new LostOwnership();
    }
    public void heartbeat(Job job){locked(()->{owned(job);jdbc.update("UPDATE ua_eval_run SET lease_epoch=? WHERE run_id=?",dbNow()+300000,job.id());return null;});}
    public List<Observation> observations(String id){return jdbc.query("SELECT detail_json FROM ua_eval_observation WHERE run_id=? ORDER BY trade_date,observation_kind,asset_code",(r,n)->decode(r.getString(1),Observation.class),id);}
    public void observe(Job job,Observation observation){locked(()->{
        owned(job);
        if(jdbc.queryForObject("SELECT COUNT(*) FROM ua_eval_observation WHERE run_id=? AND observation_kind=? AND asset_code=? AND trade_date=?",Integer.class,job.id(),observation.kind(),observation.code(),java.sql.Date.valueOf(observation.date()))==0)
            jdbc.update("INSERT INTO ua_eval_observation(run_id,observation_kind,asset_code,trade_date,detail_json) VALUES(?,?,?,?,?)",job.id(),observation.kind(),observation.code(),java.sql.Date.valueOf(observation.date()),lob(json(observation)));
        owned(job);return null;
    });}
    public void complete(Job job,Result result,long next){locked(()->{
        owned(job);var old=view(job.id());
        // Polls with no new evidence do not create misleading new evaluation versions.
        boolean changed=old.result()==null || !Objects.equals(old.result().stocks(),result.stocks())
                || !Objects.equals(old.result().forecasts(),result.forecasts()) || !Objects.equals(old.result().throughDate(),result.throughDate())
                || !Objects.equals(old.result().state(),result.state());
        int revision=old.revision()+(changed?1:0);String output=json(changed?result:old.result());
        if(changed)jdbc.update("INSERT INTO ua_eval_revision(run_id,revision,result_json,created_epoch) VALUES(?,?,?,?)",job.id(),revision,lob(output),result.evaluatedAt().toEpochMilli());
        owned(job);
        jdbc.update("UPDATE ua_eval_run SET status=?,result_json=?,revision=?,updated_epoch=?,next_try_epoch=?,attempts=0,owner_token=NULL,lease_epoch=0,error_code=NULL WHERE run_id=?",
                result.state(),lob(output),revision,result.evaluatedAt().toEpochMilli(),next,job.id());return null;
    });}
    public void failure(Job job,String error,long now){locked(()->{
        owned(job);boolean terminal=job.attempts()>=3 || now>=job.deadline() || error.equals("ENGINE_VERSION_CHANGED");
        jdbc.update("UPDATE ua_eval_run SET status=?,error_code=?,next_try_epoch=?,updated_epoch=?,owner_token=NULL,lease_epoch=0 WHERE run_id=?",
                terminal?"FAILED":"RETRY_WAIT",error,now+60000L*job.attempts(),now,job.id());return null;
    });}
    public String json(Object value){try{return mapper.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException("EVALUATION_SERIALIZATION");}}
    private <T>T decode(String text,Class<T> type){if(text==null)return null;try{return mapper.readValue(text,type);}catch(Exception e){throw new IllegalArgumentException("EVALUATION_SNAPSHOT_INVALID");}}
    private SqlParameterValue lob(String value){return new SqlParameterValue(Types.CLOB,new SqlLobValue(value));}
    public static final class LostOwnership extends RuntimeException {}
}
