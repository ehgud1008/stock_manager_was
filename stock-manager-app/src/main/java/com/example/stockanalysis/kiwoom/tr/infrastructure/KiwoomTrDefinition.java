package com.example.stockanalysis.kiwoom.tr.infrastructure;

import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;

public record KiwoomTrDefinition(
        String apiId,
        String apiName,
        String uri,
        Class<?> requestType,
        Class<? extends KiwoomResult> responseType
) {
}
