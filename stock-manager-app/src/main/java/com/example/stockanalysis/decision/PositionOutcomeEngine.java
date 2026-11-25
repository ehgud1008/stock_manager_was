package com.example.stockanalysis.decision;

import com.example.stockanalysis.engine.model.DailyCandle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.*;
import java.time.*;
import java.util.*;

/** Integer-share forward cash flows. Historical purchase profits and actual executions are excluded. */
public final class PositionOutcomeEngine {
    public static final String VERSION="decision-position-forward-v2";
    private final DecisionJson json;
    public PositionOutcomeEngine(DecisionJson json){this.json=json;}
    public JsonNode evaluate(JsonNode plan,List<LocalDate> dates,List<JsonNode> observations,Instant now) {
        ObjectNode legacy=plan.deepCopy();legacy.put("evalPolicyVersion",DecisionOutcomeEngine.VERSION);
        String action=plan.path("action").asText();
        // The original engine supplies the same immutable-data/calendar checks and per-unit fallback.
        legacy.put("action",Set.of("ADD","REDUCE").contains(action)?"HOLD":action);
        ObjectNode out=(ObjectNode)new DecisionOutcomeEngine(json).evaluate(legacy,dates,observations,now);
        out.put("policyVersion",VERSION);
        boolean held="HELD".equals(plan.path("holding").path("positionStatus").asText());
        BigDecimal initial=held?plan.path("holding").path("holding").path("quantity").decimalValue():BigDecimal.ZERO;
        BigDecimal trade=plan.path("quantity").isNumber()?plan.path("quantity").decimalValue():null;
        if("DEFER".equals(action) || "WAIT".equals(action))return out;
        boolean executable=Set.of("BUY","ADD","REDUCE","SELL").contains(action);
        if((held && !wholePositive(initial)) || (executable && (trade==null || !wholePositive(trade)))) {
            out.put("basis","PER_UNIT_FORWARD_SIMULATION");out.put("quantityEvaluationStatus","QUANTITY_UNAVAILABLE");
            if(Set.of("ADD","REDUCE").contains(action)){out.put("status","DEFERRED");clearReturns(out);}
            return out;
        }
        if((Set.of("ADD","REDUCE","SELL").contains(action) && !held)
                || ("SELL".equals(action) && trade.compareTo(initial)!=0)
                || (Set.of("REDUCE","SELL").contains(action) && trade.compareTo(initial)>0)
                || ("REDUCE".equals(action) && trade.compareTo(initial)>=0)) {
            out.put("status","DEFERRED");out.put("quantityEvaluationStatus","INVALID_QUANTITY");clearReturns(out);return out;
        }
        out.put("basis","INTEGER_SHARE_FORWARD_SIMULATION");out.put("quantityEvaluationStatus","AVAILABLE");
        out.remove(List.of("entryDate","entryPrice","exitDate","exitPrice","exitReason","partialTargetsFilled","interpretation"));
        out.put("initialHeldQuantity",initial);out.put("plannedTradeQuantity",trade==null?BigDecimal.ZERO:trade);
        out.putNull("netPnlAmount");out.putNull("benchmarkPnlAmount");out.putNull("differenceAmount");clearReturns(out);
        if(dates.size()!=plan.path("horizonTradingDays").asInt())return out;
        Map<LocalDate,JsonNode> byDate=new HashMap<>();
        for(var o:observations)if(!Instant.parse(o.path("collectedAt").asText()).isAfter(now)
                && !LocalDate.parse(o.path("date").asText()).isAfter(now.atZone(DecisionService.SEOUL).toLocalDate()))byDate.putIfAbsent(LocalDate.parse(o.path("date").asText()),o);
        List<DailyCandle> bars=new ArrayList<>();
        for(var date:dates) {
            var o=byDate.get(date);if(o==null || !"VALID".equals(o.path("quality").asText()))break;
            var b=json.convert(o.path("candle"),DailyCandle.class);
            if(b==null || !date.equals(b.businessDate()) || b.volume()<=0 || b.open().signum()<=0 || b.high().compareTo(b.low())<=0)break;
            bars.add(b);
        }
        if(bars.isEmpty())return out;
        var first=bars.get(0);BigDecimal slip=plan.path("slippageBps").decimalValue().movePointLeft(4);
        BigDecimal halfFee=plan.path("roundTripFeeBps").decimalValue().movePointLeft(4).divide(BigDecimal.valueOf(2));
        BigDecimal unitBuy=first.open().multiply(BigDecimal.ONE.add(slip)).multiply(BigDecimal.ONE.add(halfFee)).setScale(2,RoundingMode.UP);
        BigDecimal cashOut=BigDecimal.ZERO,cashIn=BigDecimal.ZERO,remaining=initial;
        BigDecimal startingCapital=first.open().multiply(initial);String entryStatus="NOT_APPLICABLE";
        var fills=out.putArray("fills");
        if(Set.of("BUY","ADD").contains(action)) {
            var scenario=plan.path("plan");
            BigDecimal rawEntry=first.open().multiply(BigDecimal.ONE.add(slip));
            boolean within=first.open().compareTo(scenario.path("entryFrom").decimalValue())>=0 && rawEntry.compareTo(scenario.path("entryTo").decimalValue())<=0;
            boolean budget=plan.path("riskSizing").path("unitPurchaseCost").isNumber() && unitBuy.compareTo(plan.path("riskSizing").path("unitPurchaseCost").decimalValue())<=0;
            if("ADD".equals(action)) {
                var sizing=plan.path("riskSizing");var settings=plan.path("accountRisk").path("settings");
                if(!sizing.path("holdingNetStopReference").isNumber() || !sizing.path("netStopReference").isNumber()
                        || !sizing.path("positionBudget").isNumber() || !settings.path("maxLossPerTrade").isNumber() || !settings.path("availableCash").isNumber())budget=false;
                else {
                    BigDecimal newCost=unitBuy.multiply(trade);
                    BigDecimal openRisk=first.open().subtract(sizing.path("holdingNetStopReference").decimalValue()).max(BigDecimal.ZERO).multiply(initial)
                            .add(unitBuy.subtract(sizing.path("netStopReference").decimalValue()).max(BigDecimal.ZERO).multiply(trade));
                    budget=budget && newCost.compareTo(settings.path("availableCash").decimalValue())<=0
                            && first.open().multiply(initial).add(newCost).compareTo(sizing.path("positionBudget").decimalValue())<=0
                            && openRisk.compareTo(settings.path("maxLossPerTrade").decimalValue())<=0;
                }
            }
            if(within && budget){cashOut=unitBuy.multiply(trade);remaining=remaining.add(trade);entryStatus="FILLED";fill(fills,first,trade,unitBuy,"ENTRY","BUY");}
            else entryStatus=within?"FROZEN_COST_LIMIT_EXCEEDED":"NEXT_OPEN_OUTSIDE_ENTRY_RANGE";
            // Unused planned cash remains cash; both comparison arms retain this fixed denominator.
            startingCapital=startingCapital.add(plan.path("riskSizing").path("estimatedPurchaseAmount").decimalValue());
            if("BUY".equals(action) && !"FILLED".equals(entryStatus)) {
                out.put("status","NOT_ENTERED");out.put("reason",entryStatus);out.put("executedTradeQuantity",0);
                out.put("entryStatus",entryStatus);out.put("remainingQuantity",0);out.put("startingCapital",startingCapital);
                out.put("netPnlAmount",BigDecimal.ZERO);out.put("netReturnPct",BigDecimal.ZERO);
                if(bars.size()==dates.size())amounts(out,BigDecimal.ZERO,exit(bars.get(bars.size()-1).close(),slip,halfFee).subtract(unitBuy).multiply(trade),startingCapital);
                return out;
            }
        } else if(Set.of("REDUCE","SELL").contains(action)) {
            var price=exit(first.open(),slip,halfFee);cashIn=price.multiply(trade);remaining=remaining.subtract(trade);
            fill(fills,first,trade,price,action,"SELL");entryStatus="FILLED";
        }
        out.put("entryStatus",entryStatus);out.put("executedTradeQuantity","FILLED".equals(entryStatus)?trade:BigDecimal.ZERO);
        out.put("startingCapital",startingCapital);out.put("purchaseAmount",cashOut);
        BigDecimal stop=plan.path("plan").path("stopLoss").decimalValue();
        List<BigDecimal> targets=new ArrayList<>();for(var t:plan.path("plan").path("targets"))targets.add(t.decimalValue());
        if(held) {
            var h=plan.path("holding").path("holding");
            if(h.path("stopPrice").isNumber())stop=stop.max(h.path("stopPrice").decimalValue());
            if(h.path("targetPrice").isNumber())targets=new ArrayList<>(List.of(h.path("targetPrice").decimalValue()));
        }
        if(remaining.signum()>0 && (stop.signum()<=0 || targets.isEmpty())){out.put("status","PLAN_UNAVAILABLE");return out;}
        BigDecimal[] targetQuantities=new BigDecimal[targets.size()];
        if(!targets.isEmpty()) {
            BigDecimal[] parts=remaining.divideAndRemainder(BigDecimal.valueOf(targets.size()));
            for(int i=0;i<targets.size();i++)targetQuantities[i]=parts[0].add(BigDecimal.valueOf(i<parts[1].intValue()?1:0));
        }
        int targetIndex=0;out.put("ambiguous",false);
        for(int index=0;index<bars.size() && remaining.signum()>0;index++) {
            var b=bars.get(index);
            if(b.open().compareTo(stop)<=0 || b.low().compareTo(stop)<=0) {
                boolean gap=b.open().compareTo(stop)<=0;var price=exit(gap?b.open():stop,slip,halfFee);
                if(!gap && targetIndex<targets.size() && b.high().compareTo(targets.get(targetIndex))>=0)out.put("ambiguous",true);
                cashIn=cashIn.add(price.multiply(remaining));fill(fills,b,remaining,price,gap?"GAP_STOP":"STOP","SELL");remaining=BigDecimal.ZERO;
            } else {
                while(targetIndex<targets.size() && b.high().compareTo(targets.get(targetIndex))>=0) {
                    var q=targetQuantities[targetIndex];var price=exit(targets.get(targetIndex),slip,halfFee);
                    if(q.signum()>0){cashIn=cashIn.add(price.multiply(q));remaining=remaining.subtract(q);fill(fills,b,q,price,"TARGET","SELL");}targetIndex++;
                }
                if(index==dates.size()-1 && remaining.signum()>0) {
                    var price=exit(b.close(),slip,halfFee);cashIn=cashIn.add(price.multiply(remaining));fill(fills,b,remaining,price,"TIME_EXIT","SELL");remaining=BigDecimal.ZERO;
                }
            }
        }
        out.put("remainingQuantity",remaining);out.put("saleProceeds",cashIn);
        if(remaining.signum()==0) {
            out.put("status","CLOSED");out.put("netPnlAmount",cashIn.subtract(cashOut).subtract(first.open().multiply(initial)));
            if(!fills.isEmpty()){var last=fills.get(fills.size()-1);out.set("exitDate",last.path("date"));out.set("exitReason",last.path("reason"));}
            out.put("netReturnPct",pct(out.path("netPnlAmount").decimalValue(),startingCapital));
        } else if(bars.size()==dates.size())out.put("status","OPEN");
        else if(!Set.of("DATA_GAP","SUSPENDED").contains(out.path("status").asText()))out.put("status","OPEN");
        if(bars.size()==dates.size()) {
            BigDecimal last=exit(bars.get(bars.size()-1).close(),slip,halfFee);
            BigDecimal benchmark=held?last.subtract(first.open()).multiply(initial):last.subtract(unitBuy).multiply(trade);
            out.put("benchmarkPnlAmount",benchmark);out.put("benchmarkReturnPct",pct(benchmark,startingCapital));
            if(out.path("netPnlAmount").isNumber())amounts(out,out.path("netPnlAmount").decimalValue(),benchmark,startingCapital);
        }
        return out;
    }
    private boolean wholePositive(BigDecimal q){return q.signum()>0 && q.stripTrailingZeros().scale()<=0;}
    private void clearReturns(ObjectNode out){for(String f:List.of("netReturnPct","benchmarkReturnPct","differencePct"))out.putNull(f);}
    private BigDecimal exit(BigDecimal price,BigDecimal slip,BigDecimal fee){return price.multiply(BigDecimal.ONE.subtract(slip)).multiply(BigDecimal.ONE.subtract(fee)).setScale(2,RoundingMode.DOWN);}
    private BigDecimal pct(BigDecimal value,BigDecimal capital){return capital.signum()>0?value.multiply(BigDecimal.valueOf(100)).divide(capital,6,RoundingMode.HALF_UP):BigDecimal.ZERO;}
    private void amounts(ObjectNode out,BigDecimal pnl,BigDecimal benchmark,BigDecimal capital){out.put("netPnlAmount",pnl);out.put("benchmarkPnlAmount",benchmark);out.put("differenceAmount",pnl.subtract(benchmark));out.put("netReturnPct",pct(pnl,capital));out.put("benchmarkReturnPct",pct(benchmark,capital));out.put("differencePct",pct(pnl.subtract(benchmark),capital));}
    private void fill(com.fasterxml.jackson.databind.node.ArrayNode fills,DailyCandle b,BigDecimal q,BigDecimal price,String reason,String side){fills.add(json.tree(Map.of("date",b.businessDate(),"quantity",q,"price",price,"amount",q.multiply(price),"reason",reason,"side",side)));}
}
