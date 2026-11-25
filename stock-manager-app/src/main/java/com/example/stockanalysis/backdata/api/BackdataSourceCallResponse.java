package com.example.stockanalysis.backdata.api;

import com.example.stockanalysis.backdata.validation.model.BackdataDatasetType;
import com.example.stockanalysis.backdata.validation.model.ValidationIssue;
import com.example.stockanalysis.kiwoom.client.KiwoomContinuation;
import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;
import java.util.List;

public record BackdataSourceCallResponse(
        int queryIndex,
        String apiId,
        String apiName,
        BackdataDatasetType datasetType,
        int recordCount,
        BackdataValidationStatus validationStatus,
        List<ValidationIssue> issues,
        KiwoomResult kiwoomResponse,
        KiwoomContinuation continuation
) {

    public BackdataSourceCallResponse {
        issues = List.copyOf(issues);
    }
}
