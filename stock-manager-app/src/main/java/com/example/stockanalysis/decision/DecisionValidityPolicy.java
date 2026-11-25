package com.example.stockanalysis.decision;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.*;

/** Temporal checks at finalization as well as request time; queued jobs can expire. */
public final class DecisionValidityPolicy {
    /** Uses only frozen dates; safe to call while committing a job or replaying its plan. */
    public static JsonNode current(JsonNode policy,Instant now) {
        var out=com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();var reasons=out.putArray("reasonCodes");
        var frozen=policy.path("validity");out.put("policyVersion",DecisionValidityService.VERSION);out.put("evaluatedAt",now.toString());
        out.put("scope","STORED_EVIDENCE_ONLY");out.put("calendarVersion",frozen.path("calendarVersion").asText("UNKNOWN"));
        out.put("priceDate",policy.path("priceDate").asText());out.putNull("validUntil");out.putNull("nextReviewAt");
        out.putNull("elapsedTradingDays");out.putNull("signalRemainingTradingDays");out.put("elapsedCountCapped",false);
        String status="UNVERIFIABLE";int elapsed=0;
        try {
            LocalDate price=LocalDate.parse(policy.path("priceDate").asText());
            if(price.isAfter(now.atZone(DecisionService.SEOUL).toLocalDate()) || policy.has("analysisAsOf") && LocalDateTime.parse(policy.path("analysisAsOf").asText()).atZone(DecisionService.SEOUL).toInstant().isAfter(now))reasons.add("ANALYSIS_FUTURE");
            else if(!DecisionValidityService.VERSION.equals(frozen.path("version").asText()))reasons.add("VALIDITY_POLICY_UNAVAILABLE");
            else if(!"AVAILABLE".equals(frozen.path("calendarStatus").asText()) || frozen.path("sessions").size()!=DecisionValidityService.MAX_SESSIONS)reasons.add("CHANGED".equals(frozen.path("calendarStatus").asText())?"CALENDAR_CHANGED":"CALENDAR_UNAVAILABLE");
            else {
                var sessions=frozen.path("sessions");
                for(var session:sessions)if(!Instant.parse(session.path("reviewAt").asText()).isAfter(now))elapsed++;
                out.put("elapsedTradingDays",elapsed);out.put("elapsedCountCapped",elapsed==sessions.size());
                Instant deadline=Instant.parse(sessions.get(sessions.size()-1).path("reviewAt").asText());
                String signal=frozen.path("signalStatus").asText();
                if("INVALID".equals(signal))reasons.add("SIGNAL_DATA_UNAVAILABLE");
                else if("EXPIRED".equals(signal)){status="EXPIRED";out.put("signalRemainingTradingDays",0);reasons.add("SIGNAL_EXPIRED");deadline=price.atStartOfDay(DecisionService.SEOUL).toInstant();}
                else {
                    if("VALID".equals(signal)) {
                        int remaining=frozen.path("signalRemainingAtPrice").asInt();out.put("signalRemainingTradingDays",Math.max(0,remaining-elapsed));
                        if(remaining<1)throw new IllegalArgumentException();
                        if(remaining<=sessions.size())deadline=Instant.parse(sessions.get(remaining-1).path("reviewAt").asText());
                    }
                    status=!deadline.isAfter(now)?"EXPIRED":elapsed>0?"REVIEW_REQUIRED":"VALID";
                    if(status.equals("EXPIRED"))reasons.add("VALID".equals(signal) && frozen.path("signalRemainingAtPrice").asInt()<=elapsed?"SIGNAL_EXPIRED":"ANALYSIS_EXPIRED");
                    else if(status.equals("REVIEW_REQUIRED"))reasons.add("NEW_TRADING_SESSION_COMPLETED");
                }
                out.put("validUntil",deadline.toString());
                if(elapsed<sessions.size())out.put("nextReviewAt",sessions.get(elapsed).path("reviewAt").asText());
            }
        }catch(RuntimeException e){status="UNVERIFIABLE";reasons.add("VALIDITY_DATA_INVALID");}
        String research=policy.path("research").path("status").asText("MISSING");
        if("VALID".equals(research)) {
            try {
                var collected=Instant.parse(policy.path("research").path("collectedAt").asText());
                var rs=frozen.path("researchSchedule");
                if(collected.isAfter(now))research="FUTURE";
                else if(!"AVAILABLE".equals(rs.path("calendarStatus").asText()) || rs.path("sessions").size()!=DecisionValidityService.MAX_SESSIONS)research="UNVERIFIABLE";
                else if(!Instant.parse(rs.path("sessions").get(DecisionValidityService.MAX_SESSIONS-1).path("reviewAt").asText()).isAfter(now))research="EXPIRED";
            }catch(RuntimeException e){research="UNVERIFIABLE";}
        }
        boolean refresh=!status.equals("VALID");boolean researchRefresh=refresh || !research.equals("VALID");
        out.put("status",status);out.put("researchStatus",research);out.put("requiresAnalysisRefresh",refresh);
        out.put("requiresResearchRefresh",researchRefresh);out.put("entryBlocked",refresh || !research.equals("VALID"));
        var steps=out.putArray("refreshSteps");if(refresh)steps.add("ANALYSIS");if(researchRefresh)steps.add("RESEARCH");if(refresh || researchRefresh)steps.add("DECISION");
        return out;
    }
    public List<String> entryBlocks(JsonNode policy,Instant now) {
        var blocks=new ArrayList<String>();
        if(policy.path("validity").isObject()) {
            var current=current(policy,now);
            if(!"VALID".equals(current.path("status").asText()))blocks.add("DECISION_"+current.path("status").asText());
            if(!"VALID".equals(current.path("researchStatus").asText()))blocks.add("RESEARCH_"+current.path("researchStatus").asText());
            return blocks;
        }
        String timing=analysisTiming(policy,now);
        if(!"VALID".equals(timing))blocks.add("ANALYSIS_"+timing);
        String research=policy.path("research").path("status").asText("MISSING");
        if("VALID".equals(research)) {
            try {
                Instant collected=Instant.parse(policy.path("research").path("collectedAt").asText());
                if(collected.isAfter(now))research="FUTURE";
                else if(!"VALID".equals(DecisionContextBuilder.timing(collected.atZone(DecisionService.SEOUL).toLocalDate(),now)))research="EXPIRED";
            } catch(RuntimeException e){research="INVALID";}
        }
        if(!"VALID".equals(research))blocks.add("RESEARCH_"+research);
        return blocks;
    }
    public String analysisTiming(JsonNode policy,Instant now) {
        try {
            String timing=DecisionContextBuilder.timing(LocalDate.parse(policy.path("priceDate").asText()),now);
            if(policy.has("analysisAsOf") && LocalDateTime.parse(policy.path("analysisAsOf").asText()).atZone(DecisionService.SEOUL).toInstant().isAfter(now))return "FUTURE";
            return timing;
        }catch(RuntimeException e){return "INVALID";}
    }
    public boolean validPlan(JsonNode plan) {
        if(!plan.isObject())return false;
        for(String f:List.of("entryFrom","entryTo","stopLoss"))if(!plan.path(f).isNumber() || plan.path(f).decimalValue().signum()<=0)return false;
        if(plan.path("stopLoss").decimalValue().compareTo(plan.path("entryFrom").decimalValue())>=0 || plan.path("entryFrom").decimalValue().compareTo(plan.path("entryTo").decimalValue())>0 || plan.path("targets").isEmpty())return false;
        var previous=plan.path("entryTo").decimalValue();
        for(var target:plan.path("targets")) {if(!target.isNumber() || target.decimalValue().compareTo(previous)<=0)return false;previous=target.decimalValue();}
        return true;
    }
}
