package com.example.stockanalysis.analysis.api;

import com.example.stockanalysis.analysis.domain.AnalysisRunView;
import com.example.stockanalysis.analysis.domain.AnalysisFactorView;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SwingAssessmentResponseTest {
    @Test void readsVersionedDetailWithoutParsingHumanReasons() {
        var view = mock(AnalysisRunView.class);
        when(view.engineVersion()).thenReturn("analysis-v5-swing");
        when(view.strategyCode()).thenReturn("SWING_V5:PULLBACK:LOW_REWARD_RISK:WAIT");
        when(view.entryFrom()).thenReturn(BigDecimal.TEN);
        var response = SwingAssessmentResponse.from(view);
        assertThat(response.setupLabel()).isEqualTo("눌림목형");
        assertThat(response.detailCode()).isEqualTo("LOW_REWARD_RISK");
        assertThat(response.scenarioStatus()).isEqualTo("AVAILABLE");
    }

    @Test void unavailableScenarioDoesNotHideAvailableRiseAndRiskGroups() {
        var view = mock(AnalysisRunView.class);
        when(view.engineVersion()).thenReturn("analysis-v5-swing");
        when(view.strategyCode()).thenReturn("SWING_V5:BREAKOUT:SCENARIO_UNAVAILABLE:WAIT");
        when(view.factors()).thenReturn(List.of(
                new AnalysisFactorView("TREND", BigDecimal.TEN, new BigDecimal("80"), new BigDecimal(".6"), new BigDecimal("48"), "test"),
                new AnalysisFactorView("ENTRY_LOCATION", BigDecimal.ONE, new BigDecimal("60"), new BigDecimal(".1"), new BigDecimal("6"), "test"),
                new AnalysisFactorView("ENTRY_REWARD_RISK", null, null, BigDecimal.ZERO, null, "missing"),
                new AnalysisFactorView("RISK_STABILITY", BigDecimal.ONE, new BigDecimal("70"), new BigDecimal(".1"), new BigDecimal("7"), "test")));
        var summary = SuitabilityScoresResponse.from(view);
        assertThat(summary.status()).isEqualTo("UNAVAILABLE");
        assertThat(summary.rise()).isEqualByComparingTo("80");
        assertThat(summary.entry()).isNull();
        assertThat(summary.risk()).isEqualByComparingTo("70");
        assertThat(summary.totalFactors()).isEqualTo(10);
        assertThat(SwingAssessmentResponse.from(view).detailLabel()).isEqualTo("목표 시나리오 산출 불가");
    }

    @Test void historicalV4KeepsItsSummaryAndHasNoV5Assessment() {
        var view = mock(AnalysisRunView.class);
        when(view.engineVersion()).thenReturn("analysis-v4-swing");
        when(view.totalScore()).thenReturn(new BigDecimal("58"));
        when(view.factors()).thenReturn(List.of());
        assertThat(SuitabilityScoresResponse.from(view).totalFactors()).isEqualTo(9);
        assertThat(SwingAssessmentResponse.from(view)).isNull();
    }
}
