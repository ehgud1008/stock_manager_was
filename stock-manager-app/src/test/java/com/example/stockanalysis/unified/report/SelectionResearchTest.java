package com.example.stockanalysis.unified.report;

import static com.example.stockanalysis.unified.report.ReportFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.stockanalysis.unified.portfolio.*;
import com.example.stockanalysis.unified.report.ReportModels.*;
import com.example.stockanalysis.unified.scan.UnifiedScanStore;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SelectionResearchTest {
    @Test void oldV4ReplayKeepsOriginalPolicyAndDoesNotInventTiers() throws Exception {
        var data=bundle("005930",true);var old=CandidateAnalyzer.legacyV4(MAPPER,settings()).analyze("r","KOSPI",data.detail(),data.input(),market().indices().get(0));
        var r=report(old,"상승 우세");
        var legacy=new DailyReport(r.runId(),r.status(),r.tradeDate(),r.forecastDate(),r.informationAsOf(),r.finalizedAt(),"candidate-market-v4-common-entry/new-entry-v1/test",r.validationStatus(),r.scope(),r.markets(),r.candidates(),r.assessedStocks(),r.eligibleStocks(),r.limitations());
        var store=mock(ReportStore.class);var scans=mock(UnifiedScanStore.class);
        when(scans.detail("r","005930")).thenReturn(Optional.of(data.detail()));when(store.input("r","005930")).thenReturn(data.input());
        var replay=new SelectionResearchReplay(store,scans,MAPPER);
        assertThat(replay.replay(legacy,settings(),List.of(old),market(),CandidateAnalyzer.ResearchMode.PATTERN_VOLUME)).singleElement().satisfies(s->{
            assertThat(s.newEntry().policyVersion()).isEqualTo("new-entry-v1");assertThat(s.newEntry().suitability()).isNull();
        });
        var json=(com.fasterxml.jackson.databind.node.ObjectNode)MAPPER.valueToTree(legacy);json.remove("tierLists");
        assertThat(MAPPER.treeToValue(json,DailyReport.class).tierLists()).isNull();
    }
    @Test void tierBUsesSameHardRulesAndNeverEntersTheOperatingList() throws Exception {
        var data=bundle("005930",true);var tree=MAPPER.valueToTree(data.detail());
        var analysis=(com.fasterxml.jackson.databind.node.ObjectNode)tree.path("result").path("analysis");
        analysis.put("totalScore",75);((com.fasterxml.jackson.databind.node.ObjectNode)analysis.path("strategy")).put("action","WATCH");
        var detail=MAPPER.treeToValue(tree,com.example.stockanalysis.unified.application.UnifiedAnalysisResponse.class);
        var analyzer=CandidateAnalyzer.legacyV5(MAPPER,settings());
        var b=analyzer.analyze("r","KOSPI",detail,data.input(),market().indices().get(0));
        assertThat(b.disposition()).isEqualTo("RESEARCH_B");assertThat(b.newEntry().status()).isEqualTo(com.example.stockanalysis.judgment.NewEntryAssessment.Status.WATCH);
        assertThat(b.newEntry().timing().status()).isEqualTo(com.example.stockanalysis.judgment.NewEntryAssessment.Status.ELIGIBLE);
        var r=report(b,"상승 우세");
        assertThat(ResearchSelections.rank(List.of(b),r,20)).isEmpty();
        assertThat(ResearchSelections.withTierB(List.of(b),r,20)).singleElement().satisfies(s->assertThat(s.disposition()).isEqualTo("ENTRY_CONDITIONAL"));
        var tiers=TierLists.from(List.of(b),20);assertThat(tiers.comparisonBTotal()).isEqualTo(1);
        var frozen=r.withTierLists(tiers);assertThat(MAPPER.readValue(MAPPER.writeValueAsString(frozen),DailyReport.class).tierLists()).isEqualTo(tiers);
        assertThat(ResearchSelections.withTierB(List.of(b),report(b,"중립"),20).get(0).disposition()).isEqualTo("WATCH");
        ((com.fasterxml.jackson.databind.node.ObjectNode)data.input()).set("sources",MAPPER.createArrayNode());
        assertThat(analyzer.analyze("r","KOSPI",detail,data.input(),market().indices().get(0)).disposition()).isEqualTo("EXCLUDED");
        assertThat(CandidateAnalyzer.legacyV4(MAPPER,settings()).analyze("r","KOSPI",detail,data.input(),market().indices().get(0)).newEntry().suitability()).isNull();
    }
    StockDecision stock() throws Exception {var d=bundle("005930",true);return CandidateAnalyzer.legacyV5(MAPPER,settings()).analyze("r","KOSPI",d.detail(),d.input(),market().indices().get(0));}
    DailyReport report(StockDecision s,String bias) {
        var m=new MarketView("KOSPI",BigDecimal.ONE,BigDecimal.ONE,BigDecimal.TEN,BigDecimal.TEN,1,1,Map.of(),0,bias,"","","",BigDecimal.ONE,BigDecimal.ONE,BigDecimal.TEN,List.of());
        return new DailyReport("r","READY",DAY,DAY.plusDays(1),NOW,NOW,"candidate-market-v5-tier/new-entry-v1/test","UNVALIDATED_RULES","ALL",List.of(m),List.of(s),1,1,List.of());
    }
    StockDecision fail(StockDecision s,String code,Boolean value) {
        var evidence=s.evidence().stream().map(e->e.code().equals(code)?new Evidence(e.code(),e.label(),value==null?null:e.value(),e.unit(),e.comparison(),e.threshold(),value,e.direction(),e.role(),value==null?"UNKNOWN":e.quality(),e.source(),e.tradeDate(),e.observedAt(),e.version()):e).toList();
        return new StockDecision(s.code(),s.name(),s.market(),"EXCLUDED","NONE",evidence,s.cautions(),s.scenario(),s.netRewardRisk(),s.relativeReturn20(),s.liquidity(),s.dailyReturn(),s.aboveEma20(),s.stage(),s.transition61(),s.newEntry());
    }
    @Test void auditSeparatesUnknownSingleAndMultipleFailures() throws Exception {
        var s=stock();var r=report(s,"상승 우세");
        var relative=fail(s,"RELATIVE_RETURN20",false);
        var audit=SelectionDiagnostics.audit(r,List.of(relative));
        assertThat(audit.singleFailure()).isEqualTo(1);assertThat(audit.nearMisses().get(0).rule()).isEqualTo("시장 대비 강도");
        assertThat(audit.commonEligible()).isEqualTo(1);
        assertThat(SelectionDiagnostics.audit(r,List.of(fail(relative,"AMOUNT_TODAY",false))).multipleFailures()).isEqualTo(1);
        var unknown=SelectionDiagnostics.audit(r,List.of(fail(relative,"AMOUNT_TODAY",null)));
        assertThat(unknown.dataUnavailable()).isEqualTo(1);assertThat(unknown.singleFailure()).isZero();assertThat(unknown.nearMisses()).isEmpty();
        assertThat(SelectionDiagnostics.audit(report(s,"중립"),List.of(relative)).multipleFailures()).isEqualTo(1);
    }
    @Test void relativeAlternativeRestoresOnlyKnownFailureAndKeepsMarketAndOtherRules() throws Exception {
        var s=stock();var r=report(s,"상승 우세");var relative=fail(s,"RELATIVE_RETURN20",false);
        var changed=ResearchSelections.withoutRelativeStrength(List.of(relative),r,20);
        assertThat(changed).singleElement().satisfies(v->{assertThat(v.disposition()).isEqualTo("ENTRY_CONDITIONAL");assertThat(v.strategy()).isEqualTo("BREAKOUT");});
        assertThat(relative.disposition()).isEqualTo("EXCLUDED");
        assertThat(ResearchSelections.withoutRelativeStrength(List.of(fail(s,"RELATIVE_RETURN20",null)),r,20)).isEmpty();
        assertThat(ResearchSelections.withoutRelativeStrength(List.of(fail(relative,"VOLUME_RATIO20",false)),r,20)).isEmpty();
        assertThat(ResearchSelections.withoutRelativeStrength(List.of(relative),report(s,"중립"),20).get(0).disposition()).isEqualTo("WATCH");
    }
    @Test void dependentOpenPriceFailureIsOneGroupAndMissingMarketIsUnknown() throws Exception {
        var s=fail(stock(),"OPEN_PRICE",false);var c=s.newEntry();
        var blocked=new com.example.stockanalysis.judgment.NewEntryAssessment(c.policyVersion(),com.example.stockanalysis.judgment.NewEntryAssessment.Status.AVOID,
                List.of("시가 기준 제외"),c.selectedSignal(),c.effectiveScenario(),c.grossRewardRisk(),c.signals());
        var low=new StockDecision(s.code(),s.name(),s.market(),s.disposition(),s.strategy(),s.evidence(),s.cautions(),s.scenario(),s.netRewardRisk(),s.relativeReturn20(),s.liquidity(),s.dailyReturn(),s.aboveEma20(),s.stage(),s.transition61(),blocked);
        var audit=SelectionDiagnostics.audit(report(low,"상승 우세"),List.of(low));
        assertThat(audit.singleFailure()).isEqualTo(1);assertThat(audit.multipleFailures()).isZero();
        var unknown=SelectionDiagnostics.audit(report(low,"판단 보류"),List.of(low));
        assertThat(unknown.dataUnavailable()).isEqualTo(1);assertThat(unknown.singleFailure()).isZero();
    }
    @Test void rawReplayUsesFrozenDataAndRefusesChangedBaseline() throws Exception {
        var data=bundle("005930",true);var s=stock();var report=report(s,"상승 우세");
        var store=mock(ReportStore.class);var scans=mock(UnifiedScanStore.class);
        when(scans.detail("r","005930")).thenReturn(Optional.of(data.detail()));when(store.input("r","005930")).thenReturn(data.input());
        var replay=new SelectionResearchReplay(store,scans,MAPPER);
        assertThat(replay.replay(report,settings(),List.of(s),market(),CandidateAnalyzer.ResearchMode.PATTERN_VOLUME)).hasSize(1);
        assertThatThrownBy(()->replay.replay(report,settings(),List.of(fail(s,"VOLUME_RATIO20",false)),market(),CandidateAnalyzer.ResearchMode.PATTERN_VOLUME))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("RESEARCH_BASELINE_MISMATCH");
        verify(store,never()).complete(any(),any(),any(),any(),any(),any(),anyLong());
    }
    @Test void ageFourReplaysOriginalSignalWithoutExtendingItsOwnExpiry() throws Exception {
        var data=bundle("005930",true);var tree=MAPPER.valueToTree(data.detail());
        var signal=(com.fasterxml.jackson.databind.node.ObjectNode)tree.path("result").path("analysis").path("signalReport").path("signals").get(0);
        signal.put("ageBars",4);signal.put("validForBars",5);signal.put("occurredOn",dates().get(95).toString());
        var detail=MAPPER.treeToValue(tree,com.example.stockanalysis.unified.application.UnifiedAnalysisResponse.class);
        var baseline=CandidateAnalyzer.legacyV5(MAPPER,settings()).analyze("r","KOSPI",detail,data.input(),market().indices().get(0));
        assertThat(baseline.disposition()).isEqualTo("SIGNAL_WAIT");
        var store=mock(ReportStore.class);var scans=mock(UnifiedScanStore.class);
        when(scans.detail("r","005930")).thenReturn(Optional.of(detail));when(store.input("r","005930")).thenReturn(data.input());
        var replay=new SelectionResearchReplay(store,scans,MAPPER);
        var alternate=replay.replay(report(baseline,"상승 우세"),settings(),List.of(baseline),market(),CandidateAnalyzer.ResearchMode.SIGNAL_AGE4);
        assertThat(alternate).singleElement().satisfies(s->{assertThat(s.disposition()).isEqualTo("ENTRY_CONDITIONAL");assertThat(s.newEntry().selectedSignal().ageBars()).isEqualTo(4);});
        assertThat(baseline.newEntry().selectedSignal()).isNull();
        signal.put("validForBars",4);
        var expired=MAPPER.treeToValue(tree,com.example.stockanalysis.unified.application.UnifiedAnalysisResponse.class);
        when(scans.detail("r","005930")).thenReturn(Optional.of(expired));
        var excluded=CandidateAnalyzer.legacyV5(MAPPER,settings()).analyze("r","KOSPI",expired,data.input(),market().indices().get(0));
        assertThat(replay.replay(report(excluded,"상승 우세"),settings(),List.of(excluded),market(),CandidateAnalyzer.ResearchMode.SIGNAL_AGE4)).isEmpty();
        when(scans.detail("r","005930")).thenReturn(Optional.empty());
        assertThatThrownBy(()->replay.replay(report(excluded,"상승 우세"),settings(),List.of(excluded),market(),CandidateAnalyzer.ResearchMode.SIGNAL_AGE4))
                .hasMessage("RESEARCH_INPUT_UNAVAILABLE");
    }
}
