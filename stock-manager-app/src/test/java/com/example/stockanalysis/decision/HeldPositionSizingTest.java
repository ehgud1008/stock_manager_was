package com.example.stockanalysis.decision;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class HeldPositionSizingTest extends PositionSizingPolicyTest {
    ObjectNode held(int quantity){var p=policy();((ObjectNode)p.path("holding")).put("positionStatus","HELD");((ObjectNode)p.path("holding").path("holding")).put("quantity",quantity);p.set("stock",json.tree(Map.of("currentPrice",100)));return p;}
    ObjectNode finish(ObjectNode p,String action,boolean latest){return new DecisionArbitrator(json).finish(json.tree(Map.of("policy",p,"versions",Map.of("sizing",latest?PositionSizingPolicy.VERSION:PositionSizingPolicy.LEGACY_VERSION))),json.tree(Map.of("action",action)),null,now.toEpochMilli());}
    @Test void addConsumesExistingExposureAndRiskBeforeBuying(){
        var r=sizing.calculate(held(10),"ADD",now);
        assertThat(r.path("status").asText()).isEqualTo("READY");assertThat(r.path("maxBuyQuantity").asInt()).isEqualTo(37);
        assertThat(r.path("existingStopRisk").decimalValue()).isEqualByComparingTo("102.30");
        assertThat(r.path("existingStopRisk").decimalValue().add(r.path("estimatedLossAtStop").decimalValue())).isLessThanOrEqualTo(new BigDecimal("500"));
        assertThat(r.path("existingExposure").decimalValue().add(r.path("estimatedPurchaseAmount").decimalValue())).isLessThanOrEqualTo(new BigDecimal("5000"));
        var result=finish(held(10),"ADD",true);assertThat(result.path("decisionAction").asText()).isEqualTo("ADD");assertThat(result.path("quantity").asInt()).isEqualTo(37);
    }
    @Test void reduceOnlyExcessRiskAndWeightWithoutInventingAProfitTakingRatio(){
        var p=held(60);setting(p,"availableCash",0);var r=sizing.calculate(p,"REDUCE",now);
        assertThat(r.path("status").asText()).isEqualTo("READY");assertThat(r.path("tradeQuantity").asInt()).isEqualTo(12);assertThat(r.path("remainingQuantity").asInt()).isEqualTo(48);
        assertThat(finish(p,"REDUCE",true).path("quantity").asInt()).isEqualTo(12);
        assertThat(finish(held(10),"REDUCE",true).path("decisionAction").asText()).isEqualTo("HOLD");
        assertThat(finish(held(10),"REDUCE",true).path("blockReasons").toString()).contains("NO_REDUCTION_NEEDED");
    }
    @Test void fullExitIsNeverSilentlyConvertedFromPartialSale(){
        var p=held(1);setting(p,"maxLossPerTrade",0);var result=finish(p,"REDUCE",true);
        assertThat(result.path("decisionAction").asText()).isEqualTo("HOLD");assertThat(result.path("quantity").isNull()).isTrue();
        assertThat(result.path("blockReasons").toString()).contains("FULL_EXIT_REVIEW_REQUIRED");
    }
    @Test void addRemainsBlockedByEntryRulesAndFreshBalanceRequirements(){
        var p=held(10);p.put("entryEligible",false);assertThat(finish(p,"ADD",true).path("decisionAction").asText()).isEqualTo("HOLD");
        p=held(10);((ObjectNode)p.path("holding").path("holding")).put("balanceDate","2026-11-16");
        assertThat(finish(p,"ADD",true).path("quantity").isNull()).isTrue();assertThat(finish(p,"REDUCE",true).path("blockReasons").toString()).contains("POSITION_STALE");
        p=held(10);((ObjectNode)p.path("stock")).put("currentPrice",89);assertThat(finish(p,"ADD",true).path("decisionAction").asText()).isEqualTo("HOLD");
    }
    @Test void confirmedFullExitHasPriorityAndLegacyHeldActionsStayRestricted(){
        var p=held(10);((ObjectNode)p.path("holding")).put("action","EXIT_TRIGGERED");
        var r=finish(p,"ADD",true);assertThat(r.path("decisionAction").asText()).isEqualTo("SELL");assertThat(r.path("quantity").asInt()).isEqualTo(10);
        assertThat(finish(held(10),"ADD",false).path("decisionAction").asText()).isEqualTo("HOLD");
        assertThat(finish(held(60),"REDUCE",false).path("quantity").isNull()).isTrue();
    }
}
