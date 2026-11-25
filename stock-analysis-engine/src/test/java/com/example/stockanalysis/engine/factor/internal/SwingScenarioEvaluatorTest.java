package com.example.stockanalysis.engine.factor.internal;

import com.example.stockanalysis.engine.model.DailyCandle;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SwingScenarioEvaluatorTest {
    private final SwingScenarioEvaluator evaluator = new SwingScenarioEvaluator();

    @Test void classifiesFourSetupsWithoutChangingTheObservedTargetsToFitRewardRisk() {
        var bars = bars();
        assertThat(evaluator.evaluate(bars, n(111), n(80)).type()).isEqualTo("BREAKOUT");
        assertThat(evaluator.evaluate(bars, n(100), n(80)).type()).isEqualTo("PULLBACK");
        assertThat(evaluator.evaluate(bars, n(108), n(80)).type()).isEqualTo("CONTINUATION");
        assertThat(evaluator.evaluate(bars, n(100), n(40)).type()).isEqualTo("RANGE");
        var result = evaluator.evaluate(bars, n(100), n(80));
        assertThat(result.scenario()).isNotNull();
        assertThat(result.scenario().targets()).allMatch(t -> bars.stream().anyMatch(b -> b.high().compareTo(t) == 0));
    }

    @Test void breakoutUsesBrokenResistanceAsInvalidationAndOlderObservedResistanceAsTarget() {
        var bars = bars();
        // A completed older high above the recent 20-bar range supplies an observed target.
        var b = bars.get(30);
        bars.set(30, new DailyCandle(b.businessDate(), n(100), n(130), n(99), n(100), 1000));
        var result = evaluator.evaluate(bars, n(111), n(80));
        assertThat(result.type()).isEqualTo("BREAKOUT");
        assertThat(result.scenario()).isNotNull();
        assertThat(result.scenario().targets()).containsExactly(n(130));
        assertThat(result.scenario().targetBasis()).isEqualTo(com.example.stockanalysis.engine.model.TradeScenario.TargetBasis.OBSERVED_RESISTANCE);
        BigDecimal oldHigh = bars.subList(79, 99).stream().map(DailyCandle::high).max(BigDecimal::compareTo).orElseThrow();
        BigDecimal stop = oldHigh.subtract(RiskStabilityFactorCalculator.averageTrueRange(bars, 14)
                .multiply(new BigDecimal("0.5"))).setScale(0, java.math.RoundingMode.HALF_UP);
        assertThat(result.scenario().stopLoss()).isEqualByComparingTo(stop);
    }

    @Test void newHighWithNoObservedTargetRemainsUnavailable() {
        var result = evaluator.evaluate(bars(), n(150), n(90));
        assertThat(result.type()).isEqualTo("BREAKOUT");
        assertThat(result.scenario()).isNull();
        assertThat(result.reason()).contains("상단 목표 저항 없음");
    }
    @Test void full252BarNewHighUsesExplicitFixedRiskTargetsWhileLegacyRemainsUnavailable() {
        var bars=new ArrayList<DailyCandle>();
        for(int i=0;i<253;i++) {
            var close=n(10000+i*10);
            bars.add(new DailyCandle(LocalDate.of(2026,1,1).plusDays(i),close,close.add(n(5)),close.subtract(n(5)),close,2000));
        }
        var current=bars.get(252).close();
        var result=evaluator.evaluate(bars,current,n(60));
        assertThat(result.syntheticTarget()).isTrue();
        assertThat(result.scenario().targetBasis()).isEqualTo(com.example.stockanalysis.engine.model.TradeScenario.TargetBasis.STRUCTURAL_RISK_MULTIPLE);
        assertThat(result.reason()).contains("가정 목표","관측 저항·가격 예측 아님");
        var scenario=result.scenario();
        var atr=RiskStabilityFactorCalculator.averageTrueRange(bars,14);
        var risk=scenario.entryTo().subtract(scenario.stopLoss()).max(atr);
        assertThat(scenario.targets().get(0)).isEqualByComparingTo(scenario.entryTo().add(risk.multiply(new BigDecimal("2.0"))).setScale(0,java.math.RoundingMode.HALF_UP));
        assertThat(scenario.targets().get(1)).isEqualByComparingTo(scenario.entryTo().add(risk.multiply(new BigDecimal("3.5"))).setScale(0,java.math.RoundingMode.HALF_UP));
        assertThat(new SwingScenarioEvaluator(false).evaluate(bars,current,n(90)).scenario()).isNull();
        assertThat(evaluator.evaluate(bars,current,new BigDecimal("59.9999")).scenario()).isNull();
        assertThat(evaluator.evaluate(bars,current,n(90),false).scenario()).isNull();
        assertThat(evaluator.evaluate(bars.subList(1,253),current,n(90)).scenario()).isNull();
        var old=bars.get(10);bars.set(10,new DailyCandle(old.businessDate(),old.open(),n(13000),old.low(),old.close(),old.volume()));
        assertThat(evaluator.evaluate(bars,current,n(90)).syntheticTarget()).isFalse();
    }
    @Test void upperEntryCapsBufferWithoutRoundingPastTheLimit() {
        assertThat(com.example.stockanalysis.engine.scenario.ProfitabilityEntryPolicy.upperEntry(n(10000),n(100))).isEqualByComparingTo("10020");
        assertThat(com.example.stockanalysis.engine.scenario.ProfitabilityEntryPolicy.upperEntry(n(10000),n(1000))).isEqualByComparingTo("10150");
        assertThat(com.example.stockanalysis.engine.scenario.ProfitabilityEntryPolicy.upperEntry(n(101),n(100))).isEqualByComparingTo("102");
        var result=evaluator.evaluate(bars(),n(100),n(80));
        assertThat(result.scenario().entryTo()).isGreaterThanOrEqualTo(n(100));
        assertThat(result.scenario().targets()).allMatch(t->t.compareTo(result.scenario().entryTo())>0);
    }
    @Test void bufferCannotSkipNearbyResistanceToClaimAFartherTarget() {
        var scaled=bars().stream().map(b->new DailyCandle(b.businessDate(),b.open().multiply(n(100)),b.high().multiply(n(100)),b.low().multiply(n(100)),b.close().multiply(n(100)),b.volume())).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        // An intact overhead pivot just above current, with higher observed pivots farther away.
        var old=scaled.get(70);
        scaled.set(70,new DailyCandle(old.businessDate(),old.open(),n(10001),old.low(),old.close(),old.volume()));
        // This explicit flat history keeps both overhead resistances intact.
        for(int i=0;i<scaled.size();i++) {
            var b=scaled.get(i);
            scaled.set(i,new DailyCandle(b.businessDate(),n(10000),n(i==30?11000:i==70?10001:10000),n(9900),n(10000),1000));
        }
        var result=evaluator.evaluate(scaled,n(10000),n(80));
        assertThat(result.scenario()).isNull();
        assertThat(result.reason()).contains("첫 목표 저항");
    }

    @Test void rewardRiskAndStopDistanceRemainIndependentAndMonotonic() {
        assertThat(SwingSuitabilityEngine.rewardRiskScore(new BigDecimal("0.5"))).isEqualByComparingTo("20");
        assertThat(SwingSuitabilityEngine.rewardRiskScore(new BigDecimal("1"))).isEqualByComparingTo("40");
        assertThat(SwingSuitabilityEngine.rewardRiskScore(new BigDecimal("2.5"))).isEqualByComparingTo("100");
        assertThat(SwingSuitabilityEngine.stopDistanceScore(new BigDecimal("1.5"))).isEqualByComparingTo("100");
        assertThat(SwingSuitabilityEngine.stopDistanceScore(new BigDecimal("4"))).isEqualByComparingTo("50");
        assertThat(SwingSuitabilityEngine.stopDistanceScore(new BigDecimal("6.5"))).isZero();
        // Even a zero stop-distance score does not erase a valid reward/risk measurement.
        assertThat(SwingSuitabilityEngine.rewardRiskScore(new BigDecimal("0.5"))
                .multiply(new BigDecimal(".14"))).isEqualByComparingTo("2.8");
    }

    private List<DailyCandle> bars() {
        List<DailyCandle> result = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            BigDecimal close = BigDecimal.valueOf(Math.round(100 + 8 * Math.sin(i * Math.PI / 10)));
            result.add(new DailyCandle(LocalDate.of(2026, 9, 19).minusDays(99 - i), close,
                    close.add(n(1)), close.subtract(n(1)), close, 1000));
        }
        return result;
    }
    private BigDecimal n(long value) { return BigDecimal.valueOf(value); }
}
