package com.example.stockanalysis.kiwoom.client;

import org.springframework.stereotype.Component;

@Component
public class KiwoomErrorDecoder {

    public void verify(String apiId, Integer returnCode, String returnMessage) {
        if (returnCode == null || returnCode == 0) {
            return;
        }

        throw new KiwoomApiException(
                "키움 API가 요청을 정상 처리하지 못했습니다.",
                null,
                "RETURN_CODE_" + returnCode
        );
    }
}
