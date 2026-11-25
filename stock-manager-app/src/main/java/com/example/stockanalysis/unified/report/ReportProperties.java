package com.example.stockanalysis.unified.report;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("unified.report")
public record ReportProperties(@DefaultValue("false") boolean enabled,
        // Retained only to deserialize historical policy snapshots; no longer a publication gate.
        @DefaultValue("0.90") BigDecimal minimumCoverage,
        @DefaultValue("3000") BigDecimal minimumDailyAmountMillion,
        @DefaultValue("1500") BigDecimal minimumAverageAmountMillion,
        @DefaultValue("1.5") BigDecimal minimumNetRewardRisk,
        @DefaultValue("40") BigDecimal roundTripCostBps,
        @DefaultValue("10") BigDecimal maximumEma20Distance,
        @DefaultValue("20") int maximumCandidates) {
    public ReportProperties {
        if (minimumCoverage == null || minimumCoverage.signum()<=0 || minimumCoverage.compareTo(BigDecimal.ONE)>0
                || minimumDailyAmountMillion==null || minimumDailyAmountMillion.signum()<0
                || minimumAverageAmountMillion==null || minimumAverageAmountMillion.signum()<0
                || minimumNetRewardRisk==null || minimumNetRewardRisk.signum()<=0
                || roundTripCostBps==null || roundTripCostBps.signum()<0 || roundTripCostBps.compareTo(new BigDecimal("500"))>0
                || maximumEma20Distance==null || maximumEma20Distance.signum()<=0 || maximumCandidates<1 || maximumCandidates>100)
            throw new IllegalArgumentException("Invalid daily report settings");
    }
}
