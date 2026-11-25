package com.example.stockanalysis.unified.performance;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("unified.performance")
public record PerformanceProperties(@DefaultValue("false") boolean enabled,
        @DefaultValue("30") BigDecimal roundTripFeeBps,
        @DefaultValue("5") BigDecimal slippageBps,
        @DefaultValue("0.3") BigDecimal neutralBandPercent) {
    public PerformanceProperties {
        if (roundTripFeeBps==null || roundTripFeeBps.signum()<0 || roundTripFeeBps.compareTo(new BigDecimal("500"))>0
                || slippageBps==null || slippageBps.signum()<0 || slippageBps.compareTo(new BigDecimal("100"))>0
                || neutralBandPercent==null || neutralBandPercent.signum()<0 || neutralBandPercent.compareTo(new BigDecimal("5"))>0)
            throw new IllegalArgumentException("Invalid performance policy");
    }
}
