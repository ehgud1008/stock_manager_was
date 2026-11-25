package com.example.stockanalysis.analysis.application;

import com.example.stockanalysis.engine.model.AnalysisMode;
import java.time.LocalDate;

public record AnalysisExecutionCommand(
        AnalysisMode analysisMode,
        LocalDate baseDate,
        boolean forceRecalculate,
        String requestedBy,
        com.example.stockanalysis.engine.model.PositionContext position
) {
    public AnalysisExecutionCommand(AnalysisMode analysisMode, LocalDate baseDate, boolean forceRecalculate, String requestedBy) {
        this(analysisMode, baseDate, forceRecalculate, requestedBy, null);
    }
}
