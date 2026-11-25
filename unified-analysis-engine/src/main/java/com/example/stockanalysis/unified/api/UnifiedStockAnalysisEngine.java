package com.example.stockanalysis.unified.api;

import com.example.stockanalysis.unified.model.UnifiedAnalysisInput;
import com.example.stockanalysis.unified.model.UnifiedAnalysisResult;

/** Pure calculation over one supplied daily snapshot; performs no collection or persistence. */
public interface UnifiedStockAnalysisEngine {
    UnifiedAnalysisResult analyze(UnifiedAnalysisInput input);
}
