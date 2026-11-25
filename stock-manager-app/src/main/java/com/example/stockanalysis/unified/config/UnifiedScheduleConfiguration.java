package com.example.stockanalysis.unified.config;

import com.example.stockanalysis.unified.scan.UnifiedAnalysisScheduler;
import com.example.stockanalysis.unified.eod.EodCoordinator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "unified.analysis-schedule", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableScheduling
public class UnifiedScheduleConfiguration {
    @Bean
    public UnifiedAnalysisScheduler unifiedAnalysisScheduler(EodCoordinator coordinator,java.time.Clock clock) {
        return new UnifiedAnalysisScheduler(coordinator,clock);
    }
}
