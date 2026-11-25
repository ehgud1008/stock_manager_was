package com.example.stockanalysis.kiwoom.tr.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KiwoomTrResponseHeader(
        @JsonProperty("api-id") String apiId,
        @JsonProperty("cont-yn") String contYn,
        @JsonProperty("next-key") String nextKey
) {
}
