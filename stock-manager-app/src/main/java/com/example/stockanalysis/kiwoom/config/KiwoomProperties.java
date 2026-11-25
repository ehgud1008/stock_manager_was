package com.example.stockanalysis.kiwoom.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "kiwoom")
public class KiwoomProperties {

    @NotBlank
    private String baseUrl;

    @NotBlank
    private String mockBaseUrl;

    private boolean useMock;

    private String appKey;

    private String secretKey = "";

    private String mockAppKey = "";

    private String mockSecretKey = "";

    @NotBlank
    private String grantType = "client_credentials";

    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(3);

    @NotNull
    private Duration readTimeout = Duration.ofSeconds(10);

    @NotNull
    private Duration tokenRefreshMargin = Duration.ofSeconds(60);

    public String activeBaseUrl() {
        return useMock ? mockBaseUrl : baseUrl;
    }

    public String activeAppKey() {
        return useMock ? mockAppKey : appKey;
    }

    public String activeSecretKey() {
        return useMock ? mockSecretKey : secretKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getMockBaseUrl() {
        return mockBaseUrl;
    }

    public void setMockBaseUrl(String mockBaseUrl) {
        this.mockBaseUrl = mockBaseUrl;
    }

    public boolean isUseMock() {
        return useMock;
    }

    public void setUseMock(boolean useMock) {
        this.useMock = useMock;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getMockAppKey() {
        return mockAppKey;
    }

    public void setMockAppKey(String mockAppKey) {
        this.mockAppKey = mockAppKey;
    }

    public String getMockSecretKey() {
        return mockSecretKey;
    }

    public void setMockSecretKey(String mockSecretKey) {
        this.mockSecretKey = mockSecretKey;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Duration getTokenRefreshMargin() {
        return tokenRefreshMargin;
    }

    public void setTokenRefreshMargin(Duration tokenRefreshMargin) {
        this.tokenRefreshMargin = tokenRefreshMargin;
    }
}
