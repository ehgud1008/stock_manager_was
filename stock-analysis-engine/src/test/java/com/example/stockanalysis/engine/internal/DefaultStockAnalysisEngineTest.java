package com.example.stockanalysis.engine.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.stockanalysis.engine.factor.FactorCalculator;
import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.AnalysisMode;
import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.engine.model.FactorResult;
import com.example.stockanalysis.engine.model.StockSnapshot;
import com.example.stockanalysis.engine.model.StrategyAction;
import com.example.stockanalysis.engine.model.StrategyDecision;
import com.example.stockanalysis.engine.model.TradeScenario;
import com.example.stockanalysis.engine.scoring.WeightedAverageScorePolicy;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultStockAnalysisEngineTest {

    @Test
    void executesFactorScoreStrategyAndScenarioPoliciesInOrder() {
        var engine = new DefaultStockAnalysisEngine(
                "analysis-v1",
                List.of(factor("TREND", "80", "0.6"), factor("FLOW", "50", "0.4")),
                new WeightedAverageScorePolicy(),
                (input, score, factors) -> new StrategyDecision(
                        "SWING_PULLBACK",
                        "스윙 눌림목",
                        StrategyAction.WATCH,
                        List.of("추세 점수 우세")
                ),
                (input, score, strategy, factors) -> new TradeScenario(
                        amount("69000"),
                        amount("70000"),
                        List.of(amount("75000")),
                        amount("67000")
                )
        );

        var result = engine.analyze(input());

        assertThat(result.engineVersion()).isEqualTo("analysis-v1");
        assertThat(result.mode()).isEqualTo(AnalysisMode.SWING);
        assertThat(result.totalScore()).isEqualByComparingTo("68.0000");
        assertThat(result.factors())
                .extracting(FactorResult::factorCode)
                .containsExactly("TREND", "FLOW");
        assertThat(result.strategy().action()).isEqualTo(StrategyAction.WATCH);
        assertThat(result.scenario().stopLoss()).isEqualByComparingTo("67000");
    }

    @Test
    void rejectsDuplicateFactorCodes() {
        var engine = new DefaultStockAnalysisEngine(
                "analysis-v1",
                List.of(factor("TREND", "80", "0.6"), factor("TREND", "50", "0.4")),
                new WeightedAverageScorePolicy(),
                (input, score, factors) -> new StrategyDecision(
                        "WAIT", "대기", StrategyAction.WAIT, List.of()
                ),
                (input, score, strategy, factors) -> new TradeScenario(
                        amount("69000"), amount("70000"),
                        List.of(amount("75000")), amount("67000")
                )
        );

        assertThatThrownBy(() -> engine.analyze(input()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("duplicate factor code: TREND");
    }

    private FactorCalculator factor(String code, String score, String weight) {
        return new FactorCalculator() {
            @Override
            public String code() {
                return code;
            }

            @Override
            public FactorResult calculate(AnalysisInput input) {
                return new FactorResult(
                        code,
                        amount(score),
                        amount(score),
                        amount(weight),
                        code + " factor"
                );
            }
        };
    }

    private AnalysisInput input() {
        return new AnalysisInput(
                new StockSnapshot("005930", "삼성전자", "KOSPI", amount("71000")),
                AnalysisMode.SWING,
                LocalDate.of(2026, 8, 14),
                Instant.parse("2026-08-14T07:00:00Z"),
                List.of(new DailyCandle(
                        LocalDate.of(2026, 8, 14),
                        amount("70000"),
                        amount("72000"),
                        amount("69500"),
                        amount("71000"),
                        10_000_000L
                )),
                Map.of("foreignNetBuy", amount("120000"))
        );
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
