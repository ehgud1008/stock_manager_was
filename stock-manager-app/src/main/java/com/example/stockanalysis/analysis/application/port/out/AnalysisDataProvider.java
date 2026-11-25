package com.example.stockanalysis.analysis.application.port.out;

import com.example.stockanalysis.analysis.application.model.CollectedAnalysisData;
import com.example.stockanalysis.engine.model.AnalysisMode;
import java.time.LocalDate;

public interface AnalysisDataProvider {

    CollectedAnalysisData collect(String stockCode, AnalysisMode mode, LocalDate baseDate);

    /** Readiness probes need only daily history. Providers without split collection retain their behavior. */
    default CollectedAnalysisData collectDaily(String stockCode, LocalDate baseDate) {
        return collect(stockCode, AnalysisMode.SWING, baseDate);
    }

    /** Enrich the verified daily sample without fetching or replacing that sample. */
    default CollectedAnalysisData completeDaily(CollectedAnalysisData daily) {
        return daily;
    }
}
