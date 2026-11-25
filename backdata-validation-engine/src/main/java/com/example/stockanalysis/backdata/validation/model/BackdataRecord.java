package com.example.stockanalysis.backdata.validation.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record BackdataRecord(
        String apiId,
        BackdataDatasetType datasetType,
        String stockCode,
        String referenceCode,
        String variant,
        LocalDate businessDate,
        Instant observedAt,
        Map<String, String> values
) {

    public BackdataRecord {
        Objects.requireNonNull(apiId, "apiId");
        Objects.requireNonNull(datasetType, "datasetType");
        Objects.requireNonNull(observedAt, "observedAt");
        values = Map.copyOf(new LinkedHashMap<>(
                Objects.requireNonNullElse(values, Map.of())
        ));
    }

    public String value(String field) {
        return values.get(field);
    }
}
