package com.example.stockanalysis.kiwoom.tr.infrastructure;

import com.example.stockanalysis.kiwoom.client.KiwoomContinuation;
import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;

public record KiwoomTrCallResult(
        KiwoomResult body,
        KiwoomContinuation continuation
) {
}
