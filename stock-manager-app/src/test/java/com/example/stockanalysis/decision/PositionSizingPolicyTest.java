package com.example.stockanalysis.decision;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class PositionSizingPolicyTest {
    DecisionJson json=new DecisionJson(new ObjectMapper().registerModule(new JavaTimeModule()));
    Instant now=Instant.parse("2026-11-17T01:00:00Z");PositionSizingPolicy sizing=new PositionSizingPolicy();
    ObjectNode policy() {
        return (ObjectNode)json.tree(Map.ofEntries(
            Map.entry("priceDate","2026-11-16"),Map.entry("analysisTiming","VALID"),Map.entry("engineAction","BUY"),Map.entry("entryEligible",true),Map.entry("collectionQuality","COMPLETE"),
            Map.entry("holding",Map.of("positionStatus","NOT_HELD","holding",Map.of("quantity",0,"balanceDate","2026-11-17","revision",2))),
            Map.entry("research",Map.of("status","VALID","collectedAt",now.toString())),
            Map.entry("accountRisk",Map.of("status","AVAILABLE","settings",Map.of("totalAssets",100000,"availableCash",10000,"maxLossPerTrade",500,"maxPositionPercent",5,"balanceDate","2026-11-17","revision",1))),
            Map.entry("plan",Map.of("entryFrom",95,"entryTo",100,"stopLoss",90,"targets",List.of(110)))));
    }
    void setting(ObjectNode p,String field,Number value){((ObjectNode)p.path("accountRisk").path("settings")).set(field,json.tree(value));}
    @Test void integerCeilingIncludesConservativeCostsAndSatisfiesAllThreeLimits(){
        var r=sizing.calculate(policy(),now);assertThat(r.path("status").asText()).isEqualTo("READY");
        assertThat(r.path("maxBuyQuantity").asInt()).isEqualTo(47);
        assertThat(r.path("unitPurchaseCost").decimalValue()).isEqualByComparingTo("100.26");
        assertThat(r.path("netStopReference").decimalValue()).isEqualByComparingTo("89.77");
        assertThat(r.path("estimatedLossAtStop").decimalValue()).isLessThanOrEqualTo(new BigDecimal("500"));
        assertThat(r.path("estimatedPurchaseAmount").decimalValue()).isLessThanOrEqualTo(new BigDecimal("5000"));
        assertThat(r.path("unitRisk").decimalValue().multiply(new BigDecimal("48"))).isGreaterThan(new BigDecimal("500"));
    }
    @Test void cashAndWeightCanIndependentlyBeTheBindingLimitAndZeroStopsNewEntry(){
        var p=policy();setting(p,"availableCash",1000);assertThat(sizing.calculate(p,now).path("maxBuyQuantity").asInt()).isEqualTo(9);
        p=policy();setting(p,"maxPositionPercent",new BigDecimal("0.5"));assertThat(sizing.calculate(p,now).path("maxBuyQuantity").asInt()).isEqualTo(4);
        for(String field:List.of("availableCash","maxLossPerTrade","maxPositionPercent")) {
            p=policy();setting(p,field,0);var r=sizing.calculate(p,now);
            assertThat(r.path("status").asText()).isEqualTo("ZERO_CAPACITY");assertThat(r.path("maxBuyQuantity").asInt()).isZero();
        }
        p=policy();setting(p,"totalAssets",0);setting(p,"availableCash",0);setting(p,"maxLossPerTrade",0);
        assertThat(sizing.calculate(p,now).path("status").asText()).isEqualTo("ZERO_CAPACITY");
    }
    @Test void staleSettingsUnknownAndUnconfirmedBalancesDoNotBecomeZeroHoldings(){
        var p=policy();((ObjectNode)p.path("accountRisk").path("settings")).put("balanceDate","2026-11-16");
        assertThat(sizing.calculate(p,now).path("status").asText()).isEqualTo("ACCOUNT_RISK_STALE");
        p=policy();((ObjectNode)p.path("holding")).remove("holding");
        assertThat(sizing.calculate(p,now).path("status").asText()).isEqualTo("POSITION_UNCONFIRMED");
        p=policy();((ObjectNode)p.path("holding")).put("positionStatus","UNKNOWN");
        assertThat(sizing.calculate(p,now).path("status").asText()).isEqualTo("POSITION_UNKNOWN");
        p=policy();((ObjectNode)p.path("holding").path("holding")).put("balanceDate","2026-11-16");
        assertThat(sizing.calculate(p,now).path("status").asText()).isEqualTo("POSITION_STALE");
    }
    @Test void missingSettingsAndBadPricePlansFailClosedAndHeldPositionsAreNotActivated(){
        var p=policy();p.remove("accountRisk");assertThat(sizing.calculate(p,now).path("maxBuyQuantity").isNull()).isTrue();
        p=policy();((ObjectNode)p.path("plan")).put("stopLoss",101);
        assertThat(sizing.calculate(p,now).path("status").asText()).isEqualTo("PLAN_INVALID");
        p=policy();((ObjectNode)p.path("holding")).put("positionStatus","HELD");
        assertThat(sizing.calculate(p,now).path("status").asText()).isEqualTo("NEW_ENTRY_ONLY");
    }
    @Test void onlyFinalBuyGetsAQuantityAndCapacityCannotOverrideOtherDecisionBlocks(){
        var arb=new DecisionArbitrator(json);var p=policy();
        var result=arb.finish(json.tree(Map.of("policy",p)),json.tree(Map.of("action","BUY")),null,now.toEpochMilli());
        assertThat(result.path("decisionAction").asText()).isEqualTo("BUY");assertThat(result.path("quantity").asInt()).isEqualTo(47);
        assertThat(result.path("executionStatus").asText()).isEqualTo("PRE_EXECUTION_ONLY");
        setting(p,"maxLossPerTrade",0);
        result=arb.finish(json.tree(Map.of("policy",p)),json.tree(Map.of("action","BUY")),null,now.toEpochMilli());
        assertThat(result.path("decisionAction").asText()).isEqualTo("WAIT");assertThat(result.path("quantity").isNull()).isTrue();
        assertThat(result.path("blockReasons").toString()).contains("ACCOUNT_LIMIT_REACHED");
        p=policy();((ObjectNode)p.path("research")).put("status","MISSING");
        result=arb.finish(json.tree(Map.of("policy",p)),json.tree(Map.of("action","BUY")),null,now.toEpochMilli());
        assertThat(result.path("riskSizing").path("status").asText()).isEqualTo("READY");
        assertThat(result.path("decisionAction").asText()).isEqualTo("WAIT");assertThat(result.path("quantity").isNull()).isTrue();
    }
    @Test void accountFinancialValuesStayInTheServerSnapshotAndAreNotSentToGemini(){
        var input=json.tree(Map.of("policy",policy(),"prompt","test","schema",Map.of("type","object"),"versions",Map.of("maxOutputTokens",1000)));
        String request=json.write(GeminiReviewAdapter.request(input));
        assertThat(request).doesNotContain("accountRisk","totalAssets","availableCash","maxLossPerTrade","maxPositionPercent");
        assertThat(input.path("policy").has("accountRisk")).isTrue();
    }
}
