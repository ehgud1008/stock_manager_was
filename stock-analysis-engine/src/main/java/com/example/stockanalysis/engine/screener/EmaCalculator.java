package com.example.stockanalysis.engine.screener;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EmaCalculator {
    public static final MathContext PRECISION = MathContext.DECIMAL128;
    /** First seed = SMA of the first period closes. No display rounding. */
    public List<BigDecimal> calculate(List<BigDecimal> closes, int period) {
        if (period < 1 || closes.stream().anyMatch(v -> v == null || v.signum() <= 0)) {
            throw new IllegalArgumentException("positive period and closes required");
        }
        List<BigDecimal> result = new ArrayList<>(Collections.nCopies(closes.size(), null));
        if (closes.size() < period) return Collections.unmodifiableList(result);
        BigDecimal value = closes.subList(0, period).stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(period), PRECISION);
        BigDecimal alpha = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(period + 1L), PRECISION);
        result.set(period - 1, value);
        for (int i = period; i < closes.size(); i++) {
            value = value.add(closes.get(i).subtract(value, PRECISION).multiply(alpha, PRECISION), PRECISION);
            result.set(i, value);
        }
        return Collections.unmodifiableList(result);
    }
}
