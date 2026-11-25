package com.example.stockanalysis.analysis.application.model;

import com.example.stockanalysis.analysis.domain.DataQualityStatus;
import com.example.stockanalysis.engine.model.AnalysisInput;
import java.util.List;

public record CollectedAnalysisData(
        AnalysisInput input,
        List<AnalysisSourcePayload> sources,
        List<String> warnings,
        DataQualityStatus dataQualityStatus
) {

    public CollectedAnalysisData {
        sources = List.copyOf(sources);
        warnings = List.copyOf(warnings);
    }
}
