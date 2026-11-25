package com.example.stockanalysis.kiwoom.client.ranking;

import com.example.stockanalysis.kiwoom.client.KiwoomApiException;
import com.example.stockanalysis.kiwoom.client.KiwoomCallLogSupport;
import com.example.stockanalysis.kiwoom.client.KiwoomContinuation;
import com.example.stockanalysis.kiwoom.client.KiwoomErrorDecoder;
import com.example.stockanalysis.kiwoom.client.KiwoomResponse;
import com.example.stockanalysis.kiwoom.contract.tr.ka10030.Ka10030Request;
import com.example.stockanalysis.kiwoom.contract.tr.ka10030.Ka10030Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class KiwoomRankingClient {

    public static final String TRADING_VOLUME_API_ID = "ka10030";
    private static final String OPERATION = "TRADING_VOLUME_RANKING";
    private static final Logger log = LoggerFactory.getLogger(KiwoomRankingClient.class);

    private final KiwoomRankingHttpApi httpApi;
    private final KiwoomErrorDecoder errorDecoder;

    public KiwoomRankingClient(
            KiwoomRankingHttpApi httpApi,
            KiwoomErrorDecoder errorDecoder
    ) {
        this.httpApi = httpApi;
        this.errorDecoder = errorDecoder;
    }

    public KiwoomResponse<Ka10030Response> getTradingVolumeRanking(
            Ka10030Request request,
            String nextKey
    ) {
        boolean continuation = StringUtils.hasText(nextKey);
        try (MDC.MDCCloseable ignored = MDC.putCloseable("apiId", TRADING_VOLUME_API_ID)) {
            long startedAt = System.currentTimeMillis();
            KiwoomCallLogSupport.logStart(log, TRADING_VOLUME_API_ID, OPERATION, continuation);
            try {
                ResponseEntity<Ka10030Response> entity = httpApi.getTradingVolumeRanking(
                        TRADING_VOLUME_API_ID,
                        continuation ? "Y" : "N",
                        continuation ? nextKey : "",
                        request
                );
                Ka10030Response body = entity.getBody();
                if (body == null) {
                    throw new KiwoomApiException(
                            "키움 거래량 순위 응답이 비어 있습니다.",
                            entity.getStatusCode().value(),
                            "EMPTY_RESPONSE"
                    );
                }
                errorDecoder.verify(
                        TRADING_VOLUME_API_ID,
                        body.getReturnCode(),
                        body.getReturnMsg()
                );
                KiwoomResponse<Ka10030Response> response = new KiwoomResponse<>(
                        body,
                        KiwoomContinuation.from(entity.getHeaders())
                );
                KiwoomCallLogSupport.logSuccess(
                        log,
                        TRADING_VOLUME_API_ID,
                        OPERATION,
                        entity.getStatusCode().value(),
                        System.currentTimeMillis() - startedAt
                );
                return response;
            } catch (RuntimeException exception) {
                KiwoomCallLogSupport.logFailure(
                        log,
                        TRADING_VOLUME_API_ID,
                        OPERATION,
                        System.currentTimeMillis() - startedAt,
                        exception
                );
                throw exception;
            }
        }
    }
}
