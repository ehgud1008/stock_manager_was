package com.example.stockanalysis.unified.eod;

import static org.assertj.core.api.Assertions.*;
import com.example.stockanalysis.kiwoom.client.KiwoomApiException;
import org.junit.jupiter.api.Test;

class EodDiagnosticsTest {
    @Test void missingFailureCodeKeepsHttpRetryPolicyAndDoesNotThrow() {
        assertThat(EodCoordinator.retryable(new KiwoomApiException("unused",429,null))).isTrue();
        assertThat(EodCoordinator.retryable(new KiwoomApiException("unused",503,null))).isTrue();
        assertThat(EodCoordinator.retryable(new KiwoomApiException("unused",400,null))).isFalse();
        assertThat(EodCoordinator.retryable(new KiwoomApiException("unused",null,null))).isFalse();
    }
    @Test void diagnosticsContainSourceLocationsButNotExceptionMessages() {
        var cause=new NullPointerException("secret-token");
        cause.setStackTrace(new StackTraceElement[]{new StackTraceElement("example.Reader","read","Reader.java",42)});
        var error=new IllegalStateException("request-body-secret",cause);
        assertThat(EodCoordinator.safeTrace(error)).contains("NullPointerException","Reader.java:42")
                .doesNotContain("secret-token","request-body-secret");
    }
}
