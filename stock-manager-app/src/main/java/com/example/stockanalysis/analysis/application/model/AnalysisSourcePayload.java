package com.example.stockanalysis.analysis.application.model;

import com.example.stockanalysis.backdata.validation.model.BackdataRecord;
import com.example.stockanalysis.kiwoom.client.KiwoomContinuation;
import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AnalysisSourcePayload(
        String apiId,
        String responseDtoType,
        String sourceRole,
        Map<String, Object> request,
        KiwoomResult response,
        KiwoomContinuation continuation,
        List<BackdataRecord> records,
        Instant requestedAt,
        Instant receivedAt
) {

    public AnalysisSourcePayload {
        request = Map.copyOf(request);
        records = List.copyOf(records);
    }
}
