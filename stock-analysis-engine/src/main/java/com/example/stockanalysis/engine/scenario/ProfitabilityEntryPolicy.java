package com.example.stockanalysis.engine.scenario;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A bounded gap allowance; risk must always be measured from this upper entry. */
public final class ProfitabilityEntryPolicy {
    private ProfitabilityEntryPolicy() {}

    public static BigDecimal upperEntry(BigDecimal current, BigDecimal atr) {
        // Round down so integer-price rounding cannot exceed either buffer cap.
        return current.add(atr.multiply(new BigDecimal("0.20"))
                .min(current.multiply(new BigDecimal("0.015")))).setScale(0, RoundingMode.FLOOR);
    }
}
