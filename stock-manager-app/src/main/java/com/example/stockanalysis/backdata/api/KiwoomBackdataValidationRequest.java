package com.example.stockanalysis.backdata.api;

import com.example.stockanalysis.backdata.validation.model.ValidationProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public record KiwoomBackdataValidationRequest(
        @NotBlank String apiId,
        @NotNull Map<String, Object> request,
        ValidationProfile profile,
        LocalDate asOfDate,
        String continuation,
        String nextKey
) {

    public KiwoomBackdataValidationRequest {
        request = request == null
                ? Map.of()
                : Map.copyOf(new LinkedHashMap<>(request));
        profile = profile == null ? ValidationProfile.BACKTEST_READY : profile;
    }
}
