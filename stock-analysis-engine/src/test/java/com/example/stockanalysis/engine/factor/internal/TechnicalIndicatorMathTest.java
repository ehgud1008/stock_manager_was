package com.example.stockanalysis.engine.factor.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TechnicalIndicatorMathTest {

    @Test
    void calculatesRsiAndMacdForAnUptrend() {
        List<BigDecimal> closes = sequence(1, 100);

        assertThat(TechnicalIndicatorMath.rsi(closes, 14))
                .isEqualByComparingTo("100");
        TechnicalIndicatorMath.Macd macd = TechnicalIndicatorMath.macd(closes);
        assertThat(macd).isNotNull();
        assertThat(macd.macd()).isPositive();
        assertThat(macd.histogram().abs()).isLessThan(new BigDecimal("0.0001"));
    }

    @Test
    void detectsRecentGoldenAndDeadCrosses() {
        assertThat(TechnicalIndicatorMath.recentCross(
                values("10", "10", "10", "10", "10", "10", "10", "10", "20"),
                3,
                5,
                5
        )).isEqualTo(TechnicalIndicatorMath.CrossSignal.GOLDEN);

        assertThat(TechnicalIndicatorMath.recentCross(
                values("20", "20", "20", "20", "20", "20", "20", "20", "10"),
                3,
                5,
                5
        )).isEqualTo(TechnicalIndicatorMath.CrossSignal.DEAD);
    }

    private List<BigDecimal> sequence(int from, int to) {
        List<BigDecimal> values = new ArrayList<>();
        for (int value = from; value <= to; value++) {
            values.add(BigDecimal.valueOf(value));
        }
        return values;
    }

    private List<BigDecimal> values(String... values) {
        return java.util.Arrays.stream(values).map(BigDecimal::new).toList();
    }
}
