package com.example.stockanalysis.unified.eod;

import java.time.Duration;
import java.time.LocalTime;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("unified.eod")
public record EodProperties(
        @DefaultValue("classpath:unified-calendar-2026.csv") String calendar,
        @DefaultValue("PT8H") Duration catchupWindow,
        @DefaultValue("PT10M") Duration settlementDelay,
        @DefaultValue("PT30S") Duration stabilityInterval,
        @DefaultValue("PT90M") Duration readinessTimeout,
        @DefaultValue("PT12H") Duration runTimeout,
        @DefaultValue("PT1M") Duration retryDelay,
        @DefaultValue("3") int maxAttempts,
        @DefaultValue("PT2M") Duration leaseDuration) {
    public static final String PRICE_BASIS = "KIWOOM_KRX_DAILY_ADJUSTED";
    public static final String POLICY = "EOD_TIME_AND_STABLE_DAILY_V1";
    public static final LocalTime START = LocalTime.of(20, 0);
    public EodProperties {
        for (Duration d : new Duration[]{catchupWindow, settlementDelay, stabilityInterval, readinessTimeout, runTimeout, retryDelay, leaseDuration})
            if (d == null || d.isNegative() || d.isZero()) throw new IllegalArgumentException("EOD durations must be positive");
        if (catchupWindow.compareTo(Duration.ofHours(24)) >= 0 || runTimeout.compareTo(Duration.ofHours(24)) >= 0
                || maxAttempts < 1 || maxAttempts > 10 || leaseDuration.compareTo(Duration.ofSeconds(60)) < 0
                || readinessTimeout.compareTo(stabilityInterval) <= 0)
            throw new IllegalArgumentException("Invalid EOD limits");
    }
}
