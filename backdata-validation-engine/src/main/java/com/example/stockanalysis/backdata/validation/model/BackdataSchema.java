package com.example.stockanalysis.backdata.validation.model;

import java.util.Objects;
import java.util.Set;

public record BackdataSchema(
        String apiId,
        BackdataDatasetType datasetType,
        boolean stockCodeRequired,
        boolean referenceCodeRequired,
        boolean businessDateRequired,
        boolean historicalBackfillSupported,
        Set<String> requiredFields,
        Set<String> numericFields,
        Set<String> nonNegativeFields
) {

    public BackdataSchema {
        Objects.requireNonNull(apiId, "apiId");
        Objects.requireNonNull(datasetType, "datasetType");
        requiredFields = Set.copyOf(Objects.requireNonNullElse(requiredFields, Set.of()));
        numericFields = Set.copyOf(Objects.requireNonNullElse(numericFields, Set.of()));
        nonNegativeFields = Set.copyOf(Objects.requireNonNullElse(nonNegativeFields, Set.of()));
    }
}
