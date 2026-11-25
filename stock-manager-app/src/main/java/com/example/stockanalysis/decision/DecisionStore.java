package com.example.stockanalysis.decision;

import static com.example.stockanalysis.decision.DecisionModels.*;
import com.example.stockanalysis.shared.exception.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.sql.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class DecisionStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final DecisionJson json;
    public DecisionStore(DataSource ds, DecisionJson json) {
        this.jdbc=new JdbcTemplate(ds); this.tx=new TransactionTemplate(new DataSourceTransactionManager(ds)); this.json=json;
    }
    private Object lob(Object value) { return new SqlParameterValue(Types.CLOB, new SqlLobValue(json.write(value))); }
    public Optional<Job> find(String owner, String id) {
        return jdbc.query("SELECT * FROM td_job WHERE owner_scope=? AND job_id=?", this::row, owner,id).stream().findFirst();
    }
    public List<DecisionBasketService.Candidate> basketCandidates(String owner, int page, int size) {
        return jdbc.query("""
                SELECT j.job_id,j.stock_code,j.snapshot_id,j.completed_epoch
                  FROM td_job j
                 WHERE j.owner_scope=? AND j.kind='DECISION' AND j.status='SUCCEEDED' AND j.final_action='BUY'
                   AND EXISTS (SELECT 1 FROM td_decision_plan p
                                WHERE p.owner_scope=j.owner_scope AND p.job_id=j.job_id AND p.plan_role='AI_FINAL')
                 ORDER BY j.completed_epoch DESC,j.job_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """, (r,n)->new DecisionBasketService.Candidate(r.getString(1),r.getString(2),r.getString(3),
                r.getLong(4)), owner, (long)page*size, size+1);
    }
    public Optional<Job> importedSnapshot(String owner,String code,String runId) {
        // Read only the small input document while locating legacy imports; result CLOBs are loaded once.
        // Existing schemas have no source-run column. Page rather than materializing every snapshot.
        for(int page=0;;page++) {
            var inputs=jdbc.query("SELECT s.job_id,s.input_json,(SELECT MAX(d.created_epoch) FROM td_job d WHERE d.owner_scope=s.owner_scope AND d.kind='DECISION' AND d.snapshot_id=s.job_id) last_decision FROM td_job s WHERE s.owner_scope=? AND s.kind='SNAPSHOT' AND s.stock_code=? ORDER BY last_decision DESC NULLS LAST,s.created_epoch DESC,s.job_id DESC OFFSET ? ROWS FETCH NEXT 50 ROWS ONLY",
                    (r,n)->Map.entry(r.getString(1),json.read(r.getString(2))),owner,code,(long)page*50);
            for(var entry:inputs) if("UNIFIED_RUN_ITEM".equals(entry.getValue().path("source").asText())
                    && runId.equals(entry.getValue().path("runId").asText())) return find(owner,entry.getKey());
            if(inputs.size()<50) return Optional.empty();
        }
    }
    public String contentKey(Job job) {
        return jdbc.queryForObject("SELECT content_key FROM td_job WHERE job_id=? AND owner_scope=?",String.class,job.id(),job.owner());
    }
    public Optional<Job> prior(String owner,String operation,String key,String bodyHash) {
        var rows=jdbc.queryForList("SELECT job_id,request_hash FROM td_request WHERE owner_scope=? AND operation=? AND request_key=?",owner,operation,key);
        if(rows.isEmpty()) return Optional.empty();
        if(!bodyHash.equals(rows.get(0).get("REQUEST_HASH"))) throw conflict();
        return find(owner,(String)rows.get(0).get("JOB_ID"));
    }
    public Job submit(String owner,String operation,String key,String bodyHash,String contentKey,String kind,String code,
                      String snapshotId,String parentId,JsonNode input,long now,String day,int dailyLimit) {
        ensureBudget(owner,day);
        try {
            return tx.execute(s -> {
                lockBudget(owner,day);
                var prior=prior(owner,operation,key,bodyHash); if(prior.isPresent()) return prior.get();
                var matching=jdbc.query("SELECT * FROM td_job WHERE owner_scope=? AND content_key=?",this::row,owner,contentKey);
                String id;
                if(!matching.isEmpty()) id=matching.get(0).id();
                else {
                    long queued=jdbc.queryForObject("SELECT COUNT(*) FROM td_job WHERE owner_scope=? AND status IN ('PENDING','RUNNING','RETRYABLE_FAILED')",Long.class,owner);
                    if(queued>=50 || jdbc.update("UPDATE td_budget SET request_count=request_count+1 WHERE owner_scope=? AND budget_day=? AND request_count<?",owner,day,dailyLimit)!=1)
                        throw new BusinessException(ErrorCode.DECISION_LIMIT);
                    id=UUID.randomUUID().toString();
                    jdbc.update("INSERT INTO td_job(job_id,owner_scope,kind,stock_code,content_key,snapshot_id,parent_id,status,input_json,created_epoch,next_epoch) VALUES(?,?,?,?,?,?,?,'PENDING',?,?,?)",
                            id,owner,kind,code,contentKey,snapshotId,parentId,lob(input),now,now);
                }
                jdbc.update("INSERT INTO td_request(owner_scope,operation,request_key,request_hash,job_id) VALUES(?,?,?,?,?)",owner,operation,key,bodyHash,id);
                return find(owner,id).orElseThrow();
            });
        } catch(DuplicateKeyException race) {
            // A transaction spanning a day boundary may use a different budget lock.
            var winner=prior(owner,operation,key,bodyHash);
            if(winner.isPresent()) return winner.get();
            throw new BusinessException(ErrorCode.DECISION_CONFLICT,"같은 입력이 등록 중입니다. 같은 요청 키로 다시 시도하세요.");
        }
    }
    public Optional<Job> claim(String owner,String worker,long now,long leaseMillis) {
        return tx.execute(s -> {
            jdbc.update("UPDATE td_job SET status='FAILED',error_code='ATTEMPTS_EXHAUSTED',retryable=0,completed_epoch=?,lease_owner=NULL,lease_until=NULL WHERE owner_scope=? AND status='RUNNING' AND lease_until<? AND attempt_token>=3",now,owner,now);
            var candidates=jdbc.queryForList("SELECT job_id FROM td_job WHERE owner_scope=? AND attempt_token<3 AND ((status IN ('PENDING','RETRYABLE_FAILED') AND next_epoch<=?) OR (status='RUNNING' AND lease_until<?)) ORDER BY created_epoch,job_id FETCH FIRST 10 ROWS ONLY",String.class,owner,now,now);
            for(String id:candidates) {
                int n=jdbc.update("UPDATE td_job SET status='RUNNING',attempt_token=attempt_token+1,lease_owner=?,lease_until=?,retryable=0,error_code=NULL WHERE job_id=? AND owner_scope=? AND attempt_token<3 AND ((status IN ('PENDING','RETRYABLE_FAILED') AND next_epoch<=?) OR (status='RUNNING' AND lease_until<?))",worker,now+leaseMillis,id,owner,now,now);
                if(n==1) {
                    Job job=find(owner,id).orElseThrow();
                    jdbc.update("INSERT INTO td_attempt(job_id,attempt_token,started_epoch) VALUES(?,?,?)",id,job.attempt(),now);
                    return Optional.of(job);
                }
            }
            return Optional.empty();
        });
    }
    public boolean heartbeat(Job job,long now,long leaseMillis) {
        return jdbc.update("UPDATE td_job SET lease_until=? WHERE job_id=? AND owner_scope=? AND status='RUNNING' AND attempt_token=? AND lease_owner=? AND lease_until>?",
                now+leaseMillis,job.id(),job.owner(),job.attempt(),job.leaseOwner(),now)==1;
    }
    public boolean complete(Job job,JsonNode result,long now) {
        return Boolean.TRUE.equals(tx.execute(s -> {
            int n=jdbc.update("UPDATE td_job SET status='SUCCEEDED',result_json=?,result_hash=?,final_action=?,completed_epoch=?,lease_owner=NULL,lease_until=NULL,error_code=NULL,retryable=0 WHERE job_id=? AND owner_scope=? AND status='RUNNING' AND attempt_token=? AND lease_owner=? AND lease_until>?",
                    lob(result),json.hash(result),result.path("finalAction").asText(null),now,job.id(),job.owner(),job.attempt(),job.leaseOwner(),now);
            if(n==1) {
                jdbc.update("UPDATE td_attempt SET completed_epoch=? WHERE job_id=? AND attempt_token=?",now,job.id(),job.attempt());
                if("research-v2".equals(result.path("evidence").path("researchVersion").asText())) archiveResearch(job,result,now);
                if("INTEGRATED_DECISION".equals(result.path("generationMode").asText()))
                    DecisionOutcomeStore.freeze(jdbc,json,job,result,now);
            }
            return n==1;
        }));
    }
    public void recordResponse(Job job,AiResponse response) {
        jdbc.update("UPDATE td_attempt SET provider_id=?,response_json=?,input_tokens=?,output_tokens=? WHERE job_id=? AND attempt_token=?",
                response.providerId(),lob(response.raw()),response.inputTokens(),response.outputTokens(),job.id(),job.attempt());
    }
    public Optional<JsonNode> marketAssessment(Job job) {
        return jdbc.query("SELECT a.response_json FROM td_attempt a JOIN td_job j ON j.job_id=a.job_id WHERE j.job_id=? AND j.owner_scope=? AND a.response_json IS NOT NULL ORDER BY a.attempt_token",
                (r,n)->json.read(r.getString(1)),job.id(),job.owner()).stream().map(n->n.path("marketAssessment")).filter(JsonNode::isObject).findFirst();
    }
    public Optional<Job> latestInsights(String owner,String code) {
        // Only inspect compact result headers for the most recent reports; legacy decisions remain readable.
        var ids=jdbc.queryForList("SELECT job_id FROM td_job WHERE owner_scope=? AND stock_code=? AND kind='DECISION' AND status='SUCCEEDED' ORDER BY created_epoch DESC,job_id DESC FETCH FIRST 50 ROWS ONLY",String.class,owner,code);
        for(String id:ids) {var job=find(owner,id).orElseThrow();if("NEWS_INSIGHTS".equals(job.result().path("generationMode").asText())) return Optional.of(job);}
        return Optional.empty();
    }
    public Optional<Job> latestInsightsForSnapshot(String owner,String code,String snapshot) {
        for(int page=0;;page++) {
            var rows=jdbc.query("SELECT * FROM td_job WHERE owner_scope=? AND stock_code=? AND snapshot_id=? AND kind='DECISION' AND status='SUCCEEDED' ORDER BY created_epoch DESC,job_id DESC OFFSET ? ROWS FETCH NEXT 50 ROWS ONLY",this::row,owner,code,snapshot,(long)page*50);
            for(var job:rows) if("NEWS_INSIGHTS".equals(job.result().path("generationMode").asText()))return Optional.of(job);
            if(rows.size()<50)return Optional.empty();
        }
    }
    public void ensureIntegratedSchema() {
        try {jdbc.queryForList("SELECT plan_id FROM td_decision_plan WHERE 1=0");jdbc.queryForList("SELECT plan_id FROM td_decision_outcome WHERE 1=0");jdbc.queryForList("SELECT plan_id FROM td_decision_observation WHERE 1=0");}
        catch(org.springframework.dao.DataAccessException e){throw new BusinessException(ErrorCode.DECISION_CONFLICT,"통합 판단 저장 테이블이 필요합니다. 20261116_01_integrated_decision.sql을 적용하세요.");}
    }
    public boolean recordIntegrated(Job job,MarketResponse response,long now) {
        return jdbc.update("UPDATE td_attempt SET response_json=?,input_tokens=?,output_tokens=?,provider_id=? WHERE job_id=? AND attempt_token=? AND EXISTS (SELECT 1 FROM td_job WHERE job_id=? AND owner_scope=? AND attempt_token=? AND lease_owner=? AND status='RUNNING' AND lease_until>?)",
                lob(response.raw()),response.inputTokens(),response.outputTokens(),response.providerId(),job.id(),job.attempt(),job.id(),job.owner(),job.attempt(),job.leaseOwner(),now)==1;
    }
    public void ensureResearchSchema() {
        try {jdbc.queryForList("SELECT document_version_id FROM td_research_document WHERE 1=0");jdbc.queryForList("SELECT event_version_id FROM td_research_event WHERE 1=0");jdbc.queryForList("SELECT source_ref FROM td_research_evidence WHERE 1=0");}
        catch(org.springframework.dao.DataAccessException e) {throw new BusinessException(ErrorCode.DECISION_CONFLICT,"뉴스 2차 저장 테이블이 필요합니다. 20261011_01_news_research.sql을 적용한 후 다시 요청하세요.");}
    }
    public Optional<JsonNode> cachedResearchDocument(String owner,String code,String url,long since) {
        String key=json.hash(Map.of("stock",code,"url",url));
        return jdbc.query("SELECT source_json FROM td_research_document WHERE owner_scope=? AND stock_code=? AND document_key=? AND last_seen_epoch>=? ORDER BY last_seen_epoch DESC FETCH FIRST 1 ROWS ONLY",
                (r,n)->json.read(r.getString(1)),owner,code,key,since).stream().filter(n->n.has("body") && n.path("originalFetchedEpoch").asLong()>=since).findFirst();
    }
    private void archiveResearch(Job job,JsonNode result,long now) {
        Map<String,String> versions=new HashMap<>();
        for(var source:result.path("evidence").path("sources")) {
            String version=json.hash(List.of(job.owner(),source.path("documentVersionId").asText()));versions.put(source.path("id").asText(),version);
            int changed=jdbc.update("UPDATE td_research_document SET last_seen_epoch=?,source_json=? WHERE document_version_id=?",now,lob(source),version);
            if(changed==0) try {jdbc.update("INSERT INTO td_research_document(document_version_id,owner_scope,stock_code,document_key,content_hash,source_json,first_seen_epoch,last_seen_epoch) VALUES(?,?,?,?,?,?,?,?)",
                    version,job.owner(),job.stockCode(),source.path("documentKey").asText(),source.path("contentHash").asText(),lob(source),now,now);}
            catch(DuplicateKeyException race) {jdbc.update("UPDATE td_research_document SET last_seen_epoch=? WHERE document_version_id=?",now,version);}
        }
        for(var event:result.path("report").path("events")) {
            String version=event.path("eventVersionId").asText();
            jdbc.update("INSERT INTO td_research_event(event_version_id,owner_scope,stock_code,event_id,job_id,event_json,created_epoch) VALUES(?,?,?,?,?,?,?)",
                    version,job.owner(),job.stockCode(),event.path("eventId").asText(),job.id(),lob(event),now);
            Set<String> inserted=new HashSet<>();for(var ref:event.path("sourceIds")) {String document=versions.get(ref.asText());if(document!=null && inserted.add(document)) jdbc.update("INSERT INTO td_research_evidence(event_version_id,document_version_id,source_ref) VALUES(?,?,?)",version,document,ref.asText());}
        }
    }
    public Optional<JsonNode> newsEvidence(Job job) {
        return jdbc.query("SELECT a.response_json FROM td_attempt a JOIN td_job j ON j.job_id=a.job_id WHERE j.job_id=? AND j.owner_scope=? AND a.response_json IS NOT NULL ORDER BY a.attempt_token",
                (r,n)->json.read(r.getString(1)),job.id(),job.owner()).stream().map(n->n.path("newsEvidence")).filter(JsonNode::isObject).findFirst();
    }
    public boolean recordNews(Job job,JsonNode evidence,MarketResponse response,long now) {
        Object payload=response==null?Map.of("newsEvidence",evidence):Map.of("newsEvidence",evidence,"insightRaw",response.raw());
        return jdbc.update("UPDATE td_attempt SET response_json=?,input_tokens=?,output_tokens=?,provider_id=? WHERE job_id=? AND attempt_token=? AND EXISTS (SELECT 1 FROM td_job WHERE job_id=? AND owner_scope=? AND attempt_token=? AND lease_owner=? AND status='RUNNING' AND lease_until>?)",
                lob(payload),response==null?0:response.inputTokens(),response==null?0:response.outputTokens(),response==null?null:response.providerId(),job.id(),job.attempt(),job.id(),job.owner(),job.attempt(),job.leaseOwner(),now)==1;
    }
    public boolean recordMarket(Job job,MarketResponse response,long now) {
        // Only the current lease owner can establish a reusable first-pass result.
        return jdbc.update("UPDATE td_attempt SET response_json=?,input_tokens=?,output_tokens=?,provider_id=? WHERE job_id=? AND attempt_token=? AND EXISTS (SELECT 1 FROM td_job WHERE job_id=? AND owner_scope=? AND attempt_token=? AND lease_owner=? AND status='RUNNING' AND lease_until>?)",
                lob(json.tree(Map.of("marketAssessment",response.assessment(),"marketRaw",response.raw()))),response.inputTokens(),response.outputTokens(),response.providerId(),
                job.id(),job.attempt(),job.id(),job.owner(),job.attempt(),job.leaseOwner(),now)==1;
    }
    public void recordPersonalized(Job job,JsonNode assessment,AiResponse response) {
        jdbc.update("UPDATE td_attempt SET provider_id=?,response_json=?,input_tokens=COALESCE(input_tokens,0)+?,output_tokens=COALESCE(output_tokens,0)+? WHERE job_id=? AND attempt_token=?",
                response.providerId(),lob(json.tree(Map.of("marketAssessment",assessment,"personalRaw",response.raw()))),response.inputTokens(),response.outputTokens(),job.id(),job.attempt());
    }
    public void fail(Job job,String error,boolean transientFailure,long now,long delay) {
        tx.executeWithoutResult(s -> {
            boolean retry=transientFailure && job.attempt()<3;
            jdbc.update("UPDATE td_job SET status=?,error_code=?,retryable=?,next_epoch=?,completed_epoch=?,lease_owner=NULL,lease_until=NULL WHERE job_id=? AND owner_scope=? AND status='RUNNING' AND attempt_token=? AND lease_owner=? AND lease_until>?",
                    retry?"RETRYABLE_FAILED":"FAILED",error,retry?1:0,now+delay,retry?null:now,job.id(),job.owner(),job.attempt(),job.leaseOwner(),now);
            jdbc.update("UPDATE td_attempt SET completed_epoch=?,error_code=? WHERE job_id=? AND attempt_token=?",now,error,job.id(),job.attempt());
        });
    }
    public void retry(Job job,long now) {
        if(jdbc.update("UPDATE td_job SET next_epoch=? WHERE job_id=? AND owner_scope=? AND status='RETRYABLE_FAILED' AND retryable=1 AND attempt_token<3",now,job.id(),job.owner())!=1) throw conflict();
    }
    public Job retryRequest(Job job,String key,long now) {
        String hash=json.hash(job.id());
        try {
            return tx.execute(s -> {
                // Row lock serializes retries with each other and worker claims.
                jdbc.queryForObject("SELECT attempt_token FROM td_job WHERE job_id=? AND owner_scope=? FOR UPDATE",Integer.class,job.id(),job.owner());
                var prior=prior(job.owner(),"retry",key,hash); if(prior.isPresent()) return prior.get();
                retry(job,now);
                jdbc.update("INSERT INTO td_request(owner_scope,operation,request_key,request_hash,job_id) VALUES(?,'retry',?,?,?)",job.owner(),key,hash,job.id());
                return find(job.owner(),job.id()).orElseThrow();
            });
        } catch(DuplicateKeyException e) { return prior(job.owner(),"retry",key,hash).orElseThrow(this::conflict); }
    }
    public boolean reserveCall(String owner,String day,int calls,long tokens,long reserve) {
        ensureBudget(owner,day);
        return Boolean.TRUE.equals(tx.execute(s -> {
            lockBudget(owner,day);
            return jdbc.update("UPDATE td_budget SET call_count=call_count+1,token_reservation=token_reservation+? WHERE owner_scope=? AND budget_day=? AND call_count<? AND token_reservation+?<=?",reserve,owner,day,calls,reserve,tokens)==1;
        }));
    }
    public History history(String owner,String code,int page,int size) {
        return history(owner,code,page,size,null);
    }
    public History history(String owner,String code,int page,int size,String snapshotId) {
        List<Object> args=new ArrayList<>(List.of(owner,code));
        String filter=snapshotId==null?"":" AND snapshot_id=?";
        if(snapshotId!=null) args.add(snapshotId);args.add((long)page*size);args.add(size+1);
        var items=jdbc.query("SELECT job_id,stock_code,snapshot_id,parent_id,status,final_action,created_epoch,completed_epoch,error_code,input_json FROM td_job WHERE owner_scope=? AND kind='DECISION' AND stock_code=?"+filter+" ORDER BY created_epoch DESC,job_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                (r,n)->{var input=json.read(r.getString(10));return new Summary(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getLong(7),r.getObject(8,Long.class),r.getString(9),input.path("mode").asText("LEGACY_DECISION"),input.path("versions").path("contract").asText(null));},args.toArray());
        return new History(items.stream().limit(size).toList(),page,size,items.size()>size);
    }
    private void ensureBudget(String owner,String day) {
        try { jdbc.update("INSERT INTO td_budget(owner_scope,budget_day) VALUES(?,?)",owner,day); } catch(DuplicateKeyException alreadyPresent) { /* immutable day key */ }
    }
    private void lockBudget(String owner,String day) { jdbc.queryForObject("SELECT request_count FROM td_budget WHERE owner_scope=? AND budget_day=? FOR UPDATE",Integer.class,owner,day); }
    private Job row(ResultSet r,int n) throws SQLException {
        String result=r.getString("result_json");
        return new Job(r.getString("job_id"),r.getString("owner_scope"),r.getString("kind"),r.getString("stock_code"),r.getString("snapshot_id"),r.getString("parent_id"),
                r.getString("status"),json.read(r.getString("input_json")),result==null?null:json.read(result),r.getString("result_hash"),r.getString("final_action"),r.getLong("created_epoch"),
                r.getObject("completed_epoch",Long.class),r.getInt("attempt_token"),r.getString("lease_owner"),r.getObject("lease_until",Long.class),r.getString("error_code"),r.getInt("retryable")==1);
    }
    private BusinessException conflict() { return new BusinessException(ErrorCode.DECISION_CONFLICT); }
}
