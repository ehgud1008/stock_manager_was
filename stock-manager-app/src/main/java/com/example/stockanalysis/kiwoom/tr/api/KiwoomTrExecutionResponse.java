package com.example.stockanalysis.kiwoom.tr.api;

import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;

public record KiwoomTrExecutionResponse(
        KiwoomTrResponseHeader header,
        KiwoomResult body
) {
}
