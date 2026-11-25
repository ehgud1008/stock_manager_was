package com.example.stockanalysis.kiwoom.client;

import com.example.stockanalysis.shared.logging.SensitiveLogSanitizer;
import org.slf4j.Logger;

public final class KiwoomCallLogSupport {

    private static final String TARGET_SYSTEM = "KIWOOM";
    private static final int RETRY_COUNT = 0;
    private static final long SLOW_CALL_THRESHOLD_MS = 2_000L;

    private KiwoomCallLogSupport() {
    }

    public static void logStart(
            Logger log,
            String apiId,
            String operation,
            boolean continuation
    ) {
        log.info(
                "외부 API 호출 시작. targetSystem={}, apiId={}, operation={}, continuation={}, retryCount={}",
                TARGET_SYSTEM,
                apiId,
                operation,
                continuation,
                RETRY_COUNT
        );
    }

    public static void logSuccess(
            Logger log,
            String apiId,
            String operation,
            int statusCode,
            long elapsedMs
    ) {
        if (elapsedMs >= SLOW_CALL_THRESHOLD_MS) {
            log.warn(
                    "외부 API 호출 종료. targetSystem={}, apiId={}, operation={}, status={}, "
                            + "result=SUCCESS, retryCount={}, elapsedMs={}, thresholdMs={}",
                    TARGET_SYSTEM,
                    apiId,
                    operation,
                    statusCode,
                    RETRY_COUNT,
                    elapsedMs,
                    SLOW_CALL_THRESHOLD_MS
            );
        } else {
            log.info(
                    "외부 API 호출 종료. targetSystem={}, apiId={}, operation={}, status={}, "
                            + "result=SUCCESS, retryCount={}, elapsedMs={}",
                    TARGET_SYSTEM,
                    apiId,
                    operation,
                    statusCode,
                    RETRY_COUNT,
                    elapsedMs
            );
        }
    }

    public static void logFailure(
            Logger log,
            String apiId,
            String operation,
            long elapsedMs,
            RuntimeException exception
    ) {
        Integer statusCode = null;
        String failureCode = exception.getClass().getSimpleName();
        if (exception instanceof KiwoomApiException apiException) {
            statusCode = apiException.getExternalStatusCode();
            failureCode = apiException.getFailureCode();
            if (!apiException.markLogged()) {
                log.warn(
                        "외부 API 호출 중단. targetSystem={}, apiId={}, operation={}, status={}, "
                                + "result=FAILURE, retryCount={}, failureCode={}, errorLoggedByUpstream=true, "
                                + "elapsedMs={}",
                        TARGET_SYSTEM,
                        apiId,
                        operation,
                        statusCode,
                        RETRY_COUNT,
                        failureCode,
                        elapsedMs
                );
                return;
            }
        }

        log.error(
                "외부 API 호출 실패. targetSystem={}, apiId={}, operation={}, status={}, "
                        + "result=FAILURE, retryCount={}, failureCode={}, elapsedMs={}",
                TARGET_SYSTEM,
                apiId,
                operation,
                statusCode,
                RETRY_COUNT,
                failureCode,
                elapsedMs,
                SensitiveLogSanitizer.sanitize(exception)
        );
    }
}
