package com.example.stockanalysis.kiwoom.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.stockanalysis.kiwoom.client.KiwoomErrorDecoder;
import com.example.stockanalysis.kiwoom.config.KiwoomProperties;
import com.example.stockanalysis.kiwoom.contract.tr.au10001.Au10001Request;
import com.example.stockanalysis.kiwoom.contract.tr.au10001.Au10001Response;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class KiwoomTokenProviderTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void usesActiveMockCredentialsAndCachesToken() {
        KiwoomAuthHttpApi authHttpApi = Mockito.mock(KiwoomAuthHttpApi.class);
        KiwoomProperties properties = new KiwoomProperties();
        properties.setUseMock(true);
        properties.setMockAppKey("mock-app-key");
        properties.setMockSecretKey("mock-secret-key");
        properties.setTokenRefreshMargin(Duration.ofSeconds(60));

        when(authHttpApi.issueToken(Mockito.any()))
                .thenReturn(Au10001Response.builder()
                        .expiresDt("20991231235959")
                        .tokenType("bearer")
                        .token("access-token")
                        .returnCode(0)
                        .returnMsg("정상처리")
                        .build());

        KiwoomTokenProvider provider = new KiwoomTokenProvider(
                authHttpApi,
                properties,
                new KiwoomErrorDecoder()
        );

        MDC.put("apiId", "ka10001");
        assertThat(provider.getAccessToken()).isEqualTo("access-token");
        assertThat(MDC.get("apiId")).isEqualTo("ka10001");
        assertThat(provider.getAccessToken()).isEqualTo("access-token");

        ArgumentCaptor<Au10001Request> requestCaptor =
                ArgumentCaptor.forClass(Au10001Request.class);
        verify(authHttpApi, times(1)).issueToken(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getAppkey()).isEqualTo("mock-app-key");
        assertThat(requestCaptor.getValue().getSecretkey()).isEqualTo("mock-secret-key");
    }
}
