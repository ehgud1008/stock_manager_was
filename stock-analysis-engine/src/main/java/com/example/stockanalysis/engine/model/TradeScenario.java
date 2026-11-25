package com.example.stockanalysis.engine.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record TradeScenario(
        BigDecimal entryFrom,
        BigDecimal entryTo,
        List<BigDecimal> targets,
        BigDecimal stopLoss,
        TargetBasis targetBasis
) {
    public enum TargetBasis { OBSERVED_RESISTANCE, STRUCTURAL_RISK_MULTIPLE }

    /** Historical scenarios did not record the target basis. Do not infer it from their prices. */
    public TradeScenario(BigDecimal entryFrom, BigDecimal entryTo, List<BigDecimal> targets, BigDecimal stopLoss) {
        this(entryFrom,entryTo,targets,stopLoss,null);
    }

    public TradeScenario {
        requirePositive(entryFrom, "entryFrom");
        requirePositive(entryTo, "entryTo");
        requirePositive(stopLoss, "stopLoss");
        targets = List.copyOf(Objects.requireNonNull(targets, "targets"));
        if (entryFrom.compareTo(entryTo) > 0) {
            throw new IllegalArgumentException("entryFrom must not exceed entryTo");
        }
        if (stopLoss.compareTo(entryFrom) >= 0) {
            throw new IllegalArgumentException("stopLoss must be below entryFrom");
        }
        if (targets.isEmpty() || targets.stream().anyMatch(target -> target == null
                || target.signum() <= 0 || target.compareTo(entryTo) <= 0)) {
            throw new IllegalArgumentException("targets must be above entryTo");
        }
    }

    private static void requirePositive(BigDecimal value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }
}
