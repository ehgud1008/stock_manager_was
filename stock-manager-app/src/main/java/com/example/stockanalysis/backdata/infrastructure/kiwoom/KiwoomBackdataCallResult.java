package com.example.stockanalysis.backdata.infrastructure.kiwoom;

import com.example.stockanalysis.kiwoom.client.KiwoomContinuation;
import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;

public record KiwoomBackdataCallResult(
        KiwoomResult body,
        KiwoomContinuation continuation
) {
}
