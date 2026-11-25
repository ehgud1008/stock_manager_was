package com.example.stockanalysis.analysis.application.model;

import com.example.stockanalysis.engine.model.AnalysisMode;
import java.time.Instant;
import java.time.LocalDate;

public record NewAnalysisRun(
        String requestKey,
        String stockCode,
        AnalysisMode analysisMode,
        LocalDate baseDate,
        String engineVersion,
        String configVersion,
        Instant dataCutoffAt,
        boolean forceRecalculate,
        String requestedBy
) {
}
