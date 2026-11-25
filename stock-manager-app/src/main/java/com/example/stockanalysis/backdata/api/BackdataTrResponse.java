package com.example.stockanalysis.backdata.api;

import com.example.stockanalysis.backdata.infrastructure.kiwoom.KiwoomBackdataTrDefinition;
import com.example.stockanalysis.backdata.validation.model.BackdataDatasetType;
import java.util.Set;

public record BackdataTrResponse(
        String apiId,
        String name,
        BackdataDatasetType datasetType,
        String requestType,
        boolean historicalBackfillSupported,
        boolean stockCodeRequired,
        boolean referenceCodeRequired,
        boolean businessDateRequired,
        Set<String> requiredFields
) {

    public static BackdataTrResponse from(KiwoomBackdataTrDefinition definition) {
        return new BackdataTrResponse(
                definition.apiId(),
                definition.name(),
                definition.schema().datasetType(),
                definition.requestType().getSimpleName(),
                definition.schema().historicalBackfillSupported(),
                definition.schema().stockCodeRequired(),
                definition.schema().referenceCodeRequired(),
                definition.schema().businessDateRequired(),
                definition.schema().requiredFields()
        );
    }
}
