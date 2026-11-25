package com.example.stockanalysis.screener.config;

import com.example.stockanalysis.engine.screener.ScreenerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("screener")
public record ScreenerProperties(Long minCallIntervalMillis, Integer maxPages, Integer maxRetries,
                                  Integer queueCapacity, ScreenerConfig calculation) {
    public ScreenerProperties {
        minCallIntervalMillis = minCallIntervalMillis == null ? 350L : minCallIntervalMillis;
        maxPages = maxPages == null ? 60 : maxPages;
        maxRetries = maxRetries == null ? 2 : maxRetries;
        queueCapacity = queueCapacity == null ? 2 : queueCapacity;
        calculation = calculation == null ? ScreenerConfig.defaults() : calculation;
        if (minCallIntervalMillis < 0 || minCallIntervalMillis > 60000 || maxPages < 1 || maxPages > 1000
                || maxRetries < 0 || maxRetries > 5 || queueCapacity < 1 || queueCapacity > 20) throw new IllegalArgumentException("invalid screener settings");
    }
}
