package com.example.stockanalysis.engine.factor.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.AnalysisMode;
import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.engine.model.FactorResult;
import com.example.stockanalysis.engine.model.StockSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FactorDisplayPrecisionTest {

    @Test
    void roundsPercentAndMultipleResultsToOneDecimalWithoutChangingScorePrecision() {
        AnalysisInput input = input();

        assertDisplayed(new TrendFactorCalculator().calculate(input), "%입니다.");
        assertDisplayed(new MomentumFactorCalculator().calculate(input), "%입니다.");
        assertDisplayed(new VolumeFactorCalculator().calculate(input), "배입니다.");
        assertDisplayed(new SupplyDemandFactorCalculator().calculate(input), "%입니다.");
    }

    private void assertDisplayed(FactorResult result, String suffix) {
        assertThat(result.rawValue().scale()).isEqualTo(1);
        assertThat(result.reason()).contains(result.rawValue().toPlainString() + suffix);
        assertThat(result.score().scale()).isEqualTo(4);
    }

    private AnalysisInput input() {
        List<DailyCandle> candles = java.util.stream.IntStream.range(0, 20)
                .mapToObj(index -> new DailyCandle(
                        LocalDate.of(2026, 7, 20).plusDays(index),
                        value(100 + index),
                        value(102 + index),
                        value(99 + index),
                        value(101 + index),
                        1_000L + index * 37L
                ))
                .toList();
        return new AnalysisInput(
                new StockSnapshot("005930", "삼성전자", "KOSPI", value(120)),
                AnalysisMode.SWING,
                LocalDate.of(2026, 8, 8),
                Instant.parse("2026-08-08T07:00:00Z"),
                candles,
                Map.of(
                        SupplyDemandFactorCalculator.FOREIGN_NET_BUY, value(37),
                        SupplyDemandFactorCalculator.INSTITUTION_NET_BUY, value(19),
                        SupplyDemandFactorCalculator.AVERAGE_VOLUME, value(913)
                )
        );
    }

    private BigDecimal value(long value) {
        return BigDecimal.valueOf(value);
    }
}
