package com.example.stockanalysis.engine.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record AnalysisResult(
        String engineVersion,
        AnalysisMode mode,
        BigDecimal totalScore,
        List<FactorResult> factors,
        StrategyDecision strategy,
        TradeScenario scenario,
        List<String> warnings,
        SignalReport signalReport
) {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    public AnalysisResult(String engineVersion, AnalysisMode mode, BigDecimal totalScore, List<FactorResult> factors,
                          StrategyDecision strategy, TradeScenario scenario, List<String> warnings) {
        this(engineVersion, mode, totalScore, factors, strategy, scenario, warnings, null);
    }

    public AnalysisResult {
        if (engineVersion == null || engineVersion.isBlank()) {
            throw new IllegalArgumentException("engineVersion must not be blank");
        }
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(strategy, "strategy");
        factors = List.copyOf(Objects.requireNonNull(factors, "factors"));
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings"));
        if (totalScore != null && (totalScore.signum() < 0 || totalScore.compareTo(ONE_HUNDRED) > 0)) {
            throw new IllegalArgumentException("totalScore must be between 0 and 100");
        }
    }
}
