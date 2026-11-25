package com.example.stockanalysis.decision;

import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.shared.exception.*;
import com.example.stockanalysis.unified.eod.TradingCalendar;
import com.example.stockanalysis.unified.performance.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class DecisionOutcomeService {
    private final DecisionOutcomeStore outcomes;private final DecisionStore jobs;private final DecisionSettings settings;
    private final DecisionJson json;private final TradingCalendar calendar;private final PerformanceSources prices;private final Clock clock;
    public DecisionOutcomeService(DecisionOutcomeStore outcomes,DecisionStore jobs,DecisionSettings settings,DecisionJson json,TradingCalendar calendar,PerformanceSources prices,Clock clock) {
        this.outcomes=outcomes;this.jobs=jobs;this.settings=settings;this.json=json;this.calendar=calendar;this.prices=prices;this.clock=clock;
    }
    public Map<String,Object> get(String id) {
        var job=job(id);
        var plans=outcomes.plans(settings.ownerScope(),id);
        return Map.of("jobId",id,"status",plans.isEmpty()?"WAITING_DECISION":"AVAILABLE","plans",plans,
                "simulationOnly",true,"executionStatus","PRE_EXECUTION_ONLY");
    }
    public Map<String,Object> comparison(String id) {
        job(id);var plans=outcomes.plans(settings.ownerScope(),id);
        Map<String,Object> result=new LinkedHashMap<>();result.put("jobId",id);result.put("plans",plans);
        result.put("scope","SAME_SNAPSHOT_HORIZON_SHARED_PLAN_AND_COSTS");
        result.put("opportunityKey",plans.isEmpty()?null:plans.get(0).opportunity());
        result.put("aggregationRule","COUNT_OPPORTUNITY_ONCE_SELECT_ONE_REVISION");
        var engine=plans.stream().filter(p->p.role().equals("ENGINE")).findFirst();
        var ai=plans.stream().filter(p->p.role().equals("AI_FINAL")).findFirst();
        result.put("status","WAITING_DATA");result.put("netReturnDifferencePct",null);
        result.put("forwardPnlDifferenceAmount",null);
        if(engine.isPresent() && ai.isPresent()) {
            var a=ai.get().outcome();var e=engine.get().outcome();
            if(terminal(a) && terminal(e) && a.path("netReturnPct").isNumber() && e.path("netReturnPct").isNumber()) {
                boolean sameCapital=!a.path("startingCapital").isNumber() || !e.path("startingCapital").isNumber()
                        || a.path("startingCapital").decimalValue().compareTo(e.path("startingCapital").decimalValue())==0;
                result.put("status",sameCapital?"READY":"READY_AMOUNT");
                if(sameCapital)result.put("netReturnDifferencePct",a.path("netReturnPct").decimalValue().subtract(e.path("netReturnPct").decimalValue()));
                if(a.path("netPnlAmount").isNumber() && e.path("netPnlAmount").isNumber())result.put("forwardPnlDifferenceAmount",a.path("netPnlAmount").decimalValue().subtract(e.path("netPnlAmount").decimalValue()));
            }
        }
        return result;
    }
    private boolean terminal(JsonNode outcome){return Set.of("CLOSED","NOT_ENTERED","OBSERVED").contains(outcome.path("status").asText());}
    private DecisionModels.Job job(String id) {
        if(!settings.enabled())throw new BusinessException(ErrorCode.DECISION_DISABLED);
        var job=jobs.find(settings.ownerScope(),id).orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if(!"INTEGRATED_DECISION".equals(job.input().path("mode").asText()))throw new BusinessException(ErrorCode.INVALID_REQUEST,"통합 판단의 성과만 조회할 수 있습니다.");
        jobs.ensureIntegratedSchema();return job;
    }
    public void tick() {
        if(!settings.enabled())return;
        for(String id:outcomes.due(settings.ownerScope(),clock.millis())) {
            try {for(var plan:outcomes.plans(settings.ownerScope(),id))refresh(plan);}
            catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Decision observation failed: type={}",e.getClass().getSimpleName());}
        }
    }
    void refresh(DecisionOutcomeStore.Plan plan) {
        Instant now=clock.instant();JsonNode p=plan.input();
        var dates=new ArrayList<LocalDate>();String version=plan.outcome().path("calendarVersion").asText("");
        if(plan.outcome().path("dates").size()==p.path("horizonTradingDays").asInt())
            plan.outcome().path("dates").forEach(d->dates.add(LocalDate.parse(d.asText())));
        else {
            LocalDate date=Instant.parse(p.path("registeredAt").asText()).atZone(DecisionService.SEOUL).toLocalDate().plusDays(1);
            for(int i=0;i<800 && dates.size()<p.path("horizonTradingDays").asInt();i++,date=date.plusDays(1)) {
                var day=calendar.day(date);version=day.version();
                if(day.status()==TradingCalendar.Status.UNKNOWN)break;
                if(day.status()==TradingCalendar.Status.OPEN)dates.add(date);
            }
        }
        List<JsonNode> samples=new ArrayList<>();String issue=null;
        if(!dates.isEmpty()) {
            LocalDate priceDate=LocalDate.parse(p.path("priceDate").asText());DailyCandle anchor=null;
            for(var bar:p.path("priceHistory")) if(priceDate.toString().equals(bar.path("businessDate").asText()))anchor=json.convert(bar,DailyCandle.class);
            if(anchor==null) issue="ANCHOR_UNAVAILABLE";
            else {
                var identity=new PerformanceModels.Anchor(p.path("stock").path("stockCode").asText(),p.path("stock").path("marketCode").asText(),p.path("priceBasis").asText(),anchor);
                var due=dates.stream().filter(d->{var day=calendar.day(d);return day.status()==TradingCalendar.Status.OPEN && !d.atTime(day.closeTime()).atZone(DecisionService.SEOUL).toInstant().isAfter(now);}).toList();
                if(!due.isEmpty())for(var observation:prices.monitoringHistory(identity,due,now))samples.add(json.tree(observation));
            }
        }
        String calendarVersion=version;String dataIssue=issue;
        outcomes.observe(settings.ownerScope(),plan,samples,observations->{
            ObjectNode result=(ObjectNode)new DecisionOutcomeEngine(json).evaluate(p,dates,observations,now);
            result.put("calendarVersion",calendarVersion);
            if(dataIssue!=null){result.put("status",dataIssue);result.put("dataIssue",dataIssue);}
            return result;
        },clock.millis());
    }
}
