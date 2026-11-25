package com.example.stockanalysis.analysis.api;

import com.example.stockanalysis.analysis.application.AnalysisHistoryPage;
import java.util.List;

public record AnalysisHistoryResponse(
        List<StockAnalysisResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    static AnalysisHistoryResponse from(AnalysisHistoryPage result) {
        return new AnalysisHistoryResponse(
                result.content().stream().map(StockAnalysisResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
