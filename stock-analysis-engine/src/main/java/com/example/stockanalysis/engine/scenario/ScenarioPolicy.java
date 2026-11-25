package com.example.stockanalysis.engine.scenario;

import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.FactorResult;
import com.example.stockanalysis.engine.model.StrategyDecision;
import com.example.stockanalysis.engine.model.TradeScenario;
import java.math.BigDecimal;
import java.util.List;

public interface ScenarioPolicy {

    TradeScenario calculate(
            AnalysisInput input,
            BigDecimal totalScore,
            StrategyDecision strategy,
            List<FactorResult> factors
    );
}
