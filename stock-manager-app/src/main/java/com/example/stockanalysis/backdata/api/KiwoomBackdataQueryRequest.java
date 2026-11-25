package com.example.stockanalysis.backdata.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public record KiwoomBackdataQueryRequest(
        @NotBlank String apiId,
        @NotNull Map<String, Object> request,
        LocalDate asOfDate,
        String continuation,
        String nextKey
) {

    public KiwoomBackdataQueryRequest {
        request = request == null
                ? Map.of()
                : Map.copyOf(new LinkedHashMap<>(request));
    }
}
