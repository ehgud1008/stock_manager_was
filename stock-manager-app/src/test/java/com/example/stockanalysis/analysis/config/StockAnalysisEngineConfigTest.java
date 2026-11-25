package com.example.stockanalysis.analysis.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.AnalysisMode;
import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.engine.model.StockSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StockAnalysisEngineConfigTest {

    @Test
    void shortTermStillUsesUnchangedLegacyEngine() {
        var date = LocalDate.of(2026, 9, 19);
        var bars = candles(date, 220);
        var input = new AnalysisInput(new StockSnapshot("005930", "테스트", "KRX", amount("319")),
                AnalysisMode.SHORT_TERM, date, Instant.parse("2026-09-19T00:00:00Z"), bars, bars, bars, Map.of());
        var legacy = new com.example.stockanalysis.engine.internal.DefaultStockAnalysisEngine("analysis-v3", List.of(
                new com.example.stockanalysis.engine.factor.internal.TrendFactorCalculator(),
                new com.example.stockanalysis.engine.factor.internal.MomentumFactorCalculator(),
                new com.example.stockanalysis.engine.factor.internal.VolumeFactorCalculator(),
                new com.example.stockanalysis.engine.factor.internal.SupplyDemandFactorCalculator(),
                new com.example.stockanalysis.engine.factor.internal.RelativeStrengthFactorCalculator(),
                new com.example.stockanalysis.engine.factor.internal.RiskStabilityFactorCalculator(),
                new com.example.stockanalysis.engine.factor.internal.ValueQualityFactorCalculator(),
                com.example.stockanalysis.engine.factor.internal.SwingTimeframeFactorCalculator.daily(),
                com.example.stockanalysis.engine.factor.internal.SwingTimeframeFactorCalculator.weekly(),
                com.example.stockanalysis.engine.factor.internal.SwingTimeframeFactorCalculator.monthly()),
                new com.example.stockanalysis.engine.scoring.WeightedAverageScorePolicy(),
                new com.example.stockanalysis.engine.strategy.DefaultStrategyPolicy(),
                new com.example.stockanalysis.engine.scenario.DefaultScenarioPolicy());
        assertThat(new StockAnalysisEngineConfig().stockAnalysisEngine().analyze(input)).isEqualTo(legacy.analyze(input));
        assertThat(StockAnalysisEngineConfig.engineVersion(AnalysisMode.SHORT_TERM)).isEqualTo("analysis-v3");
    }

    @Test
    void composesDirectionalFactorsWithNormalizedSwingWeights() {
        LocalDate baseDate = LocalDate.of(2026, 9, 3);
        List<DailyCandle> candles = candles(baseDate, 220);
        AnalysisInput input = new AnalysisInput(
                new StockSnapshot("005930", "삼성전자", "KOSPI", amount("319")),
                AnalysisMode.SWING,
                baseDate,
                Instant.parse("2026-09-03T07:00:00Z"),
                candles,
                candles,
                candles,
                Map.of()
        );

        var result = new StockAnalysisEngineConfig().stockAnalysisEngine().analyze(input);

        assertThat(result.engineVersion()).isEqualTo(StockAnalysisEngineConfig.ENGINE_VERSION);
        assertThat(result.signalReport()).isNotNull();
        assertThat(result.factors()).extracting(factor -> factor.factorCode())
                .contains(
                        "TREND",
                        "MOMENTUM",
                        "VOLUME",
                        "SUPPLY_DEMAND",
                        "RELATIVE_STRENGTH",
                        "RISK_STABILITY",
                        "VALUE_QUALITY",
                        "DAILY_TECHNICAL",
                        "WEEKLY_TECHNICAL",
                        "MONTHLY_TECHNICAL"
                        , "ENTRY_LOCATION", "ENTRY_REWARD_RISK", "LIQUIDITY"
                );
        assertThat(result.factors().stream()
                .map(factor -> factor.weight())
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("0.8");
        assertThat(result.totalScore()).isNull(); // Rising straight line has no observed overhead target.
        assertThat(result.strategy().strategyCode()).contains(":SCENARIO_UNAVAILABLE:");
    }

    private List<DailyCandle> candles(LocalDate baseDate, int count) {
        List<DailyCandle> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            BigDecimal close = BigDecimal.valueOf(100 + index);
            result.add(new DailyCandle(
                    baseDate.minusDays(count - index - 1L),
                    close,
                    close.add(BigDecimal.ONE),
                    close.subtract(BigDecimal.ONE),
                    close,
                    1_000L + index
            ));
        }
        return result;
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
