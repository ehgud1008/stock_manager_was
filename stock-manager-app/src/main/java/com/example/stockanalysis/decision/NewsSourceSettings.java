package com.example.stockanalysis.decision;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record NewsSourceSettings(
        @Value("${news-insights.naver-client-id:}") String naverClientId,
        @Value("${news-insights.naver-client-secret:}") String naverClientSecret,
        @Value("${news-insights.dart-api-key:}") String dartApiKey) {
    public boolean naverConfigured() { return !naverClientId.isBlank() && !naverClientSecret.isBlank(); }
    @Override public String toString() { return "NewsSourceSettings[naverConfigured="+naverConfigured()+",dartConfigured="+!dartApiKey.isBlank()+"]"; }
}
