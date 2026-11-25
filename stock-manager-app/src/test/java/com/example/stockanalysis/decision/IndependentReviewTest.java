package com.example.stockanalysis.decision;

import static com.example.stockanalysis.decision.DecisionModels.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.stockanalysis.analysis.domain.DataQualityStatus;
import com.example.stockanalysis.unified.application.UnifiedAnalysisResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;

class IndependentReviewTest {
    DecisionWorkflowTest fixture;DecisionJson json;AiReviewContract contract;ObjectNode input;
    @BeforeEach void setup() {
        fixture=new DecisionWorkflowTest();fixture.setup();json=fixture.json;contract=fixture.contract;
        input=(ObjectNode)json.tree(Map.of("context",new ReviewRequest(Position.NOT_HELD,10),
                "policy",fixture.policy.prepare(fixture.data,new ReviewRequest(Position.NOT_HELD,10))));
    }
    static Review independentBuy(JsonNode input,DecisionJson json) {
        var current=input.path("policy").path("metrics").path("CURRENT").path("value").decimalValue();
        var strategy=new Strategy("MAIN",10,level(current,-2,"CALCULATED"),level(current,0,"OBSERVED"),level(current,-5,"CALCULATED"),
                List.of(level(current,10,"PROJECTION"),level(current,20,"PROJECTION")),"거래량 회복을 확인한 조건부 진입 전략입니다.",
                List.of("MARKET_DATA","TREND"),List.of("단기 수급 회복이 이어진다고 가정합니다."),"지지 이탈 시 전략을 취소합니다.","선택 기간 내 추세가 이어지지 않으면 재검토합니다.");
        return new Review("BUY","MAIN",List.of(new Reason("TREND","추세 회복 가능성을 검토합니다.")),List.of(new Reason("SIGNALS","엔진 확정 신호가 없습니다.")),
                List.of(scenario("UPSIDE"),scenario("RANGE"),scenario("DOWNSIDE")),List.of(),List.of("UPSIDE","DOWNSIDE"),ReviewTestFixtures.detail(),
                new Independent("CONDITIONAL","DISAGREE",ReviewTestFixtures.explain("TREND"),List.of(strategy),null),ReviewTestFixtures.personal("BUY"));
    }
    static Scenario scenario(String id) {return new Scenario(id,"조건 확인 후 대응합니다.","추세와 거래량 변화를 확인합니다.","지지 이탈 시 취소합니다.",List.of("CURRENT"),"MAIN",
            "수급이 유지될 때 추세가 이어진다고 가정합니다.","확인 후 진입을 검토합니다.","추세 훼손 시 청산을 검토합니다.","신호 만료 전에 재검토합니다.");}
    static PriceLevel level(BigDecimal source,int adjustment,String kind) {return new PriceLevel(source.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(adjustment).movePointLeft(2))).setScale(2,RoundingMode.HALF_UP),
            kind,"/metrics/CURRENT/value","OBSERVED".equals(kind)?null:BigDecimal.valueOf(adjustment),"저장 종가를 기준으로 추세 유지 가정을 적용했습니다.");}
    ObjectNode review() {return (ObjectNode)json.tree(independentBuy(input,json));}
    ObjectNode strategy(ObjectNode r) {return (ObjectNode)r.path("independent").path("strategies").get(0);}
    @Test void engineWaitDoesNotPreventIndependentBuyAndWorkerPersistsRecalculation() {
        String id=fixture.decide(fixture.snapshot(),"independent").id();AiReviewPort ai=mock(AiReviewPort.class);
        when(ai.review(any())).thenAnswer(invocation->{JsonNode frozen=invocation.getArgument(0);var r=independentBuy(frozen,json);return new AiResponse(r,"response",json.tree(r),10,20);});
        var worker=fixture.worker(ai);try {worker.tick();} finally {worker.close();}
        var result=fixture.service.get(id,"DECISION");assertThat(result.status()).isEqualTo("SUCCEEDED");assertThat(result.action()).isEqualTo("BUY");
        assertThat(result.result().path("policy").path("engineAssessment").asText()).isEqualTo("WAIT");
        assertThat(result.result().path("strategyCalculations").size()).isEqualTo(1);
        assertThat(fixture.service.get(id,"DECISION").result()).isEqualTo(result.result());verify(ai,times(1)).review(any());
    }
    @Test void heldPositionCanSellOrAddWithoutEngineSellOrBuySignal() {
        ((ObjectNode)input.path("context")).put("positionStatus","HELD");var r=review();
        contract.validate(contract.parse(r),input);r.put("recommendation","SELL");r.set("personalization",ReviewTestFixtures.personal("SELL"));contract.validate(contract.parse(r),input);
        r.put("recommendation","WAIT");assertThatThrownBy(()->contract.validate(contract.parse(r),input)).hasMessage("AI_INVALID_RESPONSE");
    }
    @Test void fabricatedPriceOrInvalidFormulaOrRelabelledCalculatedMetricIsRejected() {
        var r=review();var level=(ObjectNode)strategy(r).path("targets").get(0);level.put("value",12345);
        assertThatThrownBy(()->contract.validate(contract.parse(r),input)).hasMessage("AI_INVALID_RESPONSE");
        level.put("sourcePath","/metrics/HORIZON/value");assertThatThrownBy(()->contract.validate(contract.parse(r),input)).hasMessage("AI_INVALID_RESPONSE");
        var observed=review();var stop=(ObjectNode)strategy(observed).path("stop");stop.put("kind","OBSERVED");stop.putNull("adjustmentPct");stop.put("sourcePath","/metrics/STOP/value");
        assertThatThrownBy(()->contract.validate(contract.parse(observed),input)).hasMessage("AI_INVALID_RESPONSE");
    }
    @Test void totalHoldingAmountsCannotBeUsedAsPerSharePriceSources() {
        var r=review();var level=(ObjectNode)strategy(r).path("targets").get(0);
        ((ObjectNode)input.path("policy").path("metrics")).set("VALUE_AMOUNT",json.tree(Map.of("unit","원","value",1000)));
        level.put("sourcePath","/metrics/VALUE_AMOUNT/value");level.put("kind","CALCULATED");level.put("adjustmentPct",10);level.put("value",1100);
        assertThatThrownBy(()->contract.validate(contract.parse(r),input)).hasMessage("AI_INVALID_RESPONSE");
    }
    @Test void actualCandlePriceMayBeUsedButVolumeAndUnknownRowsCannot() {
        var r=review();var target=(ObjectNode)strategy(r).path("targets").get(1);
        var candles=(com.fasterxml.jackson.databind.node.ArrayNode)input.path("policy").path("marketData").path("candles");
        candles.set(0,json.tree(List.of("2026-01-01",1300,1500,1200,1400,1000000)));
        target.put("kind","OBSERVED");target.putNull("adjustmentPct");target.put("value",1500);target.put("sourcePath","/marketData/candles/0/2");
        contract.validate(contract.parse(r),input);
        target.put("sourcePath","/marketData/candles/0/5");assertThatThrownBy(()->contract.validate(contract.parse(r),input)).isInstanceOf(AiFailure.class);
        target.put("sourcePath","/marketData/candles/999/2");assertThatThrownBy(()->contract.validate(contract.parse(r),input)).isInstanceOf(AiFailure.class);
    }
    @Test void inconsistentOrderingPrimaryHorizonAndScenarioReferencesAreRejected() {
        var wrongStop=review();strategy(wrongStop).set("stop",strategy(wrongStop).path("entryHigh"));
        assertThatThrownBy(()->contract.validate(contract.parse(wrongStop),input)).hasMessage("AI_INVALID_RESPONSE");
        var horizon=review();strategy(horizon).put("horizonTradingDays",20);assertThatThrownBy(()->contract.validate(contract.parse(horizon),input)).isInstanceOf(AiFailure.class);
        var orphan=review();((ObjectNode)orphan.path("scenarios").get(0)).put("strategyId","MISSING");assertThatThrownBy(()->contract.validate(contract.parse(orphan),input)).isInstanceOf(AiFailure.class);
        var noPlan=review();noPlan.putNull("planId");assertThatThrownBy(()->contract.validate(contract.parse(noPlan),input)).isInstanceOf(AiFailure.class);
    }
    @Test void independentRiskIsRecomputedFromPricesAndCostBasisIsSeparate() {
        ((ObjectNode)input.path("policy").path("metrics").path("CURRENT")).put("value",100);
        ((ObjectNode)input.path("context")).put("positionStatus","HELD").put("averageBuyPrice",80);
        var r=independentBuy(input,json);contract.validate(r,input);var row=new AiStrategyCalculator(json).calculate(r,input).get(0);
        assertThat(row.path("referenceKind").asText()).isEqualTo("CURRENT_PRICE");assertThat(row.path("lossPct").decimalValue()).isEqualByComparingTo("5");
        assertThat(row.path("targets").get(0).path("rewardRisk").decimalValue()).isEqualByComparingTo("2");
        assertThat(row.path("costBasisReturnPct").decimalValue()).isEqualByComparingTo("25");
        assertThat(row.path("targets").get(0).path("upsidePct").decimalValue()).isEqualByComparingTo("10");
    }
    @Test void shortHistoryOrIncompleteEngineDoesNotBlockIndependentInterpretation() {
        var shortData=new UnifiedAnalysisResponse(fixture.data.result(),DataQualityStatus.PARTIAL,List.of("재무 자료 일부 누락"),null,null,
                fixture.data.priceHistory().subList(280,300));
        var p=fixture.policy.prepare(shortData,new ReviewRequest(Position.NOT_HELD,60));
        assertThat(p.path("blockReason").isNull()).isTrue();assertThat(p.path("engineDataReady").asBoolean()).isFalse();
        assertThat(p.path("horizonPlan").path("status").asText()).isEqualTo("HISTORY_INSUFFICIENT");
        assertThat(p.path("marketData").path("candles").size()).isEqualTo(20);
    }
    @Test void serializedProviderRequestContainsFrozenHistoryAndFitsDefaultBudget() throws java.io.IOException {
        input.put("prompt",AiReviewContract.PROMPT);input.set("schema",contract.schema());input.set("versions",json.tree(Map.of("maxOutputTokens",12000,"thinkingLevel","LOW")));
        var request=GeminiReviewAdapter.request(input);var serialized=json.write(request);
        assertThat(serialized).contains("marketData","volume").doesNotContain("comparisons","allowedActions");
        assertThat(serialized.getBytes(StandardCharsets.UTF_8).length).isLessThan(128000);
        // Synthetic fixture only; useful for inspecting provider schema compatibility without exposing credentials.
        java.nio.file.Files.writeString(java.nio.file.Path.of("target","gemini-independent-request.json"),serialized);
    }
    @Test void legacyCompletedResultsRemainReadableAndCanBeReevaluatedWithNewContract() {
        String snapshot=fixture.snapshot();var first=fixture.decide(snapshot,"legacy-read");var claimed=fixture.store.claim(fixture.settings.ownerScope(),"legacy",fixture.clock.millis(),1000).orElseThrow();
        var legacy=json.tree(Map.of("finalAction","WAIT","aiReview",Map.of("supportingReasons",List.of()),"versions",Map.of("contract","ai-review-v2")));
        fixture.store.complete(claimed,legacy,fixture.clock.millis());assertThat(fixture.service.get(first.id(),"DECISION").result()).isEqualTo(legacy);
        assertThat(fixture.decide(snapshot,"legacy-read").id()).isEqualTo(first.id());
        var revision=fixture.service.revision(first.id(),"new-contract");assertThat(revision.id()).isNotEqualTo(first.id());
        assertThat(fixture.store.find(fixture.settings.ownerScope(),revision.id()).orElseThrow().input().path("versions").path("contract").asText()).isEqualTo(AiReviewContract.VERSION);
    }
    @Test void oldQueuedContractFailsBeforeCallingAiAndOriginalRequestStillReplays() {
        String snapshot=fixture.snapshot();var first=fixture.decide(snapshot,"old-queue");
        var frozen=(ObjectNode)fixture.store.find(fixture.settings.ownerScope(),first.id()).orElseThrow().input();
        ((ObjectNode)frozen.path("versions")).put("contract","ai-review-v2");
        new org.springframework.jdbc.core.JdbcTemplate(fixture.ds).update("UPDATE td_job SET input_json=? WHERE job_id=?",json.write(frozen),first.id());
        var ai=mock(AiReviewPort.class);var worker=fixture.worker(ai);try {worker.tick();} finally {worker.close();}
        assertThat(fixture.service.get(first.id(),"DECISION").errorCode()).isEqualTo("POLICY_VERSION_CHANGED");verifyNoInteractions(ai);
        assertThat(fixture.decide(snapshot,"old-queue").id()).isEqualTo(first.id());
    }
}
