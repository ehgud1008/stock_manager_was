package com.example.stockanalysis.kiwoom.client.watchlist;

import com.example.stockanalysis.kiwoom.client.KiwoomApiException;
import com.example.stockanalysis.kiwoom.client.KiwoomCallLogSupport;
import com.example.stockanalysis.kiwoom.client.KiwoomContinuation;
import com.example.stockanalysis.kiwoom.client.KiwoomErrorDecoder;
import com.example.stockanalysis.kiwoom.client.KiwoomResponse;
import com.example.stockanalysis.kiwoom.contract.tr.ka01300.Ka01300Request;
import com.example.stockanalysis.kiwoom.contract.tr.ka01300.Ka01300Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class KiwoomWatchlistClient {

    public static final String GROUP_LIST_API_ID = "ka01300";
    private static final String OPERATION = "WATCHLIST_GROUP_LIST";
    private static final Logger log = LoggerFactory.getLogger(KiwoomWatchlistClient.class);

    private final KiwoomWatchlistHttpApi httpApi;
    private final KiwoomErrorDecoder errorDecoder;

    public KiwoomWatchlistClient(
            KiwoomWatchlistHttpApi httpApi,
            KiwoomErrorDecoder errorDecoder
    ) {
        this.httpApi = httpApi;
        this.errorDecoder = errorDecoder;
    }

    public KiwoomResponse<Ka01300Response> getGroups() {
        try (MDC.MDCCloseable ignored = MDC.putCloseable("apiId", GROUP_LIST_API_ID)) {
            long startedAt = System.currentTimeMillis();
            KiwoomCallLogSupport.logStart(log, GROUP_LIST_API_ID, OPERATION, false);
            try {
                ResponseEntity<Ka01300Response> entity = httpApi.getWatchlistGroups(
                        GROUP_LIST_API_ID,
                        "N",
                        "",
                        new Ka01300Request()
                );
                Ka01300Response body = entity.getBody();
                if (body == null) {
                    throw new KiwoomApiException(
                            "키움 관심종목 그룹 응답이 비어 있습니다.",
                            entity.getStatusCode().value(),
                            "EMPTY_RESPONSE"
                    );
                }
                errorDecoder.verify(
                        GROUP_LIST_API_ID,
                        body.getReturnCode(),
                        body.getReturnMsg()
                );
                if (!"S".equalsIgnoreCase(body.getRtcd())) {
                    throw new KiwoomApiException(
                            "키움 관심종목 그룹 조회 결과가 실패 상태입니다.",
                            entity.getStatusCode().value(),
                            "INVALID_RESULT_STATUS"
                    );
                }
                KiwoomResponse<Ka01300Response> response = new KiwoomResponse<>(
                        body,
                        KiwoomContinuation.from(entity.getHeaders())
                );
                KiwoomCallLogSupport.logSuccess(
                        log,
                        GROUP_LIST_API_ID,
                        OPERATION,
                        entity.getStatusCode().value(),
                        System.currentTimeMillis() - startedAt
                );
                return response;
            } catch (RuntimeException exception) {
                KiwoomCallLogSupport.logFailure(
                        log,
                        GROUP_LIST_API_ID,
                        OPERATION,
                        System.currentTimeMillis() - startedAt,
                        exception
                );
                throw exception;
            }
        }
    }
}
