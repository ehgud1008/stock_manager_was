package com.example.stockanalysis.kiwoom.auth;

import com.example.stockanalysis.kiwoom.client.KiwoomApiException;
import com.example.stockanalysis.kiwoom.client.KiwoomCallLogSupport;
import com.example.stockanalysis.kiwoom.client.KiwoomErrorDecoder;
import com.example.stockanalysis.kiwoom.config.KiwoomProperties;
import com.example.stockanalysis.kiwoom.contract.tr.au10001.Au10001Request;
import com.example.stockanalysis.kiwoom.contract.tr.au10001.Au10001Response;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class KiwoomTokenProvider {

    private static final String API_ID = "au10001";
    private static final String OPERATION = "ISSUE_ACCESS_TOKEN";
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter EXPIRES_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Duration FALLBACK_TOKEN_LIFETIME = Duration.ofHours(1);
    private static final Logger log = LoggerFactory.getLogger(KiwoomTokenProvider.class);

    private final KiwoomAuthHttpApi authHttpApi;
    private final KiwoomProperties properties;
    private final KiwoomErrorDecoder errorDecoder;
    private final AtomicReference<CachedToken> cache = new AtomicReference<>();

    public KiwoomTokenProvider(
            KiwoomAuthHttpApi authHttpApi,
            KiwoomProperties properties,
            KiwoomErrorDecoder errorDecoder
    ) {
        this.authHttpApi = authHttpApi;
        this.properties = properties;
        this.errorDecoder = errorDecoder;
    }

    public String getAccessToken() {
        Instant now = Instant.now();
        CachedToken current = cache.get();
        if (current != null && current.isValidAt(now)) {
            log.debug("키움 인증 토큰 캐시 사용. expiresAt={}", current.expiresAt());
            return current.value();
        }

        synchronized (this) {
            now = Instant.now();
            CachedToken rechecked = cache.get();
            if (rechecked != null && rechecked.isValidAt(now)) {
                log.debug("키움 인증 토큰 캐시 사용. expiresAt={}", rechecked.expiresAt());
                return rechecked.value();
            }

            CachedToken refreshed = requestNewToken(now);
            cache.set(refreshed);
            return refreshed.value();
        }
    }

    private CachedToken requestNewToken(Instant requestedAt) {
        String previousApiId = MDC.get("apiId");
        MDC.put("apiId", API_ID);
        try {
            long startedAt = System.currentTimeMillis();
            KiwoomCallLogSupport.logStart(log, API_ID, OPERATION, false);
            try {
                String appKey = properties.activeAppKey();
                String secretKey = properties.activeSecretKey();
                if (!StringUtils.hasText(appKey) || !StringUtils.hasText(secretKey)) {
                    throw new KiwoomApiException(
                            "키움 API 인증정보가 설정되지 않았습니다.",
                            null,
                            "MISSING_CREDENTIALS"
                    );
                }

                Au10001Response response = authHttpApi.issueToken(
                        Au10001Request.builder()
                                .grantType(properties.getGrantType())
                                .appkey(appKey)
                                .secretkey(secretKey)
                                .build()
                );
                if (response == null) {
                    throw new KiwoomApiException(
                            "키움 인증 응답이 비어 있습니다.",
                            200,
                            "EMPTY_RESPONSE"
                    );
                }

                errorDecoder.verify(API_ID, response.getReturnCode(), response.getReturnMsg());
                if (!StringUtils.hasText(response.getToken())) {
                    throw new KiwoomApiException(
                            "키움 인증 응답에 토큰이 없습니다.",
                            200,
                            "MISSING_TOKEN"
                    );
                }

                Instant expiresAt = parseExpiresAt(response.getExpiresDt(), requestedAt)
                        .minus(properties.getTokenRefreshMargin());
                CachedToken token = new CachedToken(response.getToken(), expiresAt);
                KiwoomCallLogSupport.logSuccess(
                        log,
                        API_ID,
                        OPERATION,
                        200,
                        System.currentTimeMillis() - startedAt
                );
                log.info("키움 인증 토큰 갱신 완료. expiresAt={}", expiresAt);
                return token;
            } catch (RuntimeException exception) {
                KiwoomCallLogSupport.logFailure(
                        log,
                        API_ID,
                        OPERATION,
                        System.currentTimeMillis() - startedAt,
                        exception
                );
                throw exception;
            }
        } finally {
            if (previousApiId == null) {
                MDC.remove("apiId");
            } else {
                MDC.put("apiId", previousApiId);
            }
        }
    }

    private Instant parseExpiresAt(String value, Instant requestedAt) {
        if (!StringUtils.hasText(value)) {
            log.warn(
                    "키움 인증 토큰 만료시각 기본값 사용. reason=MISSING_EXPIRES_AT, fallbackSeconds={}",
                    FALLBACK_TOKEN_LIFETIME.toSeconds()
            );
            return requestedAt.plus(FALLBACK_TOKEN_LIFETIME);
        }
        try {
            return LocalDateTime.parse(value, EXPIRES_AT_FORMAT)
                    .atZone(SEOUL)
                    .toInstant();
        } catch (DateTimeParseException exception) {
            log.warn(
                    "키움 인증 토큰 만료시각 기본값 사용. reason=INVALID_EXPIRES_AT, fallbackSeconds={}",
                    FALLBACK_TOKEN_LIFETIME.toSeconds()
            );
            return requestedAt.plus(FALLBACK_TOKEN_LIFETIME);
        }
    }

    private record CachedToken(String value, Instant expiresAt) {

        private boolean isValidAt(Instant instant) {
            return instant.isBefore(expiresAt);
        }
    }
}
