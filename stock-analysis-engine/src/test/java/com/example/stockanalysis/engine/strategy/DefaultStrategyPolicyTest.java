package com.example.stockanalysis.engine.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.AnalysisMode;
import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.engine.model.FactorResult;
import com.example.stockanalysis.engine.model.StockSnapshot;
import com.example.stockanalysis.engine.model.StrategyAction;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultStrategyPolicyTest {

    private final DefaultStrategyPolicy policy = new DefaultStrategyPolicy();

    @Test
    void waitsForDailyPullbackWhenWeeklyTrendRemainsStrong() {
        var decision = policy.decide(
                input(),
                amount("70"),
                List.of(
                        factor("DAILY_TECHNICAL", "55"),
                        factor("WEEKLY_TECHNICAL", "75"),
                        factor("MONTHLY_TECHNICAL", "70")
                )
        );

        assertThat(decision.action()).isEqualTo(StrategyAction.WAIT);
        assertThat(decision.strategyCode()).isEqualTo("SWING_PULLBACK_WAIT");
        assertThat(decision.strategyName()).isEqualTo("스윙 눌림목 대기");
    }

    @Test
    void avoidsEntryWhenMonthlyTrendIsWeakEvenWithHighTotalScore() {
        var decision = policy.decide(
                input(),
                amount("85"),
                List.of(
                        factor("DAILY_TECHNICAL", "55"),
                        factor("WEEKLY_TECHNICAL", "75"),
                        factor("MONTHLY_TECHNICAL", "39")
                )
        );

        assertThat(decision.action()).isEqualTo(StrategyAction.AVOID);
        assertThat(decision.strategyCode()).isEqualTo("SWING_AVOID");
        assertThat(decision.strategyName()).isEqualTo("스윙 회피");
    }

    @Test
    void avoidsEntryWhenRiskStabilityIsTooLow() {
        var decision = policy.decide(
                input(),
                amount("85"),
                List.of(
                        factor("TREND", "80"),
                        factor("MOMENTUM", "75"),
                        factor("RISK_STABILITY", "34"),
                        factor("MONTHLY_TECHNICAL", "70")
                )
        );

        assertThat(decision.action()).isEqualTo(StrategyAction.AVOID);
        assertThat(decision.reasons().get(0)).isEqualTo("RISK_STABILITY");
    }

    @Test
    void doesNotCreateBuyCandidateFromHighTotalWhenPriceStructureIsWeak() {
        var decision = policy.decide(
                input(),
                amount("80"),
                List.of(
                        factor("TREND", "50"),
                        factor("MOMENTUM", "80"),
                        factor("RISK_STABILITY", "70"),
                        factor("MONTHLY_TECHNICAL", "70")
                )
        );

        assertThat(decision.action()).isEqualTo(StrategyAction.WATCH);
    }

    private AnalysisInput input() {
        LocalDate baseDate = LocalDate.of(2026, 8, 28);
        BigDecimal price = amount("70000");
        return new AnalysisInput(
                new StockSnapshot("005930", "삼성전자", "KRX", price),
                AnalysisMode.SWING,
                baseDate,
                Instant.parse("2026-08-28T07:00:00Z"),
                List.of(new DailyCandle(
                        baseDate, price, price, price, price, 1_000L
                )),
                Map.of()
        );
    }

    private FactorResult factor(String code, String score) {
        return new FactorResult(code, BigDecimal.ZERO, amount(score), amount("0.1"), code);
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
