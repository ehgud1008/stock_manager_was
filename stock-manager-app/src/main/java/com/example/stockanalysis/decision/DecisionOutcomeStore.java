package com.example.stockanalysis.decision;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.Types;
import java.time.*;
import java.util.*;

@Repository
public class DecisionOutcomeStore {
    private final JdbcTemplate jdbc;private final DecisionJson json;private final TransactionTemplate tx;
    public record Plan(String id,String jobId,String role,String opportunity,JsonNode input,JsonNode outcome) {}
    public record Sample(String stock, String policy, String model, String outcomeVersion,
                         int horizon, JsonNode ai, JsonNode engine) {}
    public DecisionOutcomeStore(DataSource ds,DecisionJson json){jdbc=new JdbcTemplate(ds);this.json=json;tx=new TransactionTemplate(new DataSourceTransactionManager(ds));}
    static Object lob(DecisionJson json,Object value){return new SqlParameterValue(Types.CLOB,new SqlLobValue(json.write(value)));}
    /** Invoked inside the fenced job-completion transaction: both plans or no completion. */
    static void freeze(JdbcTemplate jdbc,DecisionJson json,DecisionModels.Job job,JsonNode result,long now) {
        var p=job.input().path("policy");
        String opportunity=json.hash(List.of(job.owner(),job.snapshotId(),p.path("horizonTradingDays")));
        for(String role:List.of("ENGINE","AI_FINAL")) {
            String id=json.hash(List.of(job.owner(),job.id(),role));
            ObjectNode plan=p.deepCopy();JsonNode decision=result;String action=result.path("decisionAction").asText();
            if(role.equals("ENGINE")) {
                // Same data/risk restrictions in both comparison arms; only AI selection differs.
                ObjectNode baseline=job.input().deepCopy();
                var synthetic=json.tree(Map.of("action",p.path("engineAction").asText()));
                decision=new DecisionArbitrator(json).finish(baseline,synthetic,null,now);action=decision.path("decisionAction").asText();
            }
            plan.put("action",action);plan.put("registeredAt",Instant.ofEpochMilli(now).toString());
            if(decision.has("riskSizing")){plan.set("riskSizing",decision.path("riskSizing"));plan.set("quantity",decision.path("quantity"));}
            String outcomeVersion=PositionSizingPolicy.VERSION.equals(job.input().path("versions").path("sizing").asText())?PositionOutcomeEngine.VERSION:DecisionOutcomeEngine.VERSION;
            plan.put("role",role);plan.put("evalPolicyVersion",outcomeVersion);
            plan.put("roundTripFeeBps",PositionSizingPolicy.FEE_BPS);plan.put("slippageBps",PositionSizingPolicy.SLIP_BPS);
            String research=p.path("research").path("jobId").asText(null);
            Object revision=p.path("holding").path("holding").path("revision").isNumber()?p.path("holding").path("holding").path("revision").asLong():null;
            jdbc.update("INSERT INTO td_decision_plan(plan_id,owner_scope,job_id,snapshot_id,research_job_id,holding_revision,opportunity_key,plan_role,horizon_trading_days,policy_version,plan_json,created_epoch) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
                    id,job.owner(),job.id(),job.snapshotId(),research,revision,opportunity,role,p.path("horizonTradingDays").asInt(),job.input().path("versions").path("policy").asText(DecisionContextBuilder.VERSION),lob(json,plan),now);
            jdbc.update("INSERT INTO td_decision_outcome(owner_scope,plan_id,outcome_status,outcome_json,eval_policy_version,updated_epoch,next_epoch) VALUES(?,?,'WAITING_DATA',?,?,?,?)",
                    job.owner(),id,lob(json,Map.of("status","WAITING_DATA","policyVersion",outcomeVersion)),outcomeVersion,now,now);
        }
    }
    public List<Plan> plans(String owner,String job){return jdbc.query("SELECT p.*,o.outcome_json FROM td_decision_plan p JOIN td_decision_outcome o ON o.owner_scope=p.owner_scope AND o.plan_id=p.plan_id WHERE p.owner_scope=? AND p.job_id=? ORDER BY p.plan_role",
            (r,n)->new Plan(r.getString("plan_id"),r.getString("job_id"),r.getString("plan_role"),r.getString("opportunity_key"),json.read(r.getString("plan_json")),json.read(r.getString("outcome_json"))),owner,job);}
    public List<Sample> samples(String owner, long from, long until, String stock) {
        return jdbc.query("""
                WITH ranked AS (
                    SELECT p.plan_id,p.owner_scope,p.job_id,p.created_epoch,p.policy_version,p.horizon_trading_days,
                           ROW_NUMBER() OVER (PARTITION BY p.opportunity_key ORDER BY p.created_epoch,p.job_id) AS representative
                      FROM td_decision_plan p
                     WHERE p.owner_scope=? AND p.plan_role='AI_FINAL'
                )
                SELECT j.stock_code,a.policy_version,j.input_json,a.horizon_trading_days,
                       ao.eval_policy_version,ao.outcome_json AS ai_json,eo.outcome_json AS engine_json
                  FROM ranked a JOIN td_job j ON j.job_id=a.job_id AND j.owner_scope=a.owner_scope
                  LEFT JOIN td_decision_outcome ao ON ao.plan_id=a.plan_id AND ao.owner_scope=a.owner_scope
                  LEFT JOIN td_decision_plan e ON e.job_id=a.job_id AND e.owner_scope=a.owner_scope AND e.plan_role='ENGINE'
                  LEFT JOIN td_decision_outcome eo ON eo.plan_id=e.plan_id AND eo.owner_scope=a.owner_scope
                 WHERE a.representative=1 AND a.created_epoch>=? AND a.created_epoch<?
                """ + (stock == null ? "" : " AND j.stock_code=?"), (r,n) -> {
            var versions = json.read(r.getString("input_json")).path("versions");
            String ai = r.getString("ai_json"), engine = r.getString("engine_json");
            return new Sample(r.getString("stock_code"),r.getString("policy_version"),versions.path("model").asText("UNKNOWN"),
                    r.getString("eval_policy_version"),r.getInt("horizon_trading_days"),
                    ai == null ? null : json.read(ai),engine == null ? null : json.read(engine));
        }, stock == null ? new Object[]{owner,from,until} : new Object[]{owner,from,until,stock});
    }
    public List<String> due(String owner,long now){return jdbc.queryForList("SELECT p.job_id FROM td_decision_plan p JOIN td_decision_outcome o ON o.owner_scope=p.owner_scope AND o.plan_id=p.plan_id WHERE p.owner_scope=? AND o.next_epoch<=? GROUP BY p.job_id ORDER BY MIN(o.next_epoch),MIN(p.created_epoch),p.job_id FETCH FIRST 10 ROWS ONLY",String.class,owner,now);}
    public List<JsonNode> observations(String owner,String plan){return jdbc.query("SELECT observation_json FROM td_decision_observation WHERE owner_scope=? AND plan_id=? ORDER BY trade_date",(r,n)->json.read(r.getString(1)),owner,plan);}
    /** Serializes observers across server instances; first persisted price evidence is immutable. */
    public void observe(String owner,Plan plan,List<JsonNode> samples,java.util.function.Function<List<JsonNode>,JsonNode> evaluate,long now) {
        tx.executeWithoutResult(s->{
            jdbc.queryForObject("SELECT updated_epoch FROM td_decision_outcome WHERE owner_scope=? AND plan_id=? FOR UPDATE",Long.class,owner,plan.id());
            Set<String> existing=new HashSet<>();for(var o:observations(owner,plan.id()))existing.add(o.path("date").asText());
            for(var o:samples) if(existing.add(o.path("date").asText()))jdbc.update("INSERT INTO td_decision_observation(owner_scope,plan_id,trade_date,observation_json,data_quality,collected_epoch) VALUES(?,?,?,?,?,?)",
                    owner,plan.id(),java.sql.Date.valueOf(o.path("date").asText()),lob(json,o),o.path("quality").asText(),Instant.parse(o.path("collectedAt").asText()).toEpochMilli());
            JsonNode outcome=evaluate.apply(observations(owner,plan.id()));
            boolean done=Set.of("CLOSED","NOT_ENTERED","OBSERVED","DEFERRED").contains(outcome.path("status").asText());
            if(PositionOutcomeEngine.VERSION.equals(plan.input().path("evalPolicyVersion").asText())
                    && Set.of("CLOSED","NOT_ENTERED").contains(outcome.path("status").asText())
                    && outcome.path("observedTradingDays").asInt()<plan.input().path("horizonTradingDays").asInt())done=false;
            jdbc.update("UPDATE td_decision_outcome SET outcome_status=?,outcome_json=?,updated_epoch=?,next_epoch=? WHERE owner_scope=? AND plan_id=?",
                    outcome.path("status").asText(),lob(json,outcome),now,done?Long.MAX_VALUE:now+300000,owner,plan.id());
        });
    }
}
