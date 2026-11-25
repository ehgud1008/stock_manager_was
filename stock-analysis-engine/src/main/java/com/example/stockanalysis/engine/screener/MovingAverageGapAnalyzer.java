package com.example.stockanalysis.engine.screener;

import java.math.BigDecimal;
import static com.example.stockanalysis.engine.screener.ScreenerTypes.GapState;
import static com.example.stockanalysis.engine.screener.EmaCalculator.PRECISION;

public final class MovingAverageGapAnalyzer {
    public record Gap(BigDecimal signedPercent, BigDecimal distancePercent, GapState state) {}
    public BigDecimal percent(BigDecimal a, BigDecimal b) {
        return a == null || b == null || b.signum() <= 0 ? null : a.subtract(b).multiply(BigDecimal.valueOf(100)).divide(b, PRECISION);
    }
    public Gap analyze(BigDecimal current, BigDecimal previous, BigDecimal flatThreshold, boolean crossedInWindow) {
        if (current == null) return new Gap(null, null, GapState.UNKNOWN);
        if (previous == null) return new Gap(current, current.abs(), GapState.UNKNOWN);
        if (crossedInWindow || current.signum() != previous.signum()) return new Gap(current, current.abs(), GapState.CROSSING);
        BigDecimal delta = current.abs().subtract(previous.abs());
        GapState state = delta.abs().compareTo(flatThreshold) <= 0 ? GapState.FLAT : delta.signum() > 0 ? GapState.EXPANDING : GapState.CONTRACTING;
        return new Gap(current, current.abs(), state);
    }
}
