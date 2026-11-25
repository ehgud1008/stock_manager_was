package com.example.stockanalysis.shared.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SensitiveLogSanitizerTest {

    @Test
    void redactsSensitiveExceptionMessageAndPreservesOrigin() {
        IllegalStateException original = new IllegalStateException("database-password-leaked");

        Throwable sanitized = SensitiveLogSanitizer.sanitize(original);

        assertThat(sanitized.getMessage())
                .contains("originalType=java.lang.IllegalStateException")
                .contains("[REDACTED_SENSITIVE_MESSAGE]")
                .doesNotContain("database-password-leaked");
        assertThat(sanitized.getStackTrace()).isEqualTo(original.getStackTrace());
    }

    @Test
    void keepsNonSensitiveExceptionMessage() {
        IllegalArgumentException original = new IllegalArgumentException("connection timed out");

        Throwable sanitized = SensitiveLogSanitizer.sanitize(original);

        assertThat(sanitized.getMessage()).contains("connection timed out");
    }
}
