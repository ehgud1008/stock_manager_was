package com.example.stockanalysis.analysis.application;

import com.example.stockanalysis.analysis.domain.AnalysisRunView;
import java.util.List;

public record AnalysisHistoryPage(
        List<AnalysisRunView> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public AnalysisHistoryPage {
        content = List.copyOf(content);
    }
}
