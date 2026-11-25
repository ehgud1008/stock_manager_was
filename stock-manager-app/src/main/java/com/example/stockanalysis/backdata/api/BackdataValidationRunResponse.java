package com.example.stockanalysis.backdata.api;

import com.example.stockanalysis.backdata.validation.model.BackdataDatasetType;
import com.example.stockanalysis.backdata.validation.model.ValidationIssue;
import com.example.stockanalysis.backdata.validation.model.ValidationProfile;
import com.example.stockanalysis.backdata.validation.model.ValidationResult;
import com.example.stockanalysis.backdata.validation.model.ValidationSummary;
import com.example.stockanalysis.kiwoom.client.KiwoomContinuation;
import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BackdataValidationRunResponse(
        UUID runId,
        String apiId,
        String apiName,
        BackdataDatasetType datasetType,
        ValidationProfile profile,
        Instant observedAt,
        BackdataValidationStatus validationStatus,
        boolean backtestReady,
        ValidationSummary summary,
        List<ValidationIssue> issues,
        KiwoomResult kiwoomResponse,
        KiwoomContinuation continuation
) {

    public static BackdataValidationRunResponse of(
            UUID runId,
            String apiId,
            String apiName,
            BackdataDatasetType datasetType,
            ValidationProfile profile,
            Instant observedAt,
            ValidationResult result,
            KiwoomResult kiwoomResponse,
            KiwoomContinuation continuation
    ) {
        return new BackdataValidationRunResponse(
                runId,
                apiId,
                apiName,
                datasetType,
                profile,
                observedAt,
                BackdataValidationStatus.from(result),
                result.isBacktestReady(),
                result.summary(),
                result.issues(),
                kiwoomResponse,
                continuation
        );
    }
}
