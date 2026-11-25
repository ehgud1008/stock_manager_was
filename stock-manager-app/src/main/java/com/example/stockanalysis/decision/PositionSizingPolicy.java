package com.example.stockanalysis.decision;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import java.math.*;
import java.time.*;
import java.util.*;

/** Conservative integer-share ceiling for ONE hypothetical entry. No order or cash reservation. */
public final class PositionSizingPolicy {
    public static final String VERSION="position-sizing-v2-held";
    public static final String LEGACY_VERSION="position-sizing-v1";
    public static final BigDecimal FEE_BPS=new BigDecimal("30"),SLIP_BPS=new BigDecimal("10");
    private static final BigDecimal MAX_QUANTITY=new BigDecimal("9999999999");
    public ObjectNode calculate(JsonNode policy,Instant now) {
        ObjectNode out=JsonNodeFactory.instance.objectNode();out.put("policyVersion",LEGACY_VERSION);out.put("scope","SINGLE_ENTRY_CEILING_NO_RESERVATION");
        out.put("status","UNAVAILABLE");out.putNull("maxBuyQuantity");out.putNull("estimatedPurchaseAmount");out.putNull("estimatedLossAtStop");
        out.put("roundTripFeeBps",FEE_BPS);out.put("slippageBps",SLIP_BPS);var reasons=out.putArray("reasons");
        try {
            var risk=policy.path("accountRisk");
            if(!"AVAILABLE".equals(risk.path("status").asText()))return unavailable(out,"UNAVAILABLE".equals(risk.path("status").asText())?"ACCOUNT_RISK_UNAVAILABLE":"ACCOUNT_RISK_MISSING");
            var settings=risk.path("settings");out.set("accountRevision",settings.path("revision"));out.set("balanceDate",settings.path("balanceDate"));
            LocalDate today=LocalDate.ofInstant(now,DecisionService.SEOUL);
            if(!today.equals(LocalDate.parse(settings.path("balanceDate").asText())))return unavailable(out,"ACCOUNT_RISK_STALE");
            var holding=policy.path("holding");
            if("UNKNOWN".equals(holding.path("positionStatus").asText()))return unavailable(out,"POSITION_UNKNOWN");
            if("HELD".equals(holding.path("positionStatus").asText()))return unavailable(out,"NEW_ENTRY_ONLY");
            var h=holding.path("holding");
            // Absence from the manual list is not evidence of a current zero balance.
            if(!h.isObject() || !h.path("quantity").isNumber() || h.path("quantity").decimalValue().signum()!=0)return unavailable(out,"POSITION_UNCONFIRMED");
            if(!today.equals(LocalDate.parse(h.path("balanceDate").asText())))return unavailable(out,"POSITION_STALE");
            out.set("holdingRevision",h.path("revision"));
            var scenario=policy.path("plan");if(!new DecisionValidityPolicy().validPlan(scenario))return unavailable(out,"PLAN_INVALID");
            BigDecimal assets=number(settings,"totalAssets"),cash=number(settings,"availableCash"),loss=number(settings,"maxLossPerTrade"),percent=number(settings,"maxPositionPercent");
            if(assets.signum()<0 || cash.signum()<0 || loss.signum()<0 || percent.signum()<0 || percent.compareTo(new BigDecimal("100"))>0 || cash.compareTo(assets)>0 || loss.compareTo(assets)>0)return unavailable(out,"ACCOUNT_RISK_INVALID");
            BigDecimal halfFee=FEE_BPS.movePointLeft(4).divide(BigDecimal.valueOf(2));
            BigDecimal entry=scenario.path("entryTo").decimalValue().multiply(BigDecimal.ONE.add(SLIP_BPS.movePointLeft(4))).setScale(2,RoundingMode.UP);
            BigDecimal unitCost=entry.multiply(BigDecimal.ONE.add(halfFee)).setScale(2,RoundingMode.UP);
            BigDecimal stopNet=scenario.path("stopLoss").decimalValue().multiply(BigDecimal.ONE.subtract(SLIP_BPS.movePointLeft(4)))
                    .multiply(BigDecimal.ONE.subtract(halfFee)).setScale(2,RoundingMode.DOWN);
            BigDecimal unitRisk=unitCost.subtract(stopNet).setScale(2,RoundingMode.UP);
            if(unitRisk.signum()<=0)return unavailable(out,"PLAN_INVALID");
            BigDecimal weightBudget=assets.multiply(percent).movePointLeft(2).setScale(2,RoundingMode.DOWN);
            BigDecimal cashQty=cash.divide(unitCost,0,RoundingMode.DOWN),riskQty=loss.divide(unitRisk,0,RoundingMode.DOWN),weightQty=weightBudget.divide(unitCost,0,RoundingMode.DOWN);
            BigDecimal quantity=cashQty.min(riskQty).min(weightQty).min(MAX_QUANTITY);
            out.put("status",quantity.signum()>0?"READY":"ZERO_CAPACITY");out.put("maxBuyQuantity",quantity);
            out.put("entryReference",entry);out.put("unitPurchaseCost",unitCost);out.put("netStopReference",stopNet);out.put("unitRisk",unitRisk);
            out.put("cashLimitQuantity",cashQty);out.put("riskLimitQuantity",riskQty);out.put("weightLimitQuantity",weightQty);out.put("positionBudget",weightBudget);
            out.put("estimatedPurchaseAmount",unitCost.multiply(quantity));out.put("estimatedLossAtStop",unitRisk.multiply(quantity));
            if(quantity.compareTo(cashQty)==0)reasons.add("CASH_LIMIT");if(quantity.compareTo(riskQty)==0)reasons.add("LOSS_LIMIT");
            if(quantity.compareTo(weightQty)==0)reasons.add("POSITION_WEIGHT_LIMIT");if(quantity.compareTo(MAX_QUANTITY)==0)reasons.add("QUANTITY_LIMIT");
            return out;
        }catch(RuntimeException e){return unavailable(out,"ACCOUNT_RISK_INVALID");}
    }
    /** Existing exposure consumes the same position and loss budgets as a new purchase. */
    public ObjectNode calculate(JsonNode policy,String action,Instant now) {
        if(!"HELD".equals(policy.path("holding").path("positionStatus").asText())) {
            var out=calculate(policy,now);out.put("policyVersion",VERSION);out.put("action","BUY");return out;
        }
        var out=JsonNodeFactory.instance.objectNode();out.put("policyVersion",VERSION);out.put("action",action);
        out.put("scope","SINGLE_POSITION_NO_RESERVATION");out.putNull("maxBuyQuantity");out.putNull("tradeQuantity");
        out.put("roundTripFeeBps",FEE_BPS);out.put("slippageBps",SLIP_BPS);
        try {
            var h=policy.path("holding").path("holding");
            BigDecimal held=number(h,"quantity");
            if(held.signum()<=0 || held.stripTrailingZeros().scale()>0 || held.compareTo(MAX_QUANTITY)>0)return unavailable(out,"POSITION_UNCONFIRMED");
            out.put("heldQuantity",held);out.set("holdingRevision",h.path("revision"));
            if(!LocalDate.ofInstant(now,DecisionService.SEOUL).equals(LocalDate.parse(h.path("balanceDate").asText())))return unavailable(out,"POSITION_STALE");
            if("SELL".equals(action)){out.put("status","READY");out.put("tradeQuantity",held);out.put("remainingQuantity",0);return out;}
            // Reuse the validated new-entry budgets and price plan with an explicit zero balance.
            ObjectNode entryPolicy=policy.deepCopy();((ObjectNode)entryPolicy.path("holding")).put("positionStatus","NOT_HELD");
            ((ObjectNode)entryPolicy.path("holding").path("holding")).put("quantity",0);
            var base=calculate(entryPolicy,now);
            if(!Set.of("READY","ZERO_CAPACITY").contains(base.path("status").asText())) {
                out.setAll(base);out.put("policyVersion",VERSION);out.put("action",action);return out;
            }
            out.setAll(base);out.put("policyVersion",VERSION);out.put("action",action);out.put("scope","SINGLE_POSITION_NO_RESERVATION");out.put("heldQuantity",held);
            BigDecimal price=number(policy.path("stock"),"currentPrice");
            if(price.signum()<=0)return unavailable(out,"PLAN_INVALID");
            BigDecimal stop=number(policy.path("plan"),"stopLoss");
            // A tighter registered stop wins; a breached stop must never justify averaging down.
            if(h.path("stopPrice").isNumber())stop=stop.max(h.path("stopPrice").decimalValue());
            if(stop.signum()<=0 || price.compareTo(stop)<=0)return unavailable(out,"STOP_REFERENCE_BREACHED");
            if("ADD".equals(action) && stop.compareTo(number(policy.path("plan"),"entryFrom"))>=0)return unavailable(out,"PLAN_INVALID");
            BigDecimal halfFee=FEE_BPS.movePointLeft(4).divide(BigDecimal.valueOf(2));
            BigDecimal netStop=stop.multiply(BigDecimal.ONE.subtract(SLIP_BPS.movePointLeft(4))).multiply(BigDecimal.ONE.subtract(halfFee)).setScale(2,RoundingMode.DOWN);
            BigDecimal unitHeldRisk=price.subtract(netStop).max(BigDecimal.ZERO);
            var settings=policy.path("accountRisk").path("settings");
            BigDecimal exposure=price.multiply(held),existingRisk=unitHeldRisk.multiply(held);
            BigDecimal remainingWeight=base.path("positionBudget").decimalValue().subtract(exposure).max(BigDecimal.ZERO);
            BigDecimal remainingRisk=number(settings,"maxLossPerTrade").subtract(existingRisk).max(BigDecimal.ZERO);
            out.put("existingExposure",exposure);out.put("existingStopRisk",existingRisk);out.put("remainingPositionBudget",remainingWeight);out.put("remainingLossBudget",remainingRisk);
            out.put("holdingStopReference",stop);out.put("holdingNetStopReference",netStop);
            if("REDUCE".equals(action)) {
                BigDecimal weightKeep=base.path("positionBudget").decimalValue().divide(price,0,RoundingMode.DOWN);
                BigDecimal riskKeep=unitHeldRisk.signum()==0?held:number(settings,"maxLossPerTrade").divide(unitHeldRisk,0,RoundingMode.DOWN);
                BigDecimal keep=held.min(weightKeep).min(riskKeep),sell=held.subtract(keep);
                out.putNull("maxBuyQuantity");out.put("tradeQuantity",sell);out.put("remainingQuantity",keep);
                out.put("status",sell.signum()==0?"NO_REDUCTION_NEEDED":keep.signum()==0?"FULL_EXIT_REVIEW_REQUIRED":"READY");
                out.put("reductionRule","RISK_AND_WEIGHT_EXCESS_ONLY");return out;
            }
            BigDecimal cost=base.path("unitPurchaseCost").decimalValue(),risk=base.path("unitRisk").decimalValue();
            BigDecimal cashQty=base.path("cashLimitQuantity").decimalValue(),riskQty=remainingRisk.divide(risk,0,RoundingMode.DOWN),weightQty=remainingWeight.divide(cost,0,RoundingMode.DOWN);
            BigDecimal qty=cashQty.min(riskQty).min(weightQty).min(MAX_QUANTITY.subtract(held));
            out.put("cashLimitQuantity",cashQty);out.put("riskLimitQuantity",riskQty);out.put("weightLimitQuantity",weightQty);
            out.put("maxBuyQuantity",qty);out.put("tradeQuantity",qty);out.put("remainingQuantity",held.add(qty));out.put("status",qty.signum()>0?"READY":"ZERO_CAPACITY");
            out.put("estimatedPurchaseAmount",cost.multiply(qty));out.put("estimatedLossAtStop",risk.multiply(qty));
            var reasons=out.putArray("reasons");if(qty.compareTo(cashQty)==0)reasons.add("CASH_LIMIT");if(qty.compareTo(riskQty)==0)reasons.add("LOSS_LIMIT");if(qty.compareTo(weightQty)==0)reasons.add("POSITION_WEIGHT_LIMIT");
            if(qty.compareTo(MAX_QUANTITY.subtract(held))==0)reasons.add("QUANTITY_LIMIT");return out;
        }catch(RuntimeException e){return unavailable(out,"POSITION_UNCONFIRMED");}
    }
    private BigDecimal number(JsonNode node,String field){if(!node.path(field).isNumber())throw new IllegalArgumentException();return node.path(field).decimalValue();}
    private ObjectNode unavailable(ObjectNode out,String reason){out.put("status",reason);out.withArray("reasons").add(reason);return out;}
}
