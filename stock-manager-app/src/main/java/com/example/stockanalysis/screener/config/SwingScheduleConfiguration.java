package com.example.stockanalysis.screener.config;

import com.example.stockanalysis.screener.application.ScreenerService;
import com.example.stockanalysis.screener.application.SwingScreenerScheduler;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "screener.swing-schedule", name = "enabled", havingValue = "true")
@EnableScheduling
@EnableConfigurationProperties(SwingScheduleProperties.class)
public class SwingScheduleConfiguration {
    @Bean public SwingScreenerScheduler swingScreenerScheduler(ScreenerService service, SwingScheduleProperties properties, Clock clock) {
        return new SwingScreenerScheduler(service, properties, clock);
    }
}
