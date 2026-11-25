package com.example.stockanalysis.kiwoom.tr.infrastructure;

import com.example.stockanalysis.kiwoom.client.KiwoomApiException;
import com.example.stockanalysis.kiwoom.client.KiwoomCallLogSupport;
import com.example.stockanalysis.kiwoom.client.KiwoomContinuation;
import com.example.stockanalysis.kiwoom.client.KiwoomErrorDecoder;
import com.example.stockanalysis.kiwoom.client.KiwoomHeaders;
import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;
import com.example.stockanalysis.shared.exception.BusinessException;
import com.example.stockanalysis.shared.exception.ErrorCode;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KiwoomTrClient {

    private static final String OPERATION = "INTERNAL_TR_PROXY";
    private static final Logger log = LoggerFactory.getLogger(KiwoomTrClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final KiwoomErrorDecoder errorDecoder;

    public KiwoomTrClient(
            @Qualifier("kiwoomManualAuthorizationRestClient") RestClient restClient,
            ObjectMapper objectMapper,
            KiwoomErrorDecoder errorDecoder
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.errorDecoder = errorDecoder;
    }

    public KiwoomTrCallResult execute(
            KiwoomTrDefinition definition,
            String authorization,
            String continuation,
            String nextKey,
            Map<String, Object> requestValues
    ) {
        validateRequestFields(definition, requestValues);
        Object typedRequest = toTypedRequest(definition, requestValues);
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
                        .header(HttpHeaders.AUTHORIZATION, normalizeAuthorization(authorization))
                        .header(KiwoomHeaders.API_ID, definition.apiId())
                        .header(KiwoomHeaders.CONTINUATION, continuationRequest ? "Y" : "N")
                        .header(KiwoomHeaders.NEXT_KEY, nextKey == null ? "" : nextKey)
                        .body(typedRequest)
                        .retrieve()
                        .toEntity(definition.responseType());

                KiwoomResult body = entity.getBody();
                if (body == null) {
                    throw new KiwoomApiException(
                            "키움 전문 응답이 비어 있습니다.",
                            entity.getStatusCode().value(),
                            "EMPTY_RESPONSE"
                    );
                }
                errorDecoder.verify(definition.apiId(), body.getReturnCode(), body.getReturnMsg());
                KiwoomTrCallResult result = new KiwoomTrCallResult(
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

    private Object toTypedRequest(
            KiwoomTrDefinition definition,
            Map<String, Object> requestValues
    ) {
        try {
            return objectMapper.convertValue(requestValues, definition.requestType());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "API " + definition.apiId() + " 요청 본문이 DTO 형식과 일치하지 않습니다.",
                    exception
            );
        }
    }

    private void validateRequestFields(
            KiwoomTrDefinition definition,
            Map<String, Object> requestValues
    ) {
        BeanDescription description = objectMapper.getDeserializationConfig()
                .introspect(objectMapper.constructType(definition.requestType()));
        Set<String> allowedFields = description.findProperties().stream()
                .map(property -> property.getName())
                .collect(Collectors.toSet());
        Set<String> unknownFields = new LinkedHashSet<>(requestValues.keySet());
        unknownFields.removeAll(allowedFields);
        if (!unknownFields.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "API " + definition.apiId() + " 요청 DTO에 없는 필드입니다: " + unknownFields
            );
        }
    }

    private String normalizeAuthorization(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "authorization이 필요합니다.");
        }
        String value = authorization.trim();
        if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "authorization 형식이 올바르지 않습니다.");
        }
        return value.regionMatches(true, 0, "Bearer ", 0, 7)
                ? value
                : "Bearer " + value;
    }
}
