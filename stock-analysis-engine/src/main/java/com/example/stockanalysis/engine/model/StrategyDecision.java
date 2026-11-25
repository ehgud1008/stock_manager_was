package com.example.stockanalysis.engine.model;

import java.util.List;
import java.util.Objects;

public record StrategyDecision(
        String strategyCode,
        String strategyName,
        StrategyAction action,
        List<String> reasons
) {

    public StrategyDecision {
        strategyCode = requireText(strategyCode, "strategyCode");
        strategyName = requireText(strategyName, "strategyName");
        Objects.requireNonNull(action, "action");
        reasons = List.copyOf(Objects.requireNonNull(reasons, "reasons"));
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
