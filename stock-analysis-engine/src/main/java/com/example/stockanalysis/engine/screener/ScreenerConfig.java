package com.example.stockanalysis.engine.screener;

import java.math.BigDecimal;

/** Structural thresholds, not calibrated trading rules. Precision is DECIMAL128 throughout. */
public record ScreenerConfig(int shortPeriod, int middlePeriod, int longPeriod,
                             int warmupBars, int historyBars, int slopeLookback, int gapLookback,
                             BigDecimal boundaryPercent, BigDecimal flatSlopePercent,
                             BigDecimal strongSlopePercent, BigDecimal gapFlatPercent,
                             String version) {
    public ScreenerConfig {
        if (shortPeriod < 1 || middlePeriod <= shortPeriod || longPeriod <= middlePeriod
                || warmupBars < longPeriod || historyBars < 2 || slopeLookback < 1 || gapLookback < 1
                || warmupBars > 10000 || historyBars > 2000 || slopeLookback > 1000 || gapLookback > 1000
                || boundaryPercent == null || flatSlopePercent == null || strongSlopePercent == null || gapFlatPercent == null
                || boundaryPercent.signum() < 0 || flatSlopePercent.signum() < 0 || gapFlatPercent.signum() < 0
                || strongSlopePercent.compareTo(flatSlopePercent) <= 0 || version == null || version.isBlank()) {
            throw new IllegalArgumentException("invalid screener configuration");
        }
    }
    public static ScreenerConfig defaults() {
        return new ScreenerConfig(5, 20, 40, 200, 60, 3, 3,
                new BigDecimal("0.000001"), new BigDecimal("0.01"), new BigDecimal("0.5"),
                new BigDecimal("0.01"), "ema-cycle-v1");
    }
    public int requestedBars() { return warmupBars + historyBars + Math.max(slopeLookback, gapLookback); }
}
