package com.example.stockanalysis.kiwoom.client;

import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

public record KiwoomContinuation(boolean hasNext, String nextKey) {

    public static KiwoomContinuation from(HttpHeaders headers) {
        String continuation = headers.getFirst(KiwoomHeaders.CONTINUATION);
        String nextKey = headers.getFirst(KiwoomHeaders.NEXT_KEY);
        return new KiwoomContinuation(
                "Y".equalsIgnoreCase(continuation),
                StringUtils.hasText(nextKey) ? nextKey : ""
        );
    }

    public static KiwoomContinuation none() {
        return new KiwoomContinuation(false, "");
    }
}
