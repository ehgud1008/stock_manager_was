package com.example.stockanalysis.engine.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record DailyCandle(
        LocalDate businessDate,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        long volume
) {

    public DailyCandle {
        Objects.requireNonNull(businessDate, "businessDate");
        requirePositive(open, "open");
        requirePositive(high, "high");
        requirePositive(low, "low");
        requirePositive(close, "close");
        if (volume < 0) {
            throw new IllegalArgumentException("volume must not be negative");
        }
        if (high.compareTo(open) < 0 || high.compareTo(close) < 0
                || low.compareTo(open) > 0 || low.compareTo(close) > 0
                || high.compareTo(low) < 0) {
            throw new IllegalArgumentException("invalid OHLC relationship");
        }
    }

    private static void requirePositive(BigDecimal value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }
}
