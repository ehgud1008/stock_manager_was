package com.example.stockanalysis.engine.factor.internal;

import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.AnalysisMode;
import com.example.stockanalysis.engine.model.DailyCandle;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

final class FactorMath {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private FactorMath() {
    }

    static List<DailyCandle> orderedCandles(AnalysisInput input) {
        return orderedCandles(input.dailyCandles());
    }

    static List<DailyCandle> orderedCandles(List<DailyCandle> candles) {
        return candles.stream()
                .sorted(Comparator.comparing(DailyCandle::businessDate))
                .toList();
    }

    static BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return ZERO;
        }
        return values.stream()
                .reduce(ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 8, RoundingMode.HALF_UP);
    }

    static BigDecimal percentChange(BigDecimal current, BigDecimal base) {
        if (base == null || base.signum() == 0) {
            return ZERO;
        }
        return current.subtract(base)
                .multiply(ONE_HUNDRED)
                .divide(base.abs(), 8, RoundingMode.HALF_UP);
    }

    static BigDecimal clampScore(BigDecimal score) {
        return score.max(ZERO).min(ONE_HUNDRED).setScale(4, RoundingMode.HALF_UP);
    }

    static BigDecimal displayValue(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP);
    }

    static BigDecimal weight(AnalysisInput input, String swing, String shortTerm) {
        return new BigDecimal(input.mode() == AnalysisMode.SWING ? swing : shortTerm);
    }
}
