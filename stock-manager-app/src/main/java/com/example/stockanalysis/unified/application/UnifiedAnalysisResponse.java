package com.example.stockanalysis.unified.application;

import com.example.stockanalysis.analysis.domain.DataQualityStatus;
import com.example.stockanalysis.unified.model.UnifiedAnalysisResult;
import java.util.List;
import java.math.BigDecimal;
import com.example.stockanalysis.engine.model.DailyCandle;

public record UnifiedAnalysisResponse(UnifiedAnalysisResult result, DataQualityStatus collectionQuality,
                                      List<String> collectionWarnings, Long volume, BigDecimal tradingAmountMillionWon,
                                      List<DailyCandle> priceHistory, EodMetadata eod,
                                      com.example.stockanalysis.judgment.TradeJudgment tradeJudgment,
        com.example.stockanalysis.holdings.HoldingModels.Evaluation holdingAnalysis) {
    public UnifiedAnalysisResponse(UnifiedAnalysisResult result, DataQualityStatus collectionQuality,
                                      List<String> collectionWarnings, Long volume, BigDecimal tradingAmountMillionWon,
                                      List<DailyCandle> priceHistory, EodMetadata eod,
                                      com.example.stockanalysis.judgment.TradeJudgment tradeJudgment) {
        this(result, collectionQuality, collectionWarnings, volume, tradingAmountMillionWon, priceHistory, eod, tradeJudgment, null);
    }
    public UnifiedAnalysisResponse withHolding(com.example.stockanalysis.holdings.HoldingModels.Evaluation holding) {
        return new UnifiedAnalysisResponse(result, collectionQuality, collectionWarnings, volume, tradingAmountMillionWon, priceHistory, eod, tradeJudgment, holding);
    }

    public UnifiedAnalysisResponse(UnifiedAnalysisResult result, DataQualityStatus quality, List<String> warnings,
                                   Long volume, BigDecimal amount, List<DailyCandle> history, EodMetadata eod) {
        this(result, quality, warnings, volume, amount, history, eod, null);
    }
    public record EodMetadata(String priceBasis, java.time.LocalDate tradeDate, java.time.Instant priceCutoffAt,
                              java.time.Instant collectionStartedAt, java.time.Instant collectedAt,
                              java.time.Instant analyzedAt, String readinessPolicy, String calendarVersion) {}
    public UnifiedAnalysisResponse(UnifiedAnalysisResult result, DataQualityStatus quality, List<String> warnings,
                                   Long volume, BigDecimal amount, List<DailyCandle> history) {
        this(result, quality, warnings, volume, amount, history, null);
    }
    public UnifiedAnalysisResponse(UnifiedAnalysisResult result, DataQualityStatus quality, List<String> warnings, Long volume, BigDecimal amount) {
        this(result, quality, warnings, volume, amount, List.of());
    }
    public UnifiedAnalysisResponse(UnifiedAnalysisResult result, DataQualityStatus collectionQuality, List<String> collectionWarnings) {
        this(result, collectionQuality, collectionWarnings, null, null);
    }
    public UnifiedAnalysisResponse {
        collectionWarnings = List.copyOf(collectionWarnings);
        priceHistory = priceHistory == null ? List.of() : List.copyOf(priceHistory);
    }
}
