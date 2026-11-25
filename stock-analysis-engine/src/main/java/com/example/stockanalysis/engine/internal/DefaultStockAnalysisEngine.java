package com.example.stockanalysis.engine.internal;

import com.example.stockanalysis.engine.api.StockAnalysisEngine;
import com.example.stockanalysis.engine.factor.FactorCalculator;
import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.AnalysisResult;
import com.example.stockanalysis.engine.model.FactorResult;
import com.example.stockanalysis.engine.model.StrategyDecision;
import com.example.stockanalysis.engine.model.TradeScenario;
import com.example.stockanalysis.engine.scenario.ScenarioPolicy;
import com.example.stockanalysis.engine.scoring.ScorePolicy;
import com.example.stockanalysis.engine.strategy.StrategyPolicy;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class DefaultStockAnalysisEngine implements StockAnalysisEngine {

    private final String engineVersion;
    private final List<FactorCalculator> factorCalculators;
    private final ScorePolicy scorePolicy;
    private final StrategyPolicy strategyPolicy;
    private final ScenarioPolicy scenarioPolicy;

    public DefaultStockAnalysisEngine(
            String engineVersion,
            List<FactorCalculator> factorCalculators,
            ScorePolicy scorePolicy,
            StrategyPolicy strategyPolicy,
            ScenarioPolicy scenarioPolicy
    ) {
        if (engineVersion == null || engineVersion.isBlank()) {
            throw new IllegalArgumentException("engineVersion must not be blank");
        }
        this.engineVersion = engineVersion;
        this.factorCalculators = List.copyOf(
                Objects.requireNonNull(factorCalculators, "factorCalculators")
        );
        if (this.factorCalculators.isEmpty()) {
            throw new IllegalArgumentException("factorCalculators must not be empty");
        }
        this.scorePolicy = Objects.requireNonNull(scorePolicy, "scorePolicy");
        this.strategyPolicy = Objects.requireNonNull(strategyPolicy, "strategyPolicy");
        this.scenarioPolicy = Objects.requireNonNull(scenarioPolicy, "scenarioPolicy");
    }

    @Override
    public AnalysisResult analyze(AnalysisInput input) {
        Objects.requireNonNull(input, "input");
        List<FactorResult> factors = factorCalculators.stream()
                .map(calculator -> calculateFactor(calculator, input))
                .toList();
        ensureUniqueFactorCodes(factors);

        BigDecimal totalScore = scorePolicy.calculate(factors);
        StrategyDecision strategy = strategyPolicy.decide(input, totalScore, factors);
        TradeScenario scenario = scenarioPolicy.calculate(
                input,
                totalScore,
                strategy,
                factors
        );

        return new AnalysisResult(
                engineVersion,
                input.mode(),
                totalScore,
                factors,
                strategy,
                scenario,
                List.of()
        );
    }

    private FactorResult calculateFactor(FactorCalculator calculator, AnalysisInput input) {
        Objects.requireNonNull(calculator, "factorCalculator");
        FactorResult result = Objects.requireNonNull(
                calculator.calculate(input),
                "factorCalculator result"
        );
        if (!calculator.code().equals(result.factorCode())) {
            throw new IllegalStateException(
                    "factor code mismatch: calculator=" + calculator.code()
                            + ", result=" + result.factorCode()
            );
        }
        return result;
    }

    private void ensureUniqueFactorCodes(List<FactorResult> factors) {
        Set<String> codes = new HashSet<>();
        for (FactorResult factor : factors) {
            if (!codes.add(factor.factorCode())) {
                throw new IllegalStateException("duplicate factor code: " + factor.factorCode());
            }
        }
    }
}
