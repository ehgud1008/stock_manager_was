package com.example.stockanalysis.unified.report;

import static com.example.stockanalysis.unified.report.ReportFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.stockanalysis.unified.application.UnifiedAnalysisResponse;
import com.example.stockanalysis.unified.portfolio.*;
import com.example.stockanalysis.unified.scan.UnifiedScanStore;
import com.example.stockanalysis.unified.report.ReportModels.*;
import com.fasterxml.jackson.databind.node.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class ProfitabilityPolicyTest {
    @Test void qualifiedTierBPassesReportButStillRequiresNetRewardAndActualLiquidity() throws Exception {
        for(String pattern:List.of("PULLBACK_RECOVERY","BREAKOUT_RETEST")) {
            var data=bundle("005930",true);var tree=(ObjectNode)MAPPER.valueToTree(data.detail());
            var analysis=(ObjectNode)tree.path("result").path("analysis");analysis.put("totalScore",75);
            ((ObjectNode)analysis.path("strategy")).put("action","WATCH");
            ((ObjectNode)analysis.path("signalReport").path("signals").get(0)).put("type",pattern);
            for(var factor:analysis.path("factors"))if(factor.path("factorCode").asText().equals("TREND"))((ObjectNode)factor).put("score",70);
            var detail=MAPPER.treeToValue(tree,UnifiedAnalysisResponse.class);
            var analyzer=new CandidateAnalyzer(MAPPER,settings());
            var selected=analyzer.analyze("r","KOSPI",detail,data.input(),market().indices().get(0));
            assertThat(selected.disposition()).isEqualTo("ENTRY_CONDITIONAL");
            assertThat(selected.newEntry().suitability().tier()).isEqualTo("B");
            assertThat(selected.newEntry().policyVersion()).isEqualTo("new-entry-v3-tier");
            assertThat(selected.evidence()).filteredOn(e->e.code().equals("OPERATING_TIER")).allMatch(e->Boolean.TRUE.equals(e.passed()));
            assertThat(CandidateAnalyzer.legacyV6(MAPPER,settings()).analyze("r","KOSPI",detail,data.input(),market().indices().get(0)).disposition()).isEqualTo("RESEARCH_B");
            var strict=new ReportProperties(true,settings().minimumCoverage(),settings().minimumDailyAmountMillion(),settings().minimumAverageAmountMillion(),new BigDecimal("3"),settings().roundTripCostBps(),settings().maximumEma20Distance(),20);
            assertThat(new CandidateAnalyzer(MAPPER,strict).analyze("r","KOSPI",detail,data.input(),market().indices().get(0)).disposition()).isEqualTo("EXCLUDED");
            ((ObjectNode)data.input()).set("sources",MAPPER.createArrayNode());
            assertThat(analyzer.analyze("r","KOSPI",detail,data.input(),market().indices().get(0)).disposition()).isEqualTo("EXCLUDED");
        }
    }

    @Test void savedV6ReplayKeepsOldVolumeThresholdEvidenceAndVersion() throws Exception {
        var data=bundle("005930",true);
        var candles=(ArrayNode)data.input().path("input").path("dailyCandles");
        ((ObjectNode)candles.get(candles.size()-1)).put("volume",1300000);
        var old=CandidateAnalyzer.legacyV6(MAPPER,settings()).analyze("r","KOSPI",data.detail(),data.input(),market().indices().get(0));
        assertThat(old.disposition()).isEqualTo("ENTRY_CONDITIONAL");
        assertThat(new CandidateAnalyzer(MAPPER,settings()).analyze("r","KOSPI",data.detail(),data.input(),market().indices().get(0)).disposition()).isEqualTo("EXCLUDED");
        var report=new DailyReport("r","READY",DAY,DAY.plusDays(1),NOW,NOW,"candidate-v6-signals/new-entry-v2-tier/test","UNVALIDATED_RULES","ALL",List.of(),List.of(old),1,1,List.of());
        var store=mock(ReportStore.class);var scans=mock(UnifiedScanStore.class);
        when(scans.detail("r","005930")).thenReturn(Optional.of(data.detail()));when(store.input("r","005930")).thenReturn(data.input());
        var reproduced=new SelectionResearchReplay(store,scans,MAPPER).replay(report,settings(),List.of(old),market(),CandidateAnalyzer.ResearchMode.PATTERN_VOLUME);
        assertThat(reproduced).containsExactly(old);
        assertThat(old.evidence()).noneMatch(e->e.code().equals("BREAKOUT_CLOSE"));
        assertThat(old.newEntry().policyVersion()).isEqualTo("new-entry-v2-tier");
    }

    @Test void breakoutCandleGateIsAppliedEvenToPreviouslySavedSignals() throws Exception {
        var data=bundle("005930",true);
        var candles=(ArrayNode)data.input().path("input").path("dailyCandles");
        ((ObjectNode)candles.get(candles.size()-1)).put("high",21000);
        var result=new CandidateAnalyzer(MAPPER,settings()).analyze("r","KOSPI",data.detail(),data.input(),market().indices().get(0));
        assertThat(result.disposition()).isEqualTo("EXCLUDED");
        assertThat(result.evidence()).filteredOn(e->e.code().equals("BREAKOUT_CLOSE")).singleElement().satisfies(e->assertThat(e.passed()).isFalse());
        assertThat(CandidateAnalyzer.legacyV6(MAPPER,settings()).analyze("r","KOSPI",data.detail(),data.input(),market().indices().get(0)).disposition()).isEqualTo("ENTRY_CONDITIONAL");
    }
}
