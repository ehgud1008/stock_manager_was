package com.example.stockanalysis.kiwoom.client.stockinfo;

import com.example.stockanalysis.kiwoom.client.KiwoomApiException;
import com.example.stockanalysis.kiwoom.client.KiwoomCallLogSupport;
import com.example.stockanalysis.kiwoom.client.KiwoomContinuation;
import com.example.stockanalysis.kiwoom.client.KiwoomErrorDecoder;
import com.example.stockanalysis.kiwoom.client.KiwoomResponse;
import com.example.stockanalysis.kiwoom.contract.tr.ka10001.Ka10001Request;
import com.example.stockanalysis.kiwoom.contract.tr.ka10001.Ka10001Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class KiwoomStockInfoClient {

    public static final String BASIC_INFORMATION_API_ID = "ka10001";
    private static final String OPERATION = "STOCK_BASIC_INFORMATION";
    private static final Logger log = LoggerFactory.getLogger(KiwoomStockInfoClient.class);

    private final KiwoomStockInfoHttpApi httpApi;
    private final KiwoomErrorDecoder errorDecoder;

    public KiwoomStockInfoClient(
            KiwoomStockInfoHttpApi httpApi,
            KiwoomErrorDecoder errorDecoder
    ) {
        this.httpApi = httpApi;
        this.errorDecoder = errorDecoder;
    }

    public KiwoomResponse<Ka10001Response> getBasicInformation(String stockCode) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable("apiId", BASIC_INFORMATION_API_ID)) {
            long startedAt = System.currentTimeMillis();
            KiwoomCallLogSupport.logStart(log, BASIC_INFORMATION_API_ID, OPERATION, false);
            try {
                ResponseEntity<Ka10001Response> entity = httpApi.getStockBasicInformation(
                        BASIC_INFORMATION_API_ID,
                        "N",
                        "",
                        Ka10001Request.builder().stkCd(stockCode).build()
                );
                Ka10001Response body = entity.getBody();
                if (body == null) {
                    throw new KiwoomApiException(
                            "키움 종목정보 응답이 비어 있습니다.",
                            entity.getStatusCode().value(),
                            "EMPTY_RESPONSE"
                    );
                }
                errorDecoder.verify(
                        BASIC_INFORMATION_API_ID,
                        body.getReturnCode(),
                        body.getReturnMsg()
                );
                KiwoomResponse<Ka10001Response> response = new KiwoomResponse<>(
                        body,
                        KiwoomContinuation.from(entity.getHeaders())
                );
                KiwoomCallLogSupport.logSuccess(
                        log,
                        BASIC_INFORMATION_API_ID,
                        OPERATION,
                        entity.getStatusCode().value(),
                        System.currentTimeMillis() - startedAt
                );
                return response;
            } catch (RuntimeException exception) {
                KiwoomCallLogSupport.logFailure(
                        log,
                        BASIC_INFORMATION_API_ID,
                        OPERATION,
                        System.currentTimeMillis() - startedAt,
                        exception
                );
                throw exception;
            }
        }
    }
}
