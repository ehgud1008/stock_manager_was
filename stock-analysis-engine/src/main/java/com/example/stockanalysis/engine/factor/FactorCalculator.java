package com.example.stockanalysis.engine.factor;

import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.FactorResult;

public interface FactorCalculator {

    String code();

    FactorResult calculate(AnalysisInput input);
}
