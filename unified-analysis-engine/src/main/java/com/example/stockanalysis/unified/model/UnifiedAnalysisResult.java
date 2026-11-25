package com.example.stockanalysis.unified.model;

import com.example.stockanalysis.engine.model.AnalysisResult;
import com.example.stockanalysis.engine.model.StockSnapshot;
import com.example.stockanalysis.engine.screener.ScreenerResult;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** Component versions, scores, signals and quality remain in their original result objects. */
public record UnifiedAnalysisResult(
        String engineVersion,
        StockSnapshot stock,
        LocalDateTime asOf,
        LocalDate priceDate,
        int dailyBarCount,
        int weeklyBarCount,
        int monthlyBarCount,
        AnalysisResult analysis,
        ScreenerResult screener,
        List<UnifiedWarning> warnings
) {
    public UnifiedAnalysisResult {
        if (engineVersion == null || engineVersion.isBlank()) throw new IllegalArgumentException("engineVersion required");
        Objects.requireNonNull(stock, "stock");
        Objects.requireNonNull(asOf, "asOf");
        Objects.requireNonNull(priceDate, "priceDate");
        Objects.requireNonNull(analysis, "analysis");
        Objects.requireNonNull(screener, "screener");
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings"));
        if (dailyBarCount < 1 || weeklyBarCount < 0 || monthlyBarCount < 0) {
            throw new IllegalArgumentException("invalid bar counts");
        }
    }
}
