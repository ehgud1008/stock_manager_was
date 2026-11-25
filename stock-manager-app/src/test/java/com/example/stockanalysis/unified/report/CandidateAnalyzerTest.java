package com.example.stockanalysis.unified.report;

import static com.example.stockanalysis.unified.report.ReportFixtures.*;
import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;

class CandidateAnalyzerTest {
    final CandidateAnalyzer analyzer=CandidateAnalyzer.legacyV5(MAPPER,settings());
    @Test void researchVolumeUsesEventBarAndDoesNotChangeLiveSelection() throws Exception {
        var data=bundle("005930",true);var tree=(ObjectNode)MAPPER.valueToTree(data.detail());
        var candles=(ArrayNode)data.input().path("input").path("dailyCandles");int last=candles.size()-1;
        ((ObjectNode)candles.get(last)).put("volume",1000000);
        ((ObjectNode)candles.get(last-1)).put("volume",2000000);
        var signal=(ObjectNode)tree.path("result").path("analysis").path("signalReport").path("signals").get(0);
        signal.put("occurredOn",candles.get(last-1).path("businessDate").asText());signal.put("ageBars",1);signal.put("state","ACTIVE");
        var detail=MAPPER.treeToValue(tree,com.example.stockanalysis.unified.application.UnifiedAnalysisResponse.class);
        var before=analyzer.analyze("r","KOSPI",detail,data.input(),market().indices().get(0));
        assertThat(before.disposition()).isEqualTo("EXCLUDED");
        var research=analyzer.analyze("r","KOSPI",detail,data.input(),market().indices().get(0),CandidateAnalyzer.ResearchMode.PATTERN_VOLUME);
        assertThat(research.disposition()).isEqualTo("ENTRY_CONDITIONAL");
        assertThat(research.evidence()).filteredOn(e->e.code().equals("RESEARCH_VOLUME")).singleElement().satisfies(e->assertThat(e.passed()).isTrue());
        assertThat(analyzer.analyze("r","KOSPI",detail,data.input(),market().indices().get(0))).isEqualTo(before);
    }
    @Test void recoveryVolumeAcceptsEqualEventVolumeButNotMissingDenominator() throws Exception {
        for(var pattern:java.util.List.of("PULLBACK_RECOVERY","BREAKOUT_RETEST")) {
            var data=bundle("005930",true);var tree=(ObjectNode)MAPPER.valueToTree(data.detail());
            var candles=(ArrayNode)data.input().path("input").path("dailyCandles");int last=candles.size()-1;
            ((ObjectNode)candles.get(last)).put("volume",1000000);
            var signal=(ObjectNode)tree.path("result").path("analysis").path("signalReport").path("signals").get(0);
            signal.put("type",pattern);signal.put("occurredOn",candles.get(last-1).path("businessDate").asText());signal.put("ageBars",1);
            var detail=MAPPER.treeToValue(tree,com.example.stockanalysis.unified.application.UnifiedAnalysisResponse.class);
            var equal=analyzer.analyze("r","KOSPI",detail,data.input(),market().indices().get(0),CandidateAnalyzer.ResearchMode.PATTERN_VOLUME);
            assertThat(equal.disposition()).as(pattern).isEqualTo("ENTRY_CONDITIONAL");
            ((ObjectNode)candles.get(last-2)).put("volume",0);
            var unknown=analyzer.analyze("r","KOSPI",detail,data.input(),market().indices().get(0),CandidateAnalyzer.ResearchMode.PATTERN_VOLUME);
            assertThat(unknown.disposition()).isEqualTo("EXCLUDED");
            assertThat(unknown.evidence()).filteredOn(e->e.code().equals("RESEARCH_VOLUME")).singleElement().satisfies(e->assertThat(e.passed()).isNull());
        }
    }
    @Test void latestUnusableSignalDoesNotHideOlderEligibleSignalAndBothConsumersAgree() throws Exception {
        var data=bundle("005930",true);var tree=(ObjectNode)MAPPER.valueToTree(data.detail());
        var signals=(ArrayNode)tree.path("result").path("analysis").path("signalReport").path("signals");
        var older=(ObjectNode)signals.get(0);older.put("id","older-valid");older.put("occurredOn",DAY.minusDays(1).toString());
        older.put("ageBars",1);older.put("state","ACTIVE");older.put("entryTo",19950);
        var latest=older.deepCopy();latest.put("id","latest-outside");latest.put("occurredOn",DAY.toString());
        latest.put("ageBars",0);latest.put("entryFrom",20000);latest.put("entryTo",20200);signals.add(latest);
        var detail=MAPPER.treeToValue(tree,com.example.stockanalysis.unified.application.UnifiedAnalysisResponse.class);
        var selected=analyzer.analyze("r","KOSPI",detail,data.input(),market().indices().get(0));
        assertThat(selected.disposition()).isEqualTo("ENTRY_CONDITIONAL");
        assertThat(selected.newEntry().selectedSignal().id()).isEqualTo("older-valid");
        assertThat(selected.scenario().entryTo()).isEqualByComparingTo("19950");
        var judgment=new com.example.stockanalysis.judgment.TradeJudgmentEngine().evaluate(
                new com.example.stockanalysis.judgment.TradeJudgmentEngine.Input(detail.result().analysis(),detail.result().stock().currentPrice(),DAY,NOW,"COMPLETE"));
        assertThat(selected.newEntry()).usingRecursiveComparison().ignoringFields("policyVersion").isEqualTo(judgment.newEntry());
        var expected=com.example.stockanalysis.judgment.NewEntryEvaluator.rewardRisk(selected.scenario(),settings().roundTripCostBps());
        assertThat(selected.netRewardRisk()).isEqualByComparingTo(expected.setScale(6,java.math.RoundingMode.HALF_UP));
        var restored=MAPPER.readValue(MAPPER.writeValueAsString(selected),ReportModels.StockDecision.class);
        assertThat(restored).isEqualTo(selected);
    }
    @Test void reportNeverOverridesCommonDataOrTrendDeferral() throws Exception {
        for(var failure:java.util.List.of("partial","notReady","missingTrend","lowTrend","timeMismatch","age4")) {
            var data=bundle("005930",true);var tree=(ObjectNode)MAPPER.valueToTree(data.detail());
            var analysis=(ObjectNode)tree.path("result").path("analysis");
            switch(failure) {
                case "partial" -> tree.put("collectionQuality","PARTIAL");
                case "notReady" -> ((ObjectNode)analysis.path("signalReport")).put("completeness","INSUFFICIENT_DATA");
                case "missingTrend", "lowTrend" -> {
                    for(var factor:analysis.path("factors")) if("TREND".equals(factor.path("factorCode").asText())) {
                        if(failure.equals("missingTrend")) { ((ObjectNode)factor).putNull("score"); ((ObjectNode)factor).put("weight",0); }
                        else ((ObjectNode)factor).put("score",59);
                    }
                }
                case "timeMismatch" -> ((ObjectNode)analysis.path("signalReport")).put("evaluatedAt",NOW.minusSeconds(1).toString());
                default -> ((ObjectNode)analysis.path("signalReport").path("signals").get(0)).put("ageBars",4);
            }
            var detail=MAPPER.treeToValue(tree,com.example.stockanalysis.unified.application.UnifiedAnalysisResponse.class);
            var selected=analyzer.analyze("r","KOSPI",detail,data.input(),market().indices().get(0));
            assertThat(selected.disposition()).as(failure).isEqualTo(failure.equals("age4")?"SIGNAL_WAIT":"EXCLUDED");
            assertThat(selected.evidence()).filteredOn(e->e.code().equals("COMMON_ENTRY")).singleElement().satisfies(e->assertThat(e.passed()).isFalse());
            var judgment=new com.example.stockanalysis.judgment.TradeJudgmentEngine().evaluate(
                    new com.example.stockanalysis.judgment.TradeJudgmentEngine.Input(detail.result().analysis(),detail.result().stock().currentPrice(),DAY,NOW,detail.collectionQuality().name()));
            assertThat(selected.newEntry()).as(failure).usingRecursiveComparison().ignoringFields("policyVersion").isEqualTo(judgment.newEntry());
            assertThat(selected.dailyReturn()).isNotNull();
        }
    }
    @Test void historicalDecisionWithoutCommonAssessmentStillDeserializes() throws Exception {
        var data=bundle("005930",true);
        var selected=analyzer.analyze("r","KOSPI",data.detail(),data.input(),market().indices().get(0));
        var tree=(ObjectNode)MAPPER.valueToTree(selected);tree.remove("newEntry");
        assertThat(MAPPER.treeToValue(tree,ReportModels.StockDecision.class).newEntry()).isNull();
    }
    @Test void openingPriceAtOrBelow2000IsExcludedWithoutLosingMarketBreadth() throws Exception {
        for(var price:java.util.List.of("1999","2000","2000.01")) {
            var data=bundle("005930",true);
            var rows=(ArrayNode)data.input().path("input").path("dailyCandles");
            ((ObjectNode)rows.get(rows.size()-1)).put("open",new java.math.BigDecimal(price));
            ((ObjectNode)rows.get(rows.size()-1)).put("low",new java.math.BigDecimal(price));
            var result=analyzer.analyze("r","KOSPI",data.detail(),data.input(),market().indices().get(0));
            boolean eligible=new java.math.BigDecimal(price).compareTo(new java.math.BigDecimal("2000"))>0;
            assertThat(result.disposition()).isEqualTo(eligible?"ENTRY_CONDITIONAL":"EXCLUDED");
            assertThat(result.evidence()).filteredOn(e->e.code().equals("OPEN_PRICE")).singleElement()
                    .satisfies(e->assertThat(e.passed()).isEqualTo(eligible));
            assertThat(result.dailyReturn()).isNotNull();
            assertThat(result.stage()).isNotNull();
        }
    }
    @Test void storesStageAccelerationGapLiquidityAndCounterEvidenceWithoutProbability() throws Exception {
        var data=bundle("005930",true);var result=analyzer.analyze("r","KOSPI",data.detail(),data.input(),market().indices().get(0));
        assertThat(result.disposition()).isEqualTo("ENTRY_CONDITIONAL");
        assertThat(result.netRewardRisk()).isPositive();
        assertThat(result.evidence()).extracting(ReportModels.Evidence::code).contains("STAGE_6_TO_1","EMA5_ACCELERATION","EMA5_20_EXPANSION","AMOUNT_AVERAGE20","RELATIVE_RETURN20","NO_SELL_CONFLICT","NET_REWARD_RISK");
        assertThat(result.evidence()).filteredOn(e->e.code().equals("FOREIGN_FLOW5")).singleElement().satisfies(e->{assertThat(e.value()).isNull();assertThat(e.quality()).isEqualTo("UNKNOWN");});
        assertThat(MAPPER.writeValueAsString(result)).doesNotContain("probability","winRate");
    }
    @Test void stageTransitionAloneAndMissingLiquidityCannotQualify() throws Exception {
        var data=bundle("005930",true);var tree=(ObjectNode)MAPPER.valueToTree(data.detail());
        ((ObjectNode)tree.path("result").path("analysis").path("signalReport")).set("signals",MAPPER.createArrayNode());
        var detail=MAPPER.treeToValue(tree,com.example.stockanalysis.unified.application.UnifiedAnalysisResponse.class);
        assertThat(analyzer.analyze("r","KOSPI",detail,data.input(),market().indices().get(0)).disposition()).isEqualTo("SIGNAL_WAIT");
        ((ObjectNode)data.input()).set("sources",MAPPER.createArrayNode());
        var excluded=analyzer.analyze("r","KOSPI",detail,data.input(),market().indices().get(0));
        assertThat(excluded.disposition()).isEqualTo("SIGNAL_WAIT");
        assertThat(excluded.evidence()).filteredOn(e->e.code().equals("AMOUNT_TODAY")).singleElement().satisfies(e->assertThat(e.passed()).isNull());
    }
    @Test void conflictingLiquidityAndSellSignalsAreNotPositiveEvidence() throws Exception {
        var data=bundle("005930",true);var rows=(ArrayNode)data.input().path("sources").get(0).path("records");
        var duplicate=rows.get(rows.size()-1).deepCopy();((ObjectNode)duplicate.path("values")).put("tradingAmount","99999");rows.add(duplicate);
        assertThat(analyzer.analyze("r","KOSPI",data.detail(),data.input(),market().indices().get(0)).disposition()).isEqualTo("EXCLUDED");
        data=bundle("005930",true);var tree=(ObjectNode)MAPPER.valueToTree(data.detail());
        var signals=(ArrayNode)tree.path("result").path("analysis").path("signalReport").path("signals");
        var sell=(ObjectNode)signals.get(0).deepCopy();sell.put("side","SELL");sell.put("type","TREND_BREAKDOWN");signals.add(sell);
        var detail=MAPPER.treeToValue(tree,com.example.stockanalysis.unified.application.UnifiedAnalysisResponse.class);
        assertThat(analyzer.analyze("r","KOSPI",detail,data.input(),market().indices().get(0)).disposition()).isEqualTo("EXCLUDED");
    }
    @Test void samePeriodIndexHistoryIsRequiredAndThresholdsAreVersionedInputs() throws Exception {
        var data=bundle("005930",true);var index=market().indices().get(0);var bars=new java.util.ArrayList<>(index.bars());bars.remove(bars.size()-21);
        var incomplete=new ReportModels.IndexSnapshot(index.market(),index.code(),index.unit(),index.tradeDate(),index.collectedAt(),bars,index.source(),index.warnings());
        assertThat(analyzer.analyze("r","KOSPI",data.detail(),data.input(),incomplete).disposition()).isEqualTo("EXCLUDED");
        var strict=new ReportProperties(true,settings().minimumCoverage(),settings().minimumDailyAmountMillion(),settings().minimumAverageAmountMillion(),new java.math.BigDecimal("10"),settings().roundTripCostBps(),settings().maximumEma20Distance(),20);
        assertThat(new CandidateAnalyzer(MAPPER,strict).analyze("r","KOSPI",data.detail(),data.input(),index).disposition()).isEqualTo("EXCLUDED");
    }
    @Test void lowOrUnknownScoreAndNonBuyActionCannotQualify() throws Exception {
        for(String field:java.util.List.of("lowScore","missingScore","action","stage","staleSignal")) {
            var data=bundle("005930",true);var tree=(ObjectNode)MAPPER.valueToTree(data.detail());
            var analysis=(ObjectNode)tree.path("result").path("analysis");
            switch(field) {
                case "lowScore" -> analysis.put("totalScore",79.99);
                case "missingScore" -> analysis.putNull("totalScore");
                case "action" -> ((ObjectNode)analysis.path("strategy")).put("action","WATCH");
                case "stage" -> ((ObjectNode)tree.path("result").path("screener")).put("stage",2);
                default -> ((ObjectNode)analysis.path("signalReport").path("signals").get(0)).put("evaluatedOn",DAY.minusDays(1).toString());
            }
            var detail=MAPPER.treeToValue(tree,com.example.stockanalysis.unified.application.UnifiedAnalysisResponse.class);
            var result=analyzer.analyze("r","KOSPI",detail,data.input(),market().indices().get(0));
            assertThat(result.disposition()).as(field).isEqualTo(field.equals("lowScore")?"RESEARCH_B":field.equals("action")?"ENTRY_CONDITIONAL":"EXCLUDED");
            if(!field.equals("action"))assertThat(result.cautions()).anyMatch(s->s.startsWith("선정 제외:"));
        }
    }
}
