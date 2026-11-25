package com.example.stockanalysis.backdata.infrastructure.kiwoom;

import com.example.stockanalysis.backdata.validation.model.BackdataSchema;
import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;
import java.util.Map;
import java.util.Set;

public record KiwoomBackdataTrDefinition(
        String apiId,
        String name,
        String uri,
        Class<?> requestType,
        Class<? extends KiwoomResult> responseType,
        String rowField,
        String rowStockCodeField,
        String rowReferenceCodeField,
        String businessDateField,
        String requestStockCodeField,
        String requestReferenceCodeField,
        Set<String> variantRequestFields,
        Map<String, String> fieldMappings,
        BackdataSchema schema
) {

    public KiwoomBackdataTrDefinition {
        variantRequestFields = Set.copyOf(variantRequestFields);
        fieldMappings = Map.copyOf(fieldMappings);
    }

    public boolean hasRows() {
        return rowField != null && !rowField.isBlank();
    }
}
