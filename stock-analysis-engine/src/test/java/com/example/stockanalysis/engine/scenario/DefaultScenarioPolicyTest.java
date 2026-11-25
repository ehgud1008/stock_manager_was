package com.example.stockanalysis.engine.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.AnalysisMode;
import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.engine.model.StockSnapshot;
import com.example.stockanalysis.engine.model.StrategyAction;
import com.example.stockanalysis.engine.model.StrategyDecision;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultScenarioPolicyTest {

    @Test
    void calculatesStructuralStopAndRiskMultipleTargets() {
        LocalDate baseDate = LocalDate.of(2026, 9, 3);
        List<DailyCandle> candles = new ArrayList<>();
        for (int index = 0; index < 30; index++) {
            BigDecimal close = BigDecimal.valueOf(100 + index);
            candles.add(new DailyCandle(
                    baseDate.minusDays(29L - index),
                    close,
                    close.add(BigDecimal.valueOf(2)),
                    close.subtract(BigDecimal.valueOf(2)),
                    close,
                    1_000L
            ));
        }
        AnalysisInput input = new AnalysisInput(
                new StockSnapshot("005930", "삼성전자", "KOSPI", amount("129")),
                AnalysisMode.SWING,
                baseDate,
                Instant.parse("2026-09-03T07:00:00Z"),
                candles,
                Map.of()
        );
        var strategy = new StrategyDecision(
                "SWING_BUY_CANDIDATE",
                "스윙 매수 후보",
                StrategyAction.BUY_CANDIDATE,
                List.of()
        );

        var scenario = new DefaultScenarioPolicy().calculate(
                input, amount("80"), strategy, List.of()
        );

        assertThat(scenario.stopLoss()).isLessThan(scenario.entryFrom());
        assertThat(scenario.targets()).allSatisfy(
                target -> assertThat(target).isGreaterThan(scenario.entryTo())
        );
        BigDecimal risk = scenario.entryTo().subtract(scenario.stopLoss());
        assertThat(scenario.targets().get(0).subtract(scenario.entryTo()))
                .isGreaterThanOrEqualTo(risk.multiply(new BigDecimal("1.5"))
                        .subtract(BigDecimal.ONE));
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
