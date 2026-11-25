package com.example.stockanalysis.backdata.api;

import com.example.stockanalysis.backdata.validation.model.ValidationIssue;
import com.example.stockanalysis.backdata.validation.model.ValidationProfile;
import com.example.stockanalysis.backdata.validation.model.ValidationResult;
import com.example.stockanalysis.backdata.validation.model.ValidationSummary;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BackdataBatchValidationRunResponse(
        UUID runId,
        ValidationProfile profile,
        Instant observedAt,
        BackdataValidationStatus validationStatus,
        boolean backtestReady,
        List<BackdataSourceCallResponse> sourceResponses,
        ValidationSummary summary,
        List<ValidationIssue> issues
) {

    public static BackdataBatchValidationRunResponse of(
            UUID runId,
            ValidationProfile profile,
            Instant observedAt,
            List<BackdataSourceCallResponse> sourceResponses,
            ValidationResult result
    ) {
        BackdataValidationStatus status = aggregateStatus(sourceResponses, result);
        return new BackdataBatchValidationRunResponse(
                runId,
                profile,
                observedAt,
                status,
                result.isBacktestReady() && status != BackdataValidationStatus.INVALID,
                List.copyOf(sourceResponses),
                result.summary(),
                result.issues()
        );
    }

    private static BackdataValidationStatus aggregateStatus(
            List<BackdataSourceCallResponse> sourceResponses,
            ValidationResult result
    ) {
        if (!result.isBacktestReady() || sourceResponses.stream().anyMatch(
                source -> source.validationStatus() == BackdataValidationStatus.INVALID
        )) {
            return BackdataValidationStatus.INVALID;
        }
        if (!result.issues().isEmpty() || sourceResponses.stream().anyMatch(
                source -> source.validationStatus() == BackdataValidationStatus.WARNING
        )) {
            return BackdataValidationStatus.WARNING;
        }
        return BackdataValidationStatus.VALID;
    }
}
