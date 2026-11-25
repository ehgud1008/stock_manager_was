package com.example.stockanalysis.shared.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);
    private static final Pattern SAFE_REQUEST_ID =
            Pattern.compile("[A-Za-z0-9._-]{1,128}");
    private static final long SLOW_REQUEST_THRESHOLD_MS = 3_000L;
    private static final int MAX_URI_LOG_LENGTH = 512;

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestedId = request.getHeader(REQUEST_ID_HEADER);
        String requestId = requestedId;
        if (!isSafeRequestId(requestId)) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        String requestUri = sanitizeUri(request.getRequestURI());
        long startedAt = System.currentTimeMillis();
        boolean completedNormally = false;
        try {
            if (StringUtils.hasText(requestedId) && !requestedId.equals(requestId)) {
                log.warn(
                        "HTTP 요청 식별자 대체. reason=INVALID_FORMAT, headerLength={}",
                        requestedId.length()
                );
            }
            log.info(
                    "HTTP 요청 시작. method={}, uri={}",
                    request.getMethod(),
                    requestUri
            );
            filterChain.doFilter(request, response);
            completedNormally = true;
        } finally {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            int status = response.getStatus();
            String result = completedNormally && status < 400 ? "SUCCESS" : "FAILURE";
            if (elapsedMs >= SLOW_REQUEST_THRESHOLD_MS) {
                log.warn(
                        "HTTP 요청 종료. method={}, uri={}, status={}, result={}, elapsedMs={}, "
                                + "thresholdMs={}",
                        request.getMethod(),
                        requestUri,
                        status,
                        result,
                        elapsedMs,
                        SLOW_REQUEST_THRESHOLD_MS
                );
            } else {
                log.info(
                        "HTTP 요청 종료. method={}, uri={}, status={}, result={}, elapsedMs={}",
                        request.getMethod(),
                        requestUri,
                        status,
                        result,
                        elapsedMs
                );
            }
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    private boolean isSafeRequestId(String requestId) {
        return StringUtils.hasText(requestId)
                && SAFE_REQUEST_ID.matcher(requestId).matches();
    }

    private String sanitizeUri(String uri) {
        if (uri == null) {
            return "";
        }
        String sanitized = uri.replace('\r', '_').replace('\n', '_');
        if (sanitized.length() <= MAX_URI_LOG_LENGTH) {
            return sanitized;
        }
        return sanitized.substring(0, MAX_URI_LOG_LENGTH) + "...";
    }
}
