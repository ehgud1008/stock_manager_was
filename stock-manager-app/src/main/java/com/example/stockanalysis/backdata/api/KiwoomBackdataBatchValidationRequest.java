package com.example.stockanalysis.backdata.api;

import com.example.stockanalysis.backdata.validation.model.ValidationProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record KiwoomBackdataBatchValidationRequest(
        ValidationProfile profile,
        @NotEmpty List<@Valid KiwoomBackdataQueryRequest> queries
) {

    public KiwoomBackdataBatchValidationRequest {
        profile = profile == null ? ValidationProfile.BACKTEST_READY : profile;
        queries = queries == null ? List.of() : List.copyOf(queries);
    }
}
