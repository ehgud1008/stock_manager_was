package com.example.stockanalysis.engine.api;

import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.AnalysisResult;

public interface StockAnalysisEngine {

    AnalysisResult analyze(AnalysisInput input);
}
