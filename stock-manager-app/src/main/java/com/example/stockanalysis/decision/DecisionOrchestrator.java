package com.example.stockanalysis.decision;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import java.time.Clock;

@Component
public class DecisionOrchestrator {
    private final DecisionStore store;private final DecisionJson json;private final Clock clock;
    public DecisionOrchestrator(DecisionStore store,DecisionJson json,Clock clock){this.store=store;this.json=json;this.clock=clock;}
    public JsonNode run(DecisionModels.Job job,AiReviewPort ai,Runnable reserve) {
        if(!IntegratedDecisionContract.VERSION.equals(job.input().path("versions").path("contract").asText())
                || !java.util.Set.of(DecisionContextBuilder.VERSION,"integrated-policy-v3-account-risk","integrated-policy-v2-validity","integrated-policy-v1").contains(job.input().path("versions").path("policy").asText()))
            throw new DecisionModels.AiFailure("POLICY_VERSION_CHANGED",false);
        if(job.input().path("policy").has("accountRisk") && !(DecisionContextBuilder.VERSION.equals(job.input().path("versions").path("policy").asText())
                ? PositionSizingPolicy.VERSION : PositionSizingPolicy.LEGACY_VERSION).equals(job.input().path("versions").path("sizing").asText()))
            throw new DecisionModels.AiFailure("POLICY_VERSION_CHANGED",false);
        var arbitrator=new DecisionArbitrator(json);
        if(job.input().path("policy").path("validity").isObject()) {
            String status=DecisionValidityPolicy.current(job.input().path("policy"),clock.instant()).path("status").asText();
            if(!"VALID".equals(status))return arbitrator.finish(job.input(),null,"DECISION_"+status,clock.millis());
        }
        if("UNKNOWN".equals(job.input().path("policy").path("holding").path("positionStatus").asText()))
            return arbitrator.finish(job.input(),null,"POSITION_UNKNOWN",clock.millis());
        try {
            reserve.run();var response=ai.assess(job.input());
            if(!store.recordIntegrated(job,response,clock.millis()))throw new DecisionModels.AiFailure("LEASE_LOST",false);
            new IntegratedDecisionContract(json).validate(response.assessment(),job.input().path("policy"));
            return arbitrator.finish(job.input(),response.assessment(),null,clock.millis());
        } catch(DecisionModels.AiFailure e) {
            if(e.retryable() || "LEASE_LOST".equals(e.code()))throw e;
            return arbitrator.finish(job.input(),null,e.code(),clock.millis());
        }
    }
}
