package com.example.stockanalysis.engine.screener;

import java.math.BigDecimal;
import static com.example.stockanalysis.engine.screener.EmaCalculator.PRECISION;
import static com.example.stockanalysis.engine.screener.ScreenerTypes.*;

public final class StageAnalyzer {
    public record Stage(Integer stage, Quality quality) {}

    public Stage analyze(BigDecimal shortEma, BigDecimal middleEma, BigDecimal longEma, BigDecimal tolerancePercent) {
        if (shortEma == null || middleEma == null || longEma == null) return new Stage(null, Quality.INSUFFICIENT_DATA);
        if (shortEma.signum() <= 0 || middleEma.signum() <= 0 || longEma.signum() <= 0 || tolerancePercent.signum() < 0) {
            throw new IllegalArgumentException("positive EMA values and nonnegative tolerance required");
        }
        if (near(shortEma, middleEma, tolerancePercent) || near(shortEma, longEma, tolerancePercent)
                || near(middleEma, longEma, tolerancePercent)) return new Stage(null, Quality.BOUNDARY);
        int stage;
        if (shortEma.compareTo(middleEma) > 0) {
            stage = middleEma.compareTo(longEma) > 0 ? 1 : shortEma.compareTo(longEma) > 0 ? 6 : 5;
        } else {
            stage = shortEma.compareTo(longEma) > 0 ? 2 : middleEma.compareTo(longEma) > 0 ? 3 : 4;
        }
        return new Stage(stage, Quality.VALID);
    }
    private boolean near(BigDecimal a, BigDecimal b, BigDecimal tolerance) {
        return a.subtract(b).abs().multiply(BigDecimal.valueOf(100)).divide(a.max(b), PRECISION).compareTo(tolerance) <= 0;
    }
    public Transition transition(Integer previous, Integer current) {
        if (previous == null || current == null) return Transition.UNKNOWN;
        if (previous.equals(current)) return Transition.SAME;
        if (current == previous % 6 + 1) return Transition.FORWARD;
        if (previous == current % 6 + 1) return Transition.REVERSE;
        return Transition.SKIPPED;
    }
}
