package com.example.stockanalysis.engine.strategy;

import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.FactorResult;
import com.example.stockanalysis.engine.model.StrategyDecision;
import java.math.BigDecimal;
import java.util.List;

public interface StrategyPolicy {

    StrategyDecision decide(
            AnalysisInput input,
            BigDecimal totalScore,
            List<FactorResult> factors
    );
}
