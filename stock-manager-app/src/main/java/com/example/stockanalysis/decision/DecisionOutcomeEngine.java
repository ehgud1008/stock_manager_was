package com.example.stockanalysis.decision;

import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.unified.performance.TradeOutcomeRules;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.*;
import java.time.*;
import java.util.*;

/** Per-unit forward simulation, not the return of the user's historical holdings or account. */
public final class DecisionOutcomeEngine {
    public static final String VERSION="decision-forward-v1";
    private final DecisionJson json;
    public DecisionOutcomeEngine(DecisionJson json){this.json=json;}
    public JsonNode evaluate(JsonNode plan,List<LocalDate> dates,List<JsonNode> observations,Instant now) {
        if(PositionOutcomeEngine.VERSION.equals(plan.path("evalPolicyVersion").asText()))return new PositionOutcomeEngine(json).evaluate(plan,dates,observations,now);
        ObjectNode out=(ObjectNode)json.tree(Map.of());out.put("policyVersion",VERSION);out.put("status","WAITING_DATA");
        out.put("evaluatedAt",now.toString());out.put("basis","PER_UNIT_FORWARD_SIMULATION");out.put("ambiguous",false);
        out.set("dates",json.tree(dates));out.put("horizonTradingDays",plan.path("horizonTradingDays").asInt());
        out.put("roundTripFeeBps",plan.path("roundTripFeeBps").asInt());out.put("slippageBps",plan.path("slippageBps").asInt());
        out.putNull("netReturnPct");out.putNull("benchmarkReturnPct");out.putNull("differencePct");
        String action=plan.path("action").asText();
        if("DEFER".equals(action)){out.put("status","DEFERRED");return out;}
        if(dates.size()!=plan.path("horizonTradingDays").asInt()){out.put("status","WAITING_CALENDAR");return out;}
        Map<LocalDate,JsonNode> byDate=new HashMap<>();for(var o:observations) {
            if(Instant.parse(o.path("collectedAt").asText()).isAfter(now))continue;
            var date=LocalDate.parse(o.path("date").asText());
            if(date.isAfter(now.atZone(DecisionService.SEOUL).toLocalDate()))continue;
            byDate.putIfAbsent(date,o);
        }
        List<DailyCandle> bars=new ArrayList<>();
        for(var date:dates) {
            var o=byDate.get(date);
            if(o==null){out.put("status",date.atTime(20,0).atZone(DecisionService.SEOUL).toInstant().isBefore(now)?"DATA_GAP":"WAITING_DATA");break;}
            if(!"VALID".equals(o.path("quality").asText())){out.put("status","DATA_GAP");out.put("dataIssue",o.path("detail").asText());break;}
            var bar=json.convert(o.path("candle"),DailyCandle.class);
            if(bar==null || !date.equals(bar.businessDate()) || bar.volume()<=0 || bar.open().signum()<=0 || bar.high().compareTo(bar.low())<=0){out.put("status","SUSPENDED");break;}
            bars.add(bar);
        }
        out.put("observedTradingDays",bars.size());
        if(bars.isEmpty())return out;
        var slip=plan.path("slippageBps").decimalValue();var fee=plan.path("roundTripFeeBps").decimalValue();
        boolean continuing="HELD".equals(plan.path("holding").path("positionStatus").asText());
        var first=bars.get(0);var entry=continuing?first.open():TradeOutcomeRules.entry(first.open(),slip);
        out.put("referenceOpen",first.open());
        if(bars.size()==dates.size())out.put("benchmarkReturnPct",net(entry,TradeOutcomeRules.exitPrice(bars.get(bars.size()-1).close(),slip),fee,continuing));
        if(Set.of("WAIT","SELL").contains(action)) {
            out.put("netReturnPct",action.equals("SELL")?net(first.open(),TradeOutcomeRules.exitPrice(first.open(),slip),fee,true):BigDecimal.ZERO);
            if(bars.size()==dates.size()) {
                out.put("status","OBSERVED");out.put("differencePct",out.path("netReturnPct").decimalValue().subtract(out.path("benchmarkReturnPct").decimalValue()));
                out.put("interpretation",action.equals("SELL")?"CASH_VS_CONTINUED_HOLDING":"CASH_VS_ENTRY_OPPORTUNITY");
            }
            return out;
        }
        JsonNode scenario=plan.path("plan");
        if(scenario.isNull() || scenario.isMissingNode()){out.put("status","PLAN_UNAVAILABLE");return out;}
        if("BUY".equals(action) && (first.open().compareTo(scenario.path("entryFrom").decimalValue())<0 || entry.compareTo(scenario.path("entryTo").decimalValue())>0)) {
            out.put("status","NOT_ENTERED");out.put("netReturnPct",BigDecimal.ZERO);out.put("reason","NEXT_OPEN_OUTSIDE_ENTRY_RANGE");return out;
        }
        var stop=scenario.path("stopLoss").decimalValue();
        // Personal stop/target are preserved separately; apply them to continuation only, never a new-entry benchmark.
        List<BigDecimal> targets=new ArrayList<>();for(var t:scenario.path("targets"))targets.add(t.decimalValue());
        if("HOLD".equals(action)) {
            var h=plan.path("holding").path("holding");
            if(h.path("stopPrice").isNumber())stop=h.path("stopPrice").decimalValue();
            if(h.path("targetPrice").isNumber())targets=new ArrayList<>(List.of(h.path("targetPrice").decimalValue()));
        }
        if(stop.signum()<=0 || targets.isEmpty()){out.put("status","PLAN_UNAVAILABLE");return out;}
        out.put("entryDate",first.businessDate().toString());out.put("entryPrice",entry);
        BigDecimal proceeds=BigDecimal.ZERO;int sold=0;int count=targets.size();String exitReason=null;
        var fills=out.putArray("fills");
        for(int index=0;index<bars.size();index++) {
            var bar=bars.get(index);BigDecimal exit=null;boolean closeAll=false;
            if(bar.open().compareTo(stop)<=0){exit=bar.open();exitReason="GAP_STOP";closeAll=true;}
            else if(bar.low().compareTo(stop)<=0) {
                exit=stop;exitReason="STOP";closeAll=true;
                if(sold<count && bar.high().compareTo(targets.get(sold))>=0)out.put("ambiguous",true);
            }
            if(closeAll) {
                var fill=TradeOutcomeRules.exitPrice(exit,slip);proceeds=proceeds.add(fill.multiply(BigDecimal.valueOf(count-sold)));
                fills.add(json.tree(Map.of("date",bar.businessDate(),"price",fill,"parts",count-sold,"reason",exitReason)));sold=count;
            } else {
                while(sold<count && bar.high().compareTo(targets.get(sold))>=0) {
                    var fill=TradeOutcomeRules.exitPrice(targets.get(sold),slip);proceeds=proceeds.add(fill);sold++;exitReason="TARGET";
                    fills.add(json.tree(Map.of("date",bar.businessDate(),"price",fill,"parts",1,"reason",exitReason)));
                }
                if(index==dates.size()-1 && sold<count) {
                    var fill=TradeOutcomeRules.exitPrice(bar.close(),slip);proceeds=proceeds.add(fill.multiply(BigDecimal.valueOf(count-sold)));
                    fills.add(json.tree(Map.of("date",bar.businessDate(),"price",fill,"parts",count-sold,"reason","TIME_EXIT")));sold=count;exitReason="TIME_EXIT";
                }
            }
            if(sold==count) {
                out.put("status","CLOSED");out.put("exitDate",bar.businessDate().toString());out.put("exitReason",exitReason);
                var average=proceeds.divide(BigDecimal.valueOf(count),12,RoundingMode.HALF_UP);
                out.put("exitPrice",average);out.put("netReturnPct",net(entry,average,fee,continuing));break;
            }
        }
        if(sold<count && bars.size()==dates.size())out.put("status","OPEN");
        else if(sold<count && Set.of("WAITING_DATA").contains(out.path("status").asText()))out.put("status","OPEN");
        out.put("partialTargetsFilled",sold);
        if(out.path("netReturnPct").isNumber() && out.path("benchmarkReturnPct").isNumber())out.put("differencePct",out.path("netReturnPct").decimalValue().subtract(out.path("benchmarkReturnPct").decimalValue()));
        return out;
    }
    private BigDecimal net(BigDecimal entry,BigDecimal exit,BigDecimal fee,boolean continuing) {
        return continuing?TradeOutcomeRules.change(exit.multiply(BigDecimal.ONE.subtract(fee.movePointLeft(4).divide(BigDecimal.valueOf(2)))),entry):TradeOutcomeRules.net(entry,exit,fee);
    }
}
