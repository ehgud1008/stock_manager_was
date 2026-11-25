package com.example.stockanalysis.screener.domain;

import com.example.stockanalysis.engine.screener.ScreenerConfig;
import java.time.Instant;
import java.time.LocalDateTime;

public record ScreenerRun(String runId, String requestKey, ScreenerRunRequest request, LocalDateTime cutoff,
        ScreenerConfig config, String engineVersion, Instant createdAt, Instant finishedAt, Status status,
        int totalCount, int completedCount, int failedCount, int skippedCount, String errorCode) {
    public enum Status { READY, RUNNING, COMPLETED, COMPLETED_WITH_ERRORS, FAILED, INTERRUPTED }
    public ScreenerRun progress(Status next, int total, int completed, int failed, int skipped, String error, Instant finished) {
        return new ScreenerRun(runId, requestKey, request, cutoff, config, engineVersion, createdAt, finished, next, total, completed, failed, skipped, error);
    }
}
