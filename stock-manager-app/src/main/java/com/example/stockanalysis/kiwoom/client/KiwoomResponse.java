package com.example.stockanalysis.kiwoom.client;

public record KiwoomResponse<T>(T body, KiwoomContinuation continuation) {
}
