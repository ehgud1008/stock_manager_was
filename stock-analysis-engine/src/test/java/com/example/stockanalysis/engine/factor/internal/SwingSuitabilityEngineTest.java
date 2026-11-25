package com.example.stockanalysis.engine.factor.internal;

import com.example.stockanalysis.engine.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SwingSuitabilityEngineTest {
    @Test void confirmedFullWindowNewHighIsScoredWithExplicitSyntheticBasis() {
        var source=input(253,false,Map.of());
        var bars=source.dailyCandles().stream().map(b->new DailyCandle(b.businessDate(),b.open(),b.close().add(new BigDecimal("5")),b.close().subtract(new BigDecimal("5")),b.close(),b.volume())).toList();
        var complete=new AnalysisInput(source.stock(),source.mode(),source.baseDate(),source.baseDate().plusDays(1).atStartOfDay(java.time.ZoneId.of("Asia/Seoul")).toInstant(),
                bars,source.weeklyCandles(),source.monthlyCandles(),source.observations());
        var result=engine.analyze(complete);
        assertThat(result.engineVersion()).isEqualTo(SwingSuitabilityEngine.VERSION);
        assertThat(result.totalScore()).isNotNull();
        assertThat(result.scenario()).isNotNull();
        assertThat(result.warnings()).anyMatch(w->w.startsWith("SYNTHETIC_TARGET:"));
        assertThat(factor(result,"ENTRY_REWARD_RISK").reason()).contains("가정 목표");
        assertThat(SwingSuitabilityEngine.legacyV6().analyze(complete).scenario()).isNull();
        var preliminary=new AnalysisInput(source.stock(),source.mode(),source.baseDate(),source.dataCutoffAt(),bars,source.weeklyCandles(),source.monthlyCandles(),source.observations());
        assertThat(engine.analyze(preliminary).scenario()).isNull();
    }
    private final SwingSuitabilityEngine engine = new SwingSuitabilityEngine();

    @Test void missingCoreDataDoesNotBecomeNeutralOrZeroScore() {
        var result = engine.analyze(input(10, false, Map.of()));
        assertThat(result.totalScore()).isNull();
        assertThat(result.scenario()).isNull();
        assertThat(result.strategy().strategyCode()).isEqualTo("SWING_V5:UNCLASSIFIED:DATA_MISSING:WAIT");
        assertThat(result.factors()).allMatch(f -> f.score() == null && f.weight().signum() == 0);
    }

    @Test void missingOptionalDataIsVisibleAndOnlyRiseWeightsAreRedistributed() {
        var result = engine.analyze(input(100, true, Map.of()));
        assertThat(factor(result, "RELATIVE_STRENGTH").score()).isNull();
        assertThat(factor(result, "SUPPLY_DEMAND").score()).isNull();
        assertThat(factor(result, "TREND").weight()).isEqualByComparingTo("0.2769230769");
        assertThat(result.factors().stream().map(FactorResult::weight).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("1");
        assertThat(result.totalScore()).isEqualByComparingTo(result.factors().stream().filter(f -> f.score() != null)
                .map(f -> f.score().multiply(f.weight())).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, java.math.RoundingMode.HALF_UP));
        assertThat(result.warnings()).anyMatch(s -> s.contains("잠정"));
        assertThat(factor(result, "DAILY_TECHNICAL").weight()).isZero();
        assertThat(factor(result, "WEEKLY_TECHNICAL").weight()).isZero();
        assertThat(result.strategy().action()).isEqualTo(SwingSuitabilityEngine.actionFor(result.totalScore()));
    }

    @Test void unobservedBreakoutTargetIsNotFabricated() {
        var result = engine.analyze(input(100, false, Map.of()));
        assertThat(result.scenario()).isNull();
        assertThat(factor(result, "ENTRY_LOCATION").score()).isGreaterThan(BigDecimal.ZERO);
        assertThat(factor(result, "ENTRY_REWARD_RISK").score()).isNull();
        assertThat(factor(result, "ENTRY_STOP_DISTANCE").score()).isNull();
        assertThat(result.totalScore()).isNull();
        assertThat(result.strategy().strategyCode()).contains(":SCENARIO_UNAVAILABLE:");
    }

    @Test void targetsComeFromObservedPivotHighsAndRiskUsesUpperEntry() {
        var input = input(100, true, Map.of());
        var result = engine.analyze(input);
        var scenario = result.scenario();
        assertThat(scenario).isNotNull();
        assertThat(scenario.stopLoss()).isLessThan(scenario.entryFrom());
        assertThat(scenario.targets()).allMatch(t -> input.dailyCandles().stream()
                .anyMatch(c -> c.high().setScale(0, java.math.RoundingMode.HALF_UP).compareTo(t) == 0));
        var expected = scenario.targets().get(0).subtract(scenario.entryTo())
                .divide(scenario.entryTo().subtract(scenario.stopLoss()), 8, java.math.RoundingMode.HALF_UP);
        assertThat(factor(result, "ENTRY_REWARD_RISK").rawValue()).isEqualByComparingTo(expected);
    }

    @Test void supplyDoesNotSaturateOnSmallNetBuying() {
        var result = engine.analyze(input(100, true, Map.of("swingNetBuy20", new BigDecimal("100"),
                "swingFlowVolume20", new BigDecimal("20000"), "swingPositiveFlowDays20", new BigDecimal("10"),
                "marketReturn20", BigDecimal.ZERO, "industryReturn20", BigDecimal.ZERO)));
        assertThat(factor(result, "SUPPLY_DEMAND").rawValue()).isEqualByComparingTo("0.5");
        assertThat(factor(result, "SUPPLY_DEMAND").score()).isEqualByComparingTo("50.7");
        assertThat(factor(result, "SUPPLY_DEMAND").weight()).isEqualByComparingTo("0.09");
    }

    @Test void decisionBoundariesUseTheFinalScoreOnly() {
        assertThat(SwingSuitabilityEngine.actionFor(new BigDecimal("80"))).isEqualTo(StrategyAction.BUY_CANDIDATE);
        assertThat(SwingSuitabilityEngine.actionFor(new BigDecimal("79.9999"))).isEqualTo(StrategyAction.WATCH);
        assertThat(SwingSuitabilityEngine.actionFor(new BigDecimal("76.259"))).isEqualTo(StrategyAction.WATCH);
        assertThat(SwingSuitabilityEngine.actionFor(new BigDecimal("65"))).isEqualTo(StrategyAction.WATCH);
        assertThat(SwingSuitabilityEngine.actionFor(new BigDecimal("64.9999"))).isEqualTo(StrategyAction.WAIT);
        assertThat(SwingSuitabilityEngine.actionFor(new BigDecimal("45"))).isEqualTo(StrategyAction.WAIT);
        assertThat(SwingSuitabilityEngine.actionFor(new BigDecimal("44.9999"))).isEqualTo(StrategyAction.AVOID);
    }

    @Test void lowRiskScoreDoesNotTurnMissingScenarioIntoAvoidance() {
        var base = input(100, false, Map.of("swingNetBuy20", new BigDecimal("10000"),
                "swingFlowVolume20", new BigDecimal("20000"), "swingPositiveFlowDays20", new BigDecimal("20"),
                "marketReturn20", BigDecimal.ZERO, "industryReturn20", BigDecimal.ZERO));
        var bars = new ArrayList<>(base.dailyCandles());
        var crash = bars.get(70);
        bars.set(70, new DailyCandle(crash.businessDate(), new BigDecimal("400"), new BigDecimal("405"),
                new BigDecimal("395"), new BigDecimal("400"), crash.volume()));
        for (int i = 85; i < 100; i++) {
            var bar = bars.get(i);
            bars.set(i, new DailyCandle(bar.businessDate(), bar.open(), bar.close().multiply(new BigDecimal("1.4")),
                    bar.close().multiply(new BigDecimal("0.6")), bar.close(), bar.volume()));
        }
        var result = engine.analyze(new AnalysisInput(base.stock(), base.mode(), base.baseDate(), base.dataCutoffAt(),
                bars, base.weeklyCandles(), base.monthlyCandles(), base.observations()));
        assertThat(factor(result, "RISK_STABILITY").score()).isLessThan(new BigDecimal("35"));
        assertThat(result.totalScore()).isNull();
        assertThat(result.strategy().action()).isNotEqualTo(StrategyAction.AVOID);
        assertThat(result.strategy().strategyCode()).contains(":SCENARIO_UNAVAILABLE:");
    }

    @Test void zeroTradingVolumeDefersInsteadOfIssuingTradeAdvice() {
        var base = input(100, false, Map.of());
        var bars = base.dailyCandles().stream().map(c -> new DailyCandle(c.businessDate(), c.open(), c.high(),
                c.low(), c.close(), 0)).toList();
        var result = engine.analyze(new AnalysisInput(base.stock(), base.mode(), base.baseDate(), base.dataCutoffAt(),
                bars, base.weeklyCandles(), base.monthlyCandles(), base.observations()));
        assertThat(result.totalScore()).isNull();
        assertThat(result.strategy().strategyCode()).isEqualTo("SWING_V5:UNCLASSIFIED:DATA_MISSING:WAIT");
    }

    private FactorResult factor(AnalysisResult r, String code) {
        return r.factors().stream().filter(f -> f.factorCode().equals(code)).findFirst().orElseThrow();
    }
    private AnalysisInput input(int count, boolean oscillating, Map<String, BigDecimal> observations) {
        List<DailyCandle> candles = new ArrayList<>();
        LocalDate date = LocalDate.of(2026, 9, 19);
        for (int i = 0; i < count; i++) {
            BigDecimal close = BigDecimal.valueOf(oscillating ? 1000 + 100 * Math.sin(i * Math.PI / 10) : 1000 + i * 10);
            candles.add(new DailyCandle(date.minusDays(count - i - 1), close, close.add(BigDecimal.TEN),
                    close.subtract(BigDecimal.TEN), close, 1_000_000));
        }
        return new AnalysisInput(new StockSnapshot("000500", "테스트", "KRX", candles.get(count - 1).close()),
                AnalysisMode.SWING, date, Instant.parse("2026-09-19T00:00:00Z"), candles, candles, candles, observations);
    }
}
