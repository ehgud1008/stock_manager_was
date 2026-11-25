package com.example.stockanalysis.shared.logging;

import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;

public final class SensitiveLogSanitizer {

    private static final String REDACTED_MESSAGE = "[REDACTED_SENSITIVE_MESSAGE]";
    private static final String[] SENSITIVE_KEYWORDS = {
        "password", "passwd", "secret", "token", "authorization", "session",
        "주민등록", "계좌", "카드"
    };

    private SensitiveLogSanitizer() {
    }

    public static Throwable sanitize(Throwable throwable) {
        return sanitize(throwable, new IdentityHashMap<>());
    }

    private static Throwable sanitize(
            Throwable throwable,
            Map<Throwable, Throwable> sanitized
    ) {
        if (throwable == null) {
            return null;
        }
        if (sanitized.containsKey(throwable)) {
            return sanitized.get(throwable);
        }

        String originalType = throwable.getClass().getName();
        String safeMessage = sanitizeMessage(throwable.getMessage());
        RuntimeException copy = new RuntimeException(
                "originalType=" + originalType + ", message=" + safeMessage
        );
        sanitized.put(throwable, copy);
        copy.setStackTrace(throwable.getStackTrace());

        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            copy.initCause(sanitize(cause, sanitized));
        }
        for (Throwable suppressed : throwable.getSuppressed()) {
            copy.addSuppressed(sanitize(suppressed, sanitized));
        }
        return copy;
    }

    private static String sanitizeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "(no message)";
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        for (String keyword : SENSITIVE_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return REDACTED_MESSAGE;
            }
        }
        return message;
    }
}
