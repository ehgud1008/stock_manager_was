package com.example.stockanalysis.decision;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class PositionOutcomeEngineTest {
    DecisionJson json=new DecisionJson(new ObjectMapper().registerModule(new JavaTimeModule()));
    Instant now=Instant.parse("2026-11-20T12:00:00Z");LocalDate date=LocalDate.parse("2026-11-18");
    ObjectNode plan(String action,int held,int trade){return (ObjectNode)json.tree(Map.ofEntries(
        Map.entry("action",action),Map.entry("evalPolicyVersion",PositionOutcomeEngine.VERSION),Map.entry("horizonTradingDays",1),Map.entry("roundTripFeeBps",30),Map.entry("slippageBps",10),
        Map.entry("quantity",trade),Map.entry("holding",Map.of("positionStatus",held>0?"HELD":"NOT_HELD","holding",Map.of("quantity",held))),
        Map.entry("plan",Map.of("entryFrom",95,"entryTo",101,"stopLoss",90,"targets",List.of(110,120))),
        Map.entry("accountRisk",Map.of("settings",Map.of("availableCash",10000,"maxLossPerTrade",1000))),
        Map.entry("riskSizing",Map.of("unitPurchaseCost",102,"estimatedPurchaseAmount",trade*102,"holdingNetStopReference",89.77,"netStopReference",89.77,"positionBudget",5000))));}
    JsonNode observation(int open,int high,int low,int close){return json.tree(Map.of("date",date,"quality","VALID","collectedAt",now.minusSeconds(60).toString(),"candle",Map.of("businessDate",date,"open",open,"high",high,"low",low,"close",close,"volume",100)));}
    JsonNode evaluate(ObjectNode p,JsonNode o){return new DecisionOutcomeEngine(json).evaluate(p,List.of(date),List.of(o),now);}
    @Test void additionAccountsForOnlyNewPurchaseCostsAndAllIntegerShares(){
        var r=evaluate(plan("ADD",10,3),observation(100,125,99,120));
        assertThat(r.path("status").asText()).isEqualTo("CLOSED");assertThat(r.path("basis").asText()).isEqualTo("INTEGER_SHARE_FORWARD_SIMULATION");
        assertThat(r.path("purchaseAmount").decimalValue()).isEqualByComparingTo("300.78");
        assertThat(r.path("fills").get(1).path("quantity").asInt()).isEqualTo(7);assertThat(r.path("fills").get(2).path("quantity").asInt()).isEqualTo(6);
        assertThat(r.path("netPnlAmount").decimalValue()).isEqualByComparingTo("185.46");
        assertThat(r.path("remainingQuantity").asInt()).isZero();
    }
    @Test void reductionSellsOnlyRequestedSharesAtOpenAndTracksTheRemainder(){
        var r=evaluate(plan("REDUCE",10,3),observation(100,105,99,104));
        assertThat(r.path("fills").get(0).path("quantity").asInt()).isEqualTo(3);assertThat(r.path("fills").get(1).path("quantity").asInt()).isEqualTo(7);
        assertThat(r.path("netPnlAmount").decimalValue()).isEqualByComparingTo("25.43");assertThat(r.path("purchaseAmount").decimalValue()).isZero();
    }
    @Test void gapStopsApplyToEveryRemainingShareAndAmbiguousDayPrefersStop(){
        var r=evaluate(plan("ADD",10,3),observation(100,125,80,105));
        assertThat(r.path("ambiguous").asBoolean()).isTrue();assertThat(r.path("fills").get(1).path("quantity").asInt()).isEqualTo(13);
        assertThat(r.path("fills").get(1).path("reason").asText()).isEqualTo("STOP");
        r=evaluate(plan("HOLD",10,0),observation(80,100,75,90));
        assertThat(r.path("fills").get(0).path("reason").asText()).isEqualTo("GAP_STOP");assertThat(r.path("netPnlAmount").decimalValue()).isEqualByComparingTo("-2.00");
    }
    @Test void outsideEntryRangeNeverPurchasesButExistingPositionContinues(){
        var r=evaluate(plan("ADD",10,3),observation(105,108,100,106));
        assertThat(r.path("entryStatus").asText()).isEqualTo("NEXT_OPEN_OUTSIDE_ENTRY_RANGE");assertThat(r.path("executedTradeQuantity").asInt()).isZero();
        assertThat(r.path("fills").get(0).path("quantity").asInt()).isEqualTo(10);
        r=evaluate(plan("BUY",0,3),observation(105,108,100,106));assertThat(r.path("status").asText()).isEqualTo("NOT_ENTERED");assertThat(r.path("netPnlAmount").decimalValue()).isZero();assertThat(r.path("benchmarkPnlAmount").isNumber()).isTrue();
    }
    @Test void incompleteDataCannotProduceCompletedMonetaryResultsAndLegacyIsUnchanged(){
        var p=plan("REDUCE",10,3);var r=new DecisionOutcomeEngine(json).evaluate(p,List.of(date),List.of(),now);
        assertThat(r.path("netPnlAmount").isNull()).isTrue();assertThat(r.path("status").asText()).isEqualTo("DATA_GAP");
        p.put("evalPolicyVersion",DecisionOutcomeEngine.VERSION);p.put("action","HOLD");r=evaluate(p,observation(100,105,99,104));
        assertThat(r.path("basis").asText()).isEqualTo("PER_UNIT_FORWARD_SIMULATION");assertThat(r.has("netPnlAmount")).isFalse();
    }
    @Test void invalidPartialSaleCannotBecomeAFullLiquidation(){
        var r=evaluate(plan("REDUCE",10,10),observation(100,105,99,104));assertThat(r.path("status").asText()).isEqualTo("DEFERRED");assertThat(r.path("netReturnPct").isNull()).isTrue();
    }
    @Test void openingPriceCannotPushAdditionOverTheFrozenCombinedRiskBudget(){
        var p=plan("ADD",10,3);((ObjectNode)p.path("accountRisk").path("settings")).put("maxLossPerTrade",130);
        var r=evaluate(p,observation(100,105,99,104));
        assertThat(r.path("entryStatus").asText()).isEqualTo("FROZEN_COST_LIMIT_EXCEEDED");
        assertThat(r.path("executedTradeQuantity").asInt()).isZero();assertThat(r.path("fills").get(0).path("quantity").asInt()).isEqualTo(10);
    }
}
