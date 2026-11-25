package com.example.stockanalysis.engine.screener;

import java.math.BigDecimal;
import static com.example.stockanalysis.engine.screener.ScreenerTypes.Direction;
import static com.example.stockanalysis.engine.screener.EmaCalculator.PRECISION;

@FunctionalInterface
public interface MovingAverageSlopeCalculator {
    /** Total percent change over the configured number of bars, NOT percent per bar. */
    BigDecimal calculate(BigDecimal previous, BigDecimal current);

    static MovingAverageSlopeCalculator percentChange() {
        return (previous, current) -> previous == null || current == null || previous.signum() <= 0 ? null
                : current.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous, PRECISION);
    }
    static Direction direction(BigDecimal value, ScreenerConfig config) {
        if (value == null) return null;
        if (value.abs().compareTo(config.flatSlopePercent()) <= 0) return Direction.FLAT;
        if (value.abs().compareTo(config.strongSlopePercent()) >= 0) return value.signum() > 0 ? Direction.STRONG_UP : Direction.STRONG_DOWN;
        return value.signum() > 0 ? Direction.UP : Direction.DOWN;
    }
}
