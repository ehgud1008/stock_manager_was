package com.example.stockanalysis.storage;

import com.example.stockanalysis.unified.eod.TradingCalendar;
import com.example.stockanalysis.unified.report.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class StorageService {
    private static final ZoneId SEOUL=ZoneId.of("Asia/Seoul");
    private final JdbcTemplate jdbc;private final TransactionTemplate tx;private final ObjectMapper mapper;
    private final SnapshotRepository snapshots;private final StorageMigration migration;private final StorageUsageProbe probe;
    private final StorageProperties settings;private final TradingCalendar calendar;private final Clock clock;
    private final TelegramProperties telegram;private final TelegramGateway gateway;private final String owner=UUID.randomUUID().toString();
    public StorageService(DataSource source,ObjectMapper mapper,SnapshotRepository snapshots,StorageMigration migration,StorageUsageProbe probe,
            StorageProperties settings,TradingCalendar calendar,Clock clock,TelegramProperties telegram,TelegramGateway gateway){
        jdbc=new JdbcTemplate(source);tx=new TransactionTemplate(new DataSourceTransactionManager(source));this.mapper=mapper;this.snapshots=snapshots;this.migration=migration;this.probe=probe;
        this.settings=settings;this.calendar=calendar;this.clock=clock;this.telegram=telegram;this.gateway=gateway;
    }
    public record Sample(LocalDate date,Instant measuredAt,StorageUsageProbe.Usage usage,StorageHealth.Assessment assessment){}
    public record Plan(boolean enabled,boolean configured,int hotTradingDays,LocalDate keepFrom,Long eligibleObjects,String state){}
    public record Notice(String id,String level,String status,String errorCode,Instant requestedAt){}
    public record Status(boolean deduplicationEnabled,boolean legacyMigrationEnabled,boolean monitoringEnabled,boolean alertsEnabled,Sample latest,
                         Map<String,Object> deduplication,Plan archive,List<Notice> notices,String lastError){}
    public Status status(){
        var latest=jdbc.query("SELECT details_json FROM ua_storage_sample ORDER BY sample_date DESC FETCH FIRST 1 ROWS ONLY",(r,n)->read(r.getString(1),Sample.class)).stream().findFirst().orElse(null);
        var notices=jdbc.query("SELECT notice_id,severity,status,error_code,created_epoch FROM ua_storage_notice ORDER BY created_epoch DESC FETCH FIRST 10 ROWS ONLY",
            (r,n)->new Notice(r.getString(1),r.getString(2),r.getString(3),r.getString(4),Instant.ofEpochMilli(r.getLong(5))));
        String error=jdbc.queryForObject("SELECT last_error FROM ua_storage_state WHERE state_id=1",String.class);
        return new Status(settings.deduplicationEnabled(),settings.legacyMigrationEnabled(),settings.monitoringEnabled(),settings.alertsEnabled(),latest,snapshots.counts(),plan(),notices,error);
    }
    public Plan plan(){
        boolean configured=!settings.archiveDirectory().isBlank();
        if(!settings.archiveEnabled())return new Plan(false,configured,settings.hotTradingDays(),null,null,"DISABLED");
        final String identity;
        try{identity=snapshots.archiveIdentity();snapshots.validateArchiveWriter();}catch(RuntimeException e){return new Plan(true,configured,settings.hotTradingDays(),null,null,"ARCHIVE_UNAVAILABLE");}
        long unready=jdbc.queryForObject("SELECT COUNT(*) FROM ua_storage_instance WHERE last_seen_epoch>? AND (archive_store_id IS NULL OR archive_store_id<>?)",Long.class,clock.millis()-300000,identity);
        if(unready>0)return new Plan(true,configured,settings.hotTradingDays(),null,null,"READERS_NOT_READY");
        LocalDate cutoff=retentionDate();
        if(cutoff==null)return new Plan(true,configured,settings.hotTradingDays(),null,null,"CALENDAR_UNAVAILABLE");
        long count=jdbc.queryForObject("SELECT COUNT(*) FROM ua_snapshot_object WHERE payload_gzip IS NOT NULL AND last_reference_epoch<?",Long.class,cutoff.atStartOfDay(SEOUL).toInstant().toEpochMilli());
        return new Plan(true,configured,settings.hotTradingDays(),cutoff,count,"READY");
    }
    public void heartbeat(){
        String identity;try{identity=snapshots.archiveIdentity();if(settings.archiveEnabled())snapshots.validateArchiveWriter();}catch(RuntimeException e){identity=null;}
        int updated=jdbc.update("UPDATE ua_storage_instance SET archive_store_id=?,last_seen_epoch=?,archive_enabled=? WHERE instance_id=?",identity,clock.millis(),settings.archiveEnabled()?1:0,owner);
        if(updated==0)jdbc.update("INSERT INTO ua_storage_instance(instance_id,archive_store_id,last_seen_epoch,archive_enabled) VALUES(?,?,?,?)",owner,identity,clock.millis(),settings.archiveEnabled()?1:0);
        tx.executeWithoutResult(status->{
            var state=jdbc.queryForMap("SELECT last_archive_health,archived_objects,archive_store_id FROM ua_storage_state WHERE state_id=1 FOR UPDATE");
            boolean required=((Number)state.get("ARCHIVED_OBJECTS")).longValue()>0||jdbc.queryForObject("SELECT COUNT(*) FROM ua_storage_instance WHERE last_seen_epoch>? AND archive_enabled=1",Long.class,clock.millis()-300000)>0;
            long missing=jdbc.queryForObject("SELECT COUNT(*) FROM ua_storage_instance WHERE last_seen_epoch>? AND (archive_store_id IS NULL OR archive_store_id<>?)",Long.class,clock.millis()-300000,state.get("ARCHIVE_STORE_ID"));
            String health=!required?"DISABLED":missing>0?"ERROR":"OK";String previous=(String)state.get("LAST_ARCHIVE_HEALTH");
            if(!health.equals(previous)&&settings.alertsEnabled()&&telegram.enabled()&&(health.equals("ERROR")||previous.equals("ERROR"))){
                jdbc.update("INSERT INTO ua_storage_notice(notice_id,created_epoch,severity,destination,message_text,status,deadline_epoch) VALUES(?,?,?,?,?,'PENDING',?)",UUID.randomUUID().toString(),clock.millis(),health.equals("ERROR")?"ARCHIVE_ERROR":"ARCHIVE_RECOVERED",telegram.chatId(),health.equals("ERROR")?"원문 보관 파일에 접근할 준비가 되지 않은 서버가 있습니다. 저장소 연결을 확인하세요. DB 원문 이동은 보류합니다.":"원문 보관 저장소 연결이 정상 상태로 돌아왔습니다.",clock.millis()+86400000);
            }
            jdbc.update("UPDATE ua_storage_state SET last_archive_health=?,last_error=CASE WHEN ?='ERROR' THEN 'ARCHIVE_READERS_UNAVAILABLE' WHEN last_error='ARCHIVE_READERS_UNAVAILABLE' THEN NULL ELSE last_error END WHERE state_id=1",health,health);
            jdbc.update("DELETE FROM ua_storage_instance WHERE last_seen_epoch<?",clock.millis()-7*86400000L);
        });
    }
    private LocalDate retentionDate(){
        int open=0;var due=calendar.latestDue(clock.instant().atZone(SEOUL).toLocalDateTime());
        if(due==null||due.status()!=TradingCalendar.Status.OPEN)return null;var today=due.date();
        for(int offset=0;offset<settings.hotTradingDays()*3+30;offset++){
            var date=today.minusDays(offset);var day=calendar.day(date);if(day.status()==TradingCalendar.Status.UNKNOWN)return null;
            if(day.status()==TradingCalendar.Status.OPEN&&++open==settings.hotTradingDays())return date;
        }return null;
    }
    public void monitor(){
        if(!settings.monitoringEnabled())return;fenced(()->{
            var usage=probe.measure();
            // A changed measurement scope must replace an invalid recent sample immediately.
            var recent=jdbc.queryForObject("SELECT MAX(measured_epoch) FROM ua_storage_sample WHERE measurement_scope=?",Long.class,usage.scope());
            if(recent!=null&&clock.millis()-recent<3600000)return;
            var today=clock.instant().atZone(SEOUL).toLocalDate();
            var past=jdbc.query("SELECT allocated_bytes,measured_epoch FROM ua_storage_sample WHERE sample_date<? AND sample_date>=? AND measurement_scope=? ORDER BY sample_date FETCH FIRST 1 ROWS ONLY",
                (r,n)->new long[]{r.getLong(1),r.getLong(2)},java.sql.Date.valueOf(today),java.sql.Date.valueOf(today.minusDays(7)),usage.scope()).stream().findFirst().orElse(null);
            var assessment=StorageHealth.assess(usage,past==null?null:past[0],past==null?0:clock.millis()-past[1],settings);
            var sample=new Sample(today,clock.instant(),usage,assessment);
            tx.executeWithoutResult(status->{
                String previous=jdbc.queryForObject("SELECT last_level FROM ua_storage_state WHERE state_id=1 FOR UPDATE",String.class);
                int updated=jdbc.update("UPDATE ua_storage_sample SET measured_epoch=?,allocated_bytes=?,capacity_bytes=?,measurement_scope=?,severity=?,details_json=? WHERE sample_date=?",
                    clock.millis(),usage.allocatedBytes(),usage.capacityBytes(),usage.scope(),assessment.level(),json(sample),java.sql.Date.valueOf(today));
                if(updated==0)jdbc.update("INSERT INTO ua_storage_sample(sample_date,measured_epoch,allocated_bytes,capacity_bytes,measurement_scope,severity,details_json) VALUES(?,?,?,?,?,?,?)",
                    java.sql.Date.valueOf(today),clock.millis(),usage.allocatedBytes(),usage.capacityBytes(),usage.scope(),assessment.level(),json(sample));
                if(!assessment.level().equals("UNKNOWN")){
                    if(!assessment.level().equals(previous)&&settings.alertsEnabled()&&telegram.enabled()){
                        jdbc.update("INSERT INTO ua_storage_notice(notice_id,created_epoch,severity,destination,message_text,status,deadline_epoch) VALUES(?,?,?,?,?,'PENDING',?)",
                            UUID.randomUUID().toString(),clock.millis(),assessment.level(),telegram.chatId(),message(sample),clock.millis()+86400000);
                    }
                    jdbc.update("UPDATE ua_storage_state SET last_level=?,last_error=CASE WHEN last_archive_health='ERROR' THEN last_error ELSE NULL END WHERE state_id=1",assessment.level());
                }
            });
        });
    }
    public void maintain(){
        if(!settings.archiveEnabled()&&!settings.legacyMigrationEnabled())return;
        fenced(()->{
            // Collection has priority; archive only completed immutable inputs between collection runs.
            long active=jdbc.queryForObject("SELECT COUNT(*) FROM ua_eod_lease WHERE owner_token IS NOT NULL AND expires_epoch>?",Long.class,clock.millis());if(active>0)return;
            if(settings.legacyMigrationEnabled()&&snapshots.enabled())migration.batch(settings.legacyBatchSize());
            if(settings.archiveEnabled()){
                var plan=plan();if(!plan.state().equals("READY"))throw new IllegalStateException("ARCHIVE_CALENDAR_UNAVAILABLE");
                snapshots.archiveBefore(plan.keepFrom().atStartOfDay(SEOUL).toInstant().toEpochMilli(),settings.archiveBatchSize(),clock.millis());
                jdbc.update("UPDATE ua_storage_state SET last_archive_epoch=?,last_error=NULL WHERE state_id=1",clock.millis());
            }
        });
    }
    private record Delivery(String id,String token,String destination,String text,int attempts,long deadline){}
    public void deliver(){
        if(!settings.alertsEnabled()||!telegram.enabled())return;
        fenced(()->{
            Delivery delivery=tx.execute(status->{
                jdbc.queryForList("SELECT lock_id FROM ua_storage_lock WHERE lock_id=2 FOR UPDATE");
                jdbc.update("UPDATE ua_storage_notice SET status='UNKNOWN',error_code='DELIVERY_UNCONFIRMED',owner_token=NULL WHERE status='SENDING' AND lease_epoch<=?",clock.millis());
                jdbc.update("UPDATE ua_storage_notice SET status='FAILED',error_code='DELIVERY_EXPIRED' WHERE status IN ('PENDING','RETRY_WAIT') AND deadline_epoch<=?",clock.millis());
                var ids=jdbc.queryForList("SELECT notice_id FROM ua_storage_notice WHERE status IN ('PENDING','RETRY_WAIT') AND next_try_epoch<=? AND deadline_epoch>? ORDER BY created_epoch FETCH FIRST 1 ROWS ONLY",String.class,clock.millis(),clock.millis());
                if(ids.isEmpty())return null;String id=ids.get(0),token=UUID.randomUUID().toString();
                jdbc.update("UPDATE ua_storage_notice SET status='SENDING',owner_token=?,lease_epoch=?,attempts=attempts+1 WHERE notice_id=?",token,clock.millis()+300000,id);
                return jdbc.queryForObject("SELECT destination,message_text,attempts,deadline_epoch FROM ua_storage_notice WHERE notice_id=?",(r,n)->new Delivery(id,token,r.getString(1),r.getString(2),r.getInt(3),r.getLong(4)),id);
            });
            if(delivery==null)return;
            TelegramGateway.Result response;
            try{response=gateway.send(delivery.destination(),delivery.text());}catch(RuntimeException e){response=new TelegramGateway.Result("UNKNOWN",null,"DELIVERY_UNCONFIRMED",0);}
            String state=Set.of("SENT","RETRY_WAIT","FAILED","UNKNOWN").contains(response.state())?response.state():"UNKNOWN";
            long next=clock.millis()+Math.max(1,Math.min(response.retryAfterSeconds(),86400))*1000;
            if(state.equals("RETRY_WAIT")&&(delivery.attempts()>=3||next>=delivery.deadline()))state="FAILED";
            jdbc.update("UPDATE ua_storage_notice SET status=?,message_id=?,error_code=?,next_try_epoch=?,owner_token=NULL,lease_epoch=0 WHERE notice_id=? AND owner_token=? AND status='SENDING'",
                state,response.messageId(),response.errorCode(),next,delivery.id(),delivery.token());
        });
    }
    private String message(Sample sample){
        var a=sample.assessment();var u=sample.usage();
        String title=a.level().equals("NORMAL")?"DB 용량 상태가 정상 범위로 돌아왔습니다.":a.level().equals("CRITICAL")?"DB 저장공간 긴급 확인이 필요합니다.":"DB 저장공간 확인이 필요합니다.";
        String scope=switch(u.scope()){
            case "USER_DATA_SEGMENTS"->"사용자 데이터 세그먼트 할당량(Oracle 관리 계정 제외)";
            case "PDB_SEGMENTS"->"DB 전체 세그먼트 할당량(기존 측정 기준)";
            default->"현재 계정의 세그먼트 할당량(다른 계정 사용량 미확인)";
        };
        return title+"\n"+scope+": "+String.format(Locale.ROOT,"%.2f GiB",u.allocatedBytes()/1073741824.0)
            +(a.allocatedPercent()==null?"":" · 한도 대비 "+String.format(Locale.ROOT,"%.1f%%",a.allocatedPercent()))
            +(a.estimatedDays()==null?"":"\n최근 관측 증가 속도로 약 "+a.estimatedDays()+"일 후 한도 도달 예상")
            +"\n할당량 기준 참고치이며 실제 여유 공간과 다를 수 있습니다. 저장소 상태를 확인하세요.";
    }
    private void fenced(Runnable action){
        long now=clock.millis();
        if(jdbc.update("UPDATE ua_storage_lock SET owner_token=?,lease_epoch=? WHERE lock_id=2 AND (owner_token IS NULL OR lease_epoch<=?)",owner,now+300000,now)!=1)return;
        try{action.run();}
        catch(RuntimeException e){jdbc.update("UPDATE ua_storage_state SET last_error=? WHERE state_id=1","STORAGE_"+e.getClass().getSimpleName().toUpperCase(Locale.ROOT));throw e;}
        finally{jdbc.update("UPDATE ua_storage_lock SET owner_token=NULL,lease_epoch=0 WHERE lock_id=2 AND owner_token=?",owner);}
    }
    private String json(Object value){try{return mapper.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException("STORAGE_SERIALIZATION_FAILED",e);}}
    private <T>T read(String value,Class<T> type){try{return mapper.readValue(value,type);}catch(Exception e){throw new IllegalArgumentException("STORAGE_HISTORY_INVALID",e);}}
}
