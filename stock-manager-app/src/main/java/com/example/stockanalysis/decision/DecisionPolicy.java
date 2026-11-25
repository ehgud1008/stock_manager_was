package com.example.stockanalysis.decision;

import static com.example.stockanalysis.decision.DecisionModels.*;
import com.example.stockanalysis.analysis.domain.DataQualityStatus;
import com.example.stockanalysis.engine.model.TradeSignal;
import com.example.stockanalysis.unified.application.UnifiedAnalysisResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class DecisionPolicy {
    public static final String VERSION="decision-policy-v5-holdings";
    public static final Map<String,String> CONDITIONS=Map.of(
            "UPSIDE","진입 상단을 돌파하고 유효 매수 신호가 유지되는 경우",
            "RANGE","진입 구간 안에서 추세 전환이 확인되지 않는 경우",
            "DOWNSIDE","시나리오 손절선을 이탈하거나 매도 신호가 확인되는 경우");
    private final DecisionJson json;
    public DecisionPolicy(DecisionJson json) { this.json=json; }

    public JsonNode prepare(UnifiedAnalysisResponse data, ReviewRequest request) {
        var result=data.result(); var analysis=result.analysis(); var report=analysis.signalReport();
        var calculator=new HorizonPlanCalculator(json);
        int referenceDays=request.horizonTradingDays()==null?20:request.horizonTradingDays();
        var calculationContext=new ReviewRequest(request.positionStatus(),referenceDays,request.averageBuyPrice(),request.purchasedOn(),request.preferences(),request.quantity(),request.balanceDate());
        var horizonPlan=calculator.calculate(data,calculationContext);
        if(request.horizonTradingDays()==null) {
            ((ObjectNode)horizonPlan.path("metrics")).remove("HORIZON");
            horizonPlan.put("referenceOnly",true);
        }
        var plan=horizonPlan.path("plan").isNull()?null:json.convert(horizonPlan.path("plan"),com.example.stockanalysis.engine.model.TradeScenario.class);
        var active=report==null?List.<TradeSignal>of():report.signals().stream()
                .filter(s->s.state()==TradeSignal.State.CONFIRMED || s.state()==TradeSignal.State.ACTIVE).toList();
        boolean ready=data.collectionQuality()==DataQualityStatus.COMPLETE && analysis.totalScore()!=null
                && "VALID".equals(result.screener().quality().name()) && report!=null && "READY".equals(report.completeness());
        boolean sell=active.stream().anyMatch(s->s.side()==TradeSignal.Side.SELL);
        boolean buy=plan!=null && active.stream().anyMatch(s->s.side()==TradeSignal.Side.BUY && "IN_RANGE".equals(s.entryStatus()))
                && result.stock().currentPrice().compareTo(plan.entryFrom())>=0 && result.stock().currentPrice().compareTo(plan.entryTo())<=0 && !sell;
        List<String> allowed=new ArrayList<>(); String block=null;
        if(request.positionStatus()==Position.UNKNOWN) block="POSITION_UNKNOWN";
        else if(Set.of("HISTORY_REQUIRED","HISTORY_INVALID").contains(horizonPlan.path("status").asText())) block=horizonPlan.path("status").asText();
        if(block!=null || !ready) allowed.add("UNDETERMINED");
        else if(request.positionStatus()==Position.NOT_HELD) { allowed.add("WAIT"); if(buy) allowed.add("BUY"); allowed.add("UNDETERMINED"); }
        else { allowed.add("HOLD"); if(sell) allowed.add("SELL"); allowed.add("UNDETERMINED"); }
        ObjectNode p=(ObjectNode)json.tree(new LinkedHashMap<>());
        p.put("policyVersion",VERSION); p.set("engineActions",json.tree(allowed));
        p.put("engineAssessment",!ready?"UNDETERMINED":request.positionStatus()==Position.HELD?(sell?"SELL":"HOLD"):(buy?"BUY":"WAIT"));
        p.put("engineDataReady",ready);
        p.set("context",json.tree(request));
        p.set("preferences",json.tree(request.effectivePreferences()));p.put("horizonMode",request.horizonTradingDays()==null?"AI_PROPOSED":"FIXED");
        var candles=data.priceHistory().stream().filter(b->!b.businessDate().isAfter(result.priceDate()))
                .sorted(Comparator.comparing(com.example.stockanalysis.engine.model.DailyCandle::businessDate)).toList();
        candles=candles.subList(Math.max(0,candles.size()-300),candles.size());
        p.set("marketData",json.tree(Map.of("columns",List.of("date","open","high","low","close","volume"),
                "priceDate",result.priceDate().toString(),"candles",candles.stream().map(b->Arrays.asList(b.businessDate().toString(),b.open(),b.high(),b.low(),b.close(),b.volume())).toList())));
        p.put("blockReason",block); p.put("planId",plan==null?null:"HORIZON_"+referenceDays); p.set("plan",json.tree(plan));
        p.set("horizonPlan",horizonPlan);p.set("metrics",horizonPlan.path("metrics"));
        var comparisons=json.tree(List.of());
        var choices=new TreeSet<>(List.of(5,20,60,referenceDays));
        for(int days:choices) ((com.fasterxml.jackson.databind.node.ArrayNode)comparisons).add(days==referenceDays?horizonPlan:
                calculator.calculate(data,new ReviewRequest(request.positionStatus(),days,request.averageBuyPrice(),request.purchasedOn())));
        p.set("comparisons",comparisons);
        p.put("priceDate",result.priceDate().toString()); p.put("asOf",result.asOf().toString());
        p.set("stock",json.tree(result.stock()));
        p.set("rewardRisk",json.tree(plan==null?null:plan.targets().get(0).subtract(plan.entryTo()).divide(plan.entryTo().subtract(plan.stopLoss()),6,RoundingMode.HALF_UP)));
        var evidence=new LinkedHashMap<String,Object>();
        evidence.put("QUALITY",Map.of("collection",data.collectionQuality(),"warnings",data.collectionWarnings(),"engineWarnings",result.warnings()));
        var trend=new LinkedHashMap<String,Object>();
        trend.put("quality",result.screener().quality()); trend.put("stage",result.screener().stage());
        trend.put("ema",result.screener().ema()); trend.put("slopes",result.screener().slopes()); trend.put("features",result.screener().features());
        evidence.put("TREND",trend);
        evidence.put("VALUATION",analysis.strategy()); evidence.put("FACTORS",analysis.factors());
        evidence.put("SIGNALS",active); evidence.put("POSITION",request.positionStatus());
        evidence.put("PLAN",horizonPlan.path("reason")); evidence.put("HORIZON",Map.of("days",request.horizonTradingDays()==null?"AI_PROPOSED":request.horizonTradingDays(),"status",horizonPlan.path("status"),
                "focus",horizonPlan.path("focus"),"distanceAssessment",horizonPlan.path("distanceAssessment"),"signalExpiresBeforeHorizon",horizonPlan.path("signalExpiresBeforeHorizon")));
        evidence.put("PRICE_STRUCTURE",Map.of("support",horizonPlan.path("support"),"targets",horizonPlan.path("targetLevels")));
        evidence.put("WEEKLY",horizonPlan.has("weeklyTrend")?horizonPlan.path("weeklyTrend"):json.tree(Map.of("status","UNAVAILABLE")));
        evidence.put("TIME_RULE",horizonPlan.has("timeRule")?horizonPlan.path("timeRule"):json.tree("기간 계산 자료 부족. 선택 기간 내 추세와 신호를 다시 확인할 시점을 제안하세요."));
        evidence.put("MARKET_DATA",Map.of("rows",candles.size(),"priceDate",result.priceDate().toString(),"location","marketData.candles"));
        evidence.put("HOLDING",json.tree(request));
        evidence.put("PREFERENCES",json.tree(request.effectivePreferences()));
        p.set("evidence",json.tree(evidence)); p.set("conditions",json.tree(CONDITIONS));
        p.put("limitations","AI는 엔진과 다른 의견과 전망 가격을 제안할 수 있습니다. 근거 참조와 산술 검증은 해석의 정확성·수익성·도달 확률을 보장하지 않습니다. 개인 손절 한도·계좌 비중·별도 뉴스 검색·실시간 가격·수수료·세금은 반영하지 않았습니다.");
        return p;
    }
    public JsonNode finish(JsonNode input,Review review,long now) {
        JsonNode policy=input.path("policy");
        String action=review==null?"UNDETERMINED":review.recommendation();
        if(!AiReviewContract.actions(input.path("context").path("positionStatus").asText()).contains(action)) throw new AiFailure("AI_INVALID_ACTION",false);
        ObjectNode result=(ObjectNode)json.tree(new LinkedHashMap<>());
        result.put("finalAction",action); result.put("generationMode",review==null?"POLICY_ONLY":"AI_REVIEWED");
        result.set("aiReview",json.tree(review)); result.set("policy",policy); result.set("context",input.path("context"));
        result.set("strategyCalculations",review==null?json.tree(List.of()):new AiStrategyCalculator(json).calculate(review,input));
        result.put("validationScope","EVIDENCE_REFERENCES_AND_ARITHMETIC_ONLY");
        result.set("versions",input.path("versions")); result.put("inputHash",json.hash(input)); result.put("completedAt",now);
        result.put("probabilityStatus","UNVALIDATED"); result.putNull("winProbability"); result.putNull("expectedReturn");
        result.put("exitReason","SELL".equals(action)?"AI_INDEPENDENT_EXIT":null);
        return result;
    }
}
