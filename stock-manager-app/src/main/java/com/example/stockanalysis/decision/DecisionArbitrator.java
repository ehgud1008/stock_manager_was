package com.example.stockanalysis.decision;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;

/** A review is never an authorization to submit an order. */
public final class DecisionArbitrator {
    private final DecisionJson json;
    public DecisionArbitrator(DecisionJson json) {this.json=json;}
    public ObjectNode finish(JsonNode input,JsonNode review,String error,long now) {
        var p=input.path("policy");String engine=p.path("engineAction").asText("DEFER");
        var validity=new DecisionValidityPolicy();String timing=validity.analysisTiming(p,java.time.Instant.ofEpochMilli(now));
        JsonNode temporal=p.path("validity").isObject()?DecisionValidityPolicy.current(p,java.time.Instant.ofEpochMilli(now)):null;
        if(temporal!=null)timing=temporal.path("status").asText();
        String ai=review==null?"DEFER":review.path("action").asText("DEFER");String action=ai;
        var reasons=new ArrayList<String>();
        boolean heldSizing=PositionSizingPolicy.VERSION.equals(input.path("versions").path("sizing").asText());
        boolean held="HELD".equals(p.path("holding").path("positionStatus").asText());
        String raw=p.path("holding").path("action").asText();
        if(error!=null) reasons.add(error);
        if(Set.of("ADD","REDUCE").contains(action) && !heldSizing) {action=held?"HOLD":"DEFER";reasons.add("QUANTITY_POLICY_UNAVAILABLE");}
        if(Set.of("BUY","ADD").contains(ai)) {
            if(!p.path("entryEligible").asBoolean()) reasons.add("ENTRY_NOT_ELIGIBLE");
            if(!"COMPLETE".equals(p.path("collectionQuality").asText())) reasons.add("ANALYSIS_INCOMPLETE");
            reasons.addAll(validity.entryBlocks(p,java.time.Instant.ofEpochMilli(now)));
            if("RECONSIDER".equals(p.path("research").path("report").path("signalInterpretation").path("stance").asText())) reasons.add("RESEARCH_CONFLICT");
            if(p.path("holding").path("stopBreached").asBoolean()) reasons.add("STOP_REFERENCE_BREACHED");
            if(p.path("personalStopBreached").asBoolean()) reasons.add("PERSONAL_STOP_REVIEW");
            if(p.path("plan").isNull() || p.path("plan").isMissingNode()) reasons.add("PLAN_MISSING");
            else if(!validity.validPlan(p.path("plan")))reasons.add("PLAN_INVALID");
            if(!reasons.isEmpty()) action=held?"HOLD":"WAIT";
        }
        if(!"VALID".equals(timing)) {action="DEFER";add(reasons,(temporal==null?"ANALYSIS_":"DECISION_")+timing);}
        if(!"COMPLETE".equals(p.path("collectionQuality").asText())) {action="DEFER";add(reasons,"ANALYSIS_INCOMPLETE");}
        if(held && p.path("personalStopBreached").asBoolean() && !"SELL".equals(action)) {action="DEFER";add(reasons,"PERSONAL_STOP_REVIEW");}
        if("UNKNOWN".equals(p.path("holding").path("positionStatus").asText())) {action="DEFER";reasons.add("POSITION_UNKNOWN");}
        // Preserve confirmed exit facts even when AI/research is missing. Stale facts remain visible but cannot imply a fresh opinion.
        if(held && Set.of("EXIT_TRIGGERED","SELL_PRIORITY").contains(raw) && "VALID".equals(timing)) {action="SELL";reasons.add("ENGINE_EXIT_PRIORITY");}
        ObjectNode sizing=p.has("accountRisk")?(heldSizing?new PositionSizingPolicy().calculate(p,action,java.time.Instant.ofEpochMilli(now)):new PositionSizingPolicy().calculate(p,java.time.Instant.ofEpochMilli(now))):null;
        if(Set.of("BUY","ADD","REDUCE").contains(action) && (heldSizing || sizing!=null) && (sizing==null || !"READY".equals(sizing.path("status").asText()))) {
            action=held?"HOLD":"WAIT";add(reasons,sizing==null?"ACCOUNT_RISK_MISSING":"ZERO_CAPACITY".equals(sizing.path("status").asText())?"ACCOUNT_LIMIT_REACHED":sizing.path("status").asText());
        }
        var r=(ObjectNode)json.tree(Map.of());
        r.put("generationMode","INTEGRATED_DECISION");r.put("contractVersion",IntegratedDecisionContract.VERSION);
        r.put("engineAction",engine);r.put("aiAction",ai);r.put("decisionAction",action);
        r.put("finalAction",switch(action){case "ADD"->"BUY";case "REDUCE"->"SELL";case "DEFER"->"UNDETERMINED";default->action;});
        r.put("executionStatus","PRE_EXECUTION_ONLY");r.set("blockReasons",json.tree(reasons));r.putNull("quantity");
        r.put("quantityStatus","ACCOUNT_RISK_NOT_PROVIDED");r.set("aiReview",review==null?json.tree(null):review);
        if(sizing!=null) {
            r.set("riskSizing",sizing);r.put("quantityStatus",sizing.path("status").asText());
            if(Set.of("BUY","ADD","REDUCE","SELL").contains(action) && "READY".equals(sizing.path("status").asText()))r.set("quantity",sizing.has("tradeQuantity")?sizing.path("tradeQuantity"):sizing.path("maxBuyQuantity"));
        }
        r.set("context",input.path("context"));r.set("policy",p);r.set("versions",input.path("versions"));
        r.put("inputHash",json.hash(input));r.put("completedAt",now);r.put("probabilityStatus","UNVALIDATED");r.putNull("winProbability");
        r.put("validationScope","REFERENCES_QUOTES_AND_SERVER_RULES");r.put("aiErrorCode",error);
        if(temporal!=null)r.set("validityAtCompletion",temporal);
        return r;
    }
    private void add(List<String> reasons,String value){if(!reasons.contains(value))reasons.add(value);}
}
