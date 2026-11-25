package com.example.stockanalysis.backdata.infrastructure.kiwoom;

import com.example.stockanalysis.kiwoom.client.KiwoomApiException;
import com.example.stockanalysis.kiwoom.client.KiwoomCallLogSupport;
import com.example.stockanalysis.kiwoom.client.KiwoomContinuation;
import com.example.stockanalysis.kiwoom.client.KiwoomErrorDecoder;
import com.example.stockanalysis.kiwoom.client.KiwoomHeaders;
import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;
import com.example.stockanalysis.shared.exception.BusinessException;
import com.example.stockanalysis.shared.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KiwoomBackdataClient {

    private static final String OPERATION = "BACKDATA_QUERY";
    private static final Logger log = LoggerFactory.getLogger(KiwoomBackdataClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final KiwoomErrorDecoder errorDecoder;

    public KiwoomBackdataClient(
            @Qualifier("kiwoomAuthorizedRestClient") RestClient restClient,
            ObjectMapper objectMapper,
            KiwoomErrorDecoder errorDecoder
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.errorDecoder = errorDecoder;
    }

    public KiwoomBackdataCallResult execute(
            KiwoomBackdataTrDefinition definition,
            Map<String, Object> requestValues,
            String continuation,
            String nextKey
    ) {
        Object typedRequest;
        try {
            typedRequest = objectMapper.convertValue(requestValues, definition.requestType());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "API " + definition.apiId() + " 요청 형식이 올바르지 않습니다.",
                    exception
            );
        }

        boolean continuationRequest = "Y".equalsIgnoreCase(continuation);
        try (MDC.MDCCloseable ignored = MDC.putCloseable("apiId", definition.apiId())) {
            long startedAt = System.currentTimeMillis();
            KiwoomCallLogSupport.logStart(
                    log,
                    definition.apiId(),
                    OPERATION,
                    continuationRequest
            );
            try {
                ResponseEntity<? extends KiwoomResult> entity = restClient.post()
                        .uri(definition.uri())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(KiwoomHeaders.API_ID, definition.apiId())
                        .header(KiwoomHeaders.CONTINUATION, normalizeContinuation(continuation))
                        .header(KiwoomHeaders.NEXT_KEY, nextKey == null ? "" : nextKey)
                        .body(typedRequest)
                        .retrieve()
                        .toEntity(definition.responseType());

                KiwoomResult body = entity.getBody();
                if (body == null) {
                    throw new KiwoomApiException(
                            "키움 백데이터 응답이 비어 있습니다.",
                            entity.getStatusCode().value(),
                            "EMPTY_RESPONSE"
                    );
                }
                errorDecoder.verify(definition.apiId(), body.getReturnCode(), body.getReturnMsg());
                KiwoomBackdataCallResult result = new KiwoomBackdataCallResult(
                        body,
                        KiwoomContinuation.from(entity.getHeaders())
                );
                KiwoomCallLogSupport.logSuccess(
                        log,
                        definition.apiId(),
                        OPERATION,
                        entity.getStatusCode().value(),
                        System.currentTimeMillis() - startedAt
                );
                return result;
            } catch (RuntimeException exception) {
                KiwoomCallLogSupport.logFailure(
                        log,
                        definition.apiId(),
                        OPERATION,
                        System.currentTimeMillis() - startedAt,
                        exception
                );
                throw exception;
            }
        }
    }

    private String normalizeContinuation(String continuation) {
        return "Y".equalsIgnoreCase(continuation) ? "Y" : "N";
    }
}
