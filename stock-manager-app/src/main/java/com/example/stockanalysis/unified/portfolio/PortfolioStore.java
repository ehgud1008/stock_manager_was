package com.example.stockanalysis.unified.portfolio;

import static com.example.stockanalysis.unified.portfolio.PortfolioModels.*;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class PortfolioStore {
    public record Job(String id,String token,Input input,int attempts,long deadline) {}
    private final JdbcTemplate jdbc;private final ObjectMapper mapper;private final TransactionTemplate tx;
    public PortfolioStore(DataSource source,ObjectMapper mapper){jdbc=new JdbcTemplate(source);this.mapper=mapper;tx=new TransactionTemplate(new DataSourceTransactionManager(source));}
    private long dbNow(){return jdbc.queryForObject("SELECT CURRENT_TIMESTAMP FROM dual",(r,n)->r.getTimestamp(1).getTime());}
    private <T>T locked(Supplier<T> action){return tx.execute(s->{jdbc.queryForList("SELECT lock_id FROM ua_portfolio_lock WHERE lock_id=1 FOR UPDATE");return action.get();});}
    public String byKey(String key,String fingerprint){
        return jdbc.query("SELECT run_id,request_hash FROM ua_portfolio_run WHERE request_key=?",(r,n)->{
            if(!fingerprint.equals(r.getString(2)))throw new IllegalArgumentException("같은 요청 키에 다른 조건을 사용할 수 없습니다.");return r.getString(1);
        },key).stream().findFirst().orElse(null);
    }
    public String register(String key,String fingerprint,Input input,long deadline){return locked(()->{
        var existing=byKey(key,fingerprint);if(existing!=null)return existing;var id=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO ua_portfolio_run(run_id,request_key,request_hash,status,input_json,created_epoch,updated_epoch,deadline_epoch) VALUES(?,?,?,'PENDING',?,?,?,?)",
                id,key,fingerprint,lob(json(input)),input.registeredAt().toEpochMilli(),input.registeredAt().toEpochMilli(),deadline);return id;
    });}
    public List<Summary> recent(){return jdbc.query("SELECT run_id,status,created_epoch,revision,error_code FROM ua_portfolio_run ORDER BY created_epoch DESC FETCH FIRST 50 ROWS ONLY",
            (r,n)->new Summary(r.getString(1),r.getString(2),Instant.ofEpochMilli(r.getLong(3)),r.getInt(4),r.getString(5)));}
    public View view(String id){return jdbc.query("SELECT * FROM ua_portfolio_run WHERE run_id=?",(r,n)->new View(id,r.getString("status"),r.getString("error_code"),r.getInt("revision"),
            decode(r.getString("input_json"),Input.class),decode(r.getString("result_json"),Result.class)),id).stream().findFirst().orElse(null);}
    public Result revision(String id,int revision){return jdbc.query("SELECT result_json FROM ua_portfolio_revision WHERE run_id=? AND revision=?",(r,n)->decode(r.getString(1),Result.class),id,revision).stream().findFirst().orElse(null);}
    public Optional<Job> claim(long now){return locked(()->{
        var ids=jdbc.queryForList("SELECT run_id FROM ua_portfolio_run WHERE status IN ('PENDING','WORKING','WAITING_DATA','RETRY_WAIT') AND next_try_epoch<=? AND (owner_token IS NULL OR lease_epoch<=?) ORDER BY next_try_epoch,created_epoch FETCH FIRST 1 ROWS ONLY",String.class,now,dbNow());
        if(ids.isEmpty())return Optional.empty();var id=ids.get(0);var token=UUID.randomUUID().toString();
        jdbc.update("UPDATE ua_portfolio_run SET status='WORKING',owner_token=?,lease_epoch=?,attempts=attempts+1,updated_epoch=? WHERE run_id=?",token,dbNow()+300000,now,id);
        return Optional.of(jdbc.queryForObject("SELECT input_json,attempts,deadline_epoch FROM ua_portfolio_run WHERE run_id=?",(r,n)->new Job(id,token,decode(r.getString(1),Input.class),r.getInt(2),r.getLong(3)),id));
    });}
    private void owned(Job job){if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
        if(jdbc.queryForObject("SELECT COUNT(*) FROM ua_portfolio_run WHERE run_id=? AND status='WORKING' AND owner_token=? AND lease_epoch>?",Integer.class,job.id(),job.token(),dbNow())!=1)throw new LostOwnership();}
    public void heartbeat(Job job){locked(()->{owned(job);jdbc.update("UPDATE ua_portfolio_run SET lease_epoch=? WHERE run_id=?",dbNow()+300000,job.id());return null;});}
    public List<Observed> observations(String id){return jdbc.query("SELECT detail_json FROM ua_portfolio_observation WHERE run_id=? ORDER BY observation_key",(r,n)->decode(r.getString(1),Observed.class),id);}
    public void observe(Job job,Observed value){locked(()->{owned(job);
        if(jdbc.queryForObject("SELECT COUNT(*) FROM ua_portfolio_observation WHERE run_id=? AND observation_key=?",Integer.class,job.id(),value.key())==0)
            jdbc.update("INSERT INTO ua_portfolio_observation(run_id,observation_key,detail_json) VALUES(?,?,?)",job.id(),value.key(),lob(json(value)));
        return null;});}
    public void complete(Job job,Result result,long next){locked(()->{
        owned(job);var old=view(job.id());boolean changed=old.result()==null || !old.result().state().equals(result.state()) || !Objects.equals(old.result().throughDate(),result.throughDate())
                || !old.result().variants().equals(result.variants()) || !old.result().forecasts().equals(result.forecasts());
        int revision=old.revision()+(changed?1:0);var output=json(changed?result:old.result());
        if(changed)jdbc.update("INSERT INTO ua_portfolio_revision(run_id,revision,result_json,created_epoch) VALUES(?,?,?,?)",job.id(),revision,lob(output),result.evaluatedAt().toEpochMilli());
        owned(job);jdbc.update("UPDATE ua_portfolio_run SET status=?,result_json=?,revision=?,updated_epoch=?,next_try_epoch=?,attempts=0,owner_token=NULL,lease_epoch=0,error_code=NULL WHERE run_id=?",
                result.state(),lob(output),revision,result.evaluatedAt().toEpochMilli(),next,job.id());return null;
    });}
    public void failure(Job job,String error,long now){locked(()->{owned(job);boolean terminal=job.attempts()>=3||now>=job.deadline()||error.equals("ENGINE_VERSION_CHANGED");
        jdbc.update("UPDATE ua_portfolio_run SET status=?,error_code=?,updated_epoch=?,next_try_epoch=?,owner_token=NULL,lease_epoch=0 WHERE run_id=?",terminal?"FAILED":"RETRY_WAIT",error,now,now+60000L*job.attempts(),job.id());return null;});}
    public String json(Object value){try{return mapper.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException("PORTFOLIO_SERIALIZATION");}}
    private <T>T decode(String value,Class<T> type){if(value==null)return null;try{return mapper.readValue(value,type);}catch(Exception e){throw new IllegalArgumentException("PORTFOLIO_SNAPSHOT_INVALID");}}
    private SqlParameterValue lob(String text){return new SqlParameterValue(java.sql.Types.CLOB,new SqlLobValue(text));}
    public static final class LostOwnership extends RuntimeException {}
}
