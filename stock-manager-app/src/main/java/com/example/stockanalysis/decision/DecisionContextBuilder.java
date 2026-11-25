package com.example.stockanalysis.decision;

import com.example.stockanalysis.holdings.HoldingModels;
import com.example.stockanalysis.judgment.*;
import com.example.stockanalysis.unified.application.UnifiedAnalysisResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Captures the current registered balance, never trusts an old analysis's personal opinion. */
public final class DecisionContextBuilder {
    public static final String VERSION="integrated-policy-v4-held-sizing";
    private final DecisionJson json;
    public DecisionContextBuilder(DecisionJson json) {this.json=json;}
    public ObjectNode build(DecisionModels.Job snapshot,UnifiedAnalysisResponse data,HoldingModels.Snapshot balance,
                            DecisionModels.Job research,int horizon,Instant now) {
        var result=data.result();
        var judgment=new TradeJudgmentEngine().evaluate(new TradeJudgmentEngine.Input(result.analysis(),result.stock().currentPrice(),result.priceDate(),result.asOf().atZone(ZoneId.of("Asia/Seoul")).toInstant(),data.collectionQuality().name()));
        var scenario=judgment.newEntry()==null || judgment.newEntry().effectiveScenario()==null?result.analysis().scenario():judgment.newEntry().effectiveScenario();
        var holding=HoldingModels.evaluate(balance,snapshot.stockCode(),result.stock().currentPrice(),result.priceDate(),
                scenario==null?null:scenario.stopLoss(),scenario==null?List.of():scenario.targets(),judgment);
        // Existing evaluator reports technical references; private stop/target must be explicit as separate context.
        var p=(ObjectNode)json.tree(Map.of());
        p.put("snapshotId",snapshot.id());p.put("snapshotHash",snapshot.resultHash());p.put("capturedAt",now.toString());
        p.put("priceDate",result.priceDate().toString());p.put("analysisAsOf",result.asOf().toString());
        p.put("priceBasis",data.eod()==null?"KIWOOM_KRX_DAILY_ADJUSTED":data.eod().priceBasis());
        p.put("horizonTradingDays",horizon);p.set("stock",json.tree(result.stock()));
        p.put("monitoring","DAILY");
        p.put("collectionQuality",data.collectionQuality().name());p.set("holding",json.tree(holding));p.set("balanceSnapshot",json.tree(balance));
        p.set("tradeJudgment",json.tree(judgment));p.set("plan",json.tree(scenario));
        p.put("engineAction",map(holding.action(),holding.positionStatus()));
        p.put("entryEligible",judgment.newEntry()!=null && judgment.newEntry().status()==NewEntryAssessment.Status.ELIGIBLE);
        var registered=holding.holding();
        p.put("personalStopBreached",registered!=null && registered.stopPrice()!=null && result.stock().currentPrice().compareTo(registered.stopPrice())<=0);
        p.set("priceHistory",json.tree(data.priceHistory()));
        p.set("research",research(research,now,result.priceDate()));
        p.put("analysisTiming",timing(result.priceDate(),now));
        var evidence=p.putObject("evidence");
        evidence.set("QUALITY",json.tree(Map.of("collection",data.collectionQuality(),"warnings",data.collectionWarnings())));
        evidence.set("RULES",json.tree(judgment));evidence.set("HOLDING",json.tree(holding));evidence.set("PLAN",json.tree(scenario));
        evidence.set("ANALYSIS",json.tree(result.analysis()));evidence.set("RESEARCH",p.path("research"));
        return p;
    }
    public static String timing(LocalDate price,Instant now) {
        long gap=ChronoUnit.DAYS.between(price,now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate());
        return gap<0?"FUTURE":gap>7?"EXPIRED":"VALID";
    }
    private JsonNode research(DecisionModels.Job job,Instant now,LocalDate priceDate) {
        var node=(ObjectNode)json.tree(Map.of());node.put("status","MISSING");node.putNull("jobId");node.set("sources",json.tree(List.of()));
        if(job==null) return node;
        var r=job.result();node.put("jobId",job.id());node.put("resultHash",job.resultHash());
        node.set("report",r.path("report"));node.set("sources",new NewsResearch(json).modelSources(r.path("evidence")));
        node.set("signalContext",r.path("signalContext"));node.put("collectedAt",r.path("evidence").path("collectedAt").asText());
        String status="INVALID";
        try {
            Instant collected=Instant.parse(node.path("collectedAt").asText());
            long days=ChronoUnit.DAYS.between(collected.atZone(ZoneId.of("Asia/Seoul")).toLocalDate(),now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate());
            boolean within="VALID".equals(timing(priceDate,collected));
            status=collected.isAfter(now) || job.completedAt()==null || job.completedAt()>now.toEpochMilli()?"FUTURE":days>7 || !within?"EXPIRED":
                    node.path("sources").isEmpty() || "INSUFFICIENT".equals(r.path("report").path("signalInterpretation").path("stance").asText("INSUFFICIENT"))?"INSUFFICIENT":"VALID";
            // Metadata alone is not enough to make a title-only report valid.
            boolean detailed=false;for(var s:node.path("sources")) if(!s.path("contextOutsideWindow").asBoolean() && Set.of("OFFICIAL_DOCUMENT","SEARCH_SNIPPET","FULL_TEXT").contains(s.path("depth").asText())) detailed=true;
            if("VALID".equals(status) && !detailed) status="INSUFFICIENT";
        } catch(RuntimeException ignored) { /* fail closed */ }
        node.put("status",status);return node;
    }
    public static String map(String action,String position) {
        if("UNKNOWN".equals(position))return "DEFER";
        return switch(action) {
            case "BUY_CANDIDATE","STRONG_BUY_CANDIDATE" -> "BUY";
            case "ADD_AVERAGING_DOWN","ADD_PYRAMID","ADD_REVIEW" -> "ADD";
            case "HOLD","REVIEW_HOLDING" -> "HOLD";
            case "EXIT_TRIGGERED","SELL_PRIORITY" -> "SELL";
            case "TAKE_PROFIT_REVIEW" -> "REDUCE";
            case "WATCH","AVOID" -> "WAIT"; default -> "DEFER";
        };
    }
}
