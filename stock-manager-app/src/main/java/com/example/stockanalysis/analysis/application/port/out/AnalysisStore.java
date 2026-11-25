package com.example.stockanalysis.analysis.application.port.out;

import com.example.stockanalysis.analysis.application.model.CollectedAnalysisData;
import com.example.stockanalysis.analysis.application.model.NewAnalysisRun;
import com.example.stockanalysis.analysis.domain.AnalysisRunView;
import com.example.stockanalysis.analysis.domain.StockPricePoint;
import com.example.stockanalysis.engine.model.AnalysisMode;
import com.example.stockanalysis.engine.model.AnalysisResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AnalysisStore {

    long createRun(NewAnalysisRun run);

    default void saveCompleted(long runId, CollectedAnalysisData data, AnalysisResult result, Instant completedAt) {
        saveCompleted(runId, data, result, completedAt, null);
    }

    void saveCompleted(long runId, CollectedAnalysisData data, AnalysisResult result, Instant completedAt,
                       com.example.stockanalysis.judgment.TradeJudgment judgment);

    void markFailed(long runId, String errorCode, String errorMessage, Instant failedAt);

    Optional<AnalysisRunView> findById(long runId);

    Optional<AnalysisRunView> findLatest(String stockCode, AnalysisMode mode);

    Optional<AnalysisRunView> findCompleted(
            String stockCode,
            AnalysisMode mode,
            LocalDate baseDate
    );

    List<AnalysisRunView> findHistory(String stockCode, AnalysisMode mode, int offset, int size);

    long countHistory(String stockCode, AnalysisMode mode);

    List<StockPricePoint> findPrices(
            String stockCode,
            LocalDate from,
            LocalDate to
    );
}
