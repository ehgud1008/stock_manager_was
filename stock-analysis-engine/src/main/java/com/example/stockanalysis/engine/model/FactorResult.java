package com.example.stockanalysis.engine.model;

import java.math.BigDecimal;
import java.util.Objects;

public record FactorResult(
        String factorCode,
        BigDecimal rawValue,
        BigDecimal score,
        BigDecimal weight,
        String reason
) {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    public FactorResult {
        factorCode = requireText(factorCode, "factorCode");
        Objects.requireNonNull(weight, "weight");
        reason = requireText(reason, "reason");
        if (score != null && (score.signum() < 0 || score.compareTo(ONE_HUNDRED) > 0)) {
            throw new IllegalArgumentException("score must be between 0 and 100");
        }
        if (weight.signum() < 0 || (score == null && weight.signum() != 0)) {
            throw new IllegalArgumentException("weight must be nonnegative and zero for unavailable factors");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
