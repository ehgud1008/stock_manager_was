package com.example.stockanalysis.unified.config;

import com.example.stockanalysis.unified.api.UnifiedStockAnalysisEngine;
import com.example.stockanalysis.unified.internal.DefaultUnifiedStockAnalysisEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class UnifiedAnalysisConfiguration {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultUnifiedStockAnalysisEngine.class);
    @Bean(name = "unifiedScanExecutor", destroyMethod = "shutdownNow")
    public java.util.concurrent.ExecutorService unifiedScanExecutor() {
        return new java.util.concurrent.ThreadPoolExecutor(1, 1, 0, java.util.concurrent.TimeUnit.MILLISECONDS,
                new java.util.concurrent.ArrayBlockingQueue<>(1), task -> {
                    var thread = new Thread(task, "unified-analysis-worker");
                    thread.setDaemon(true);
                    return thread;
                }, new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
    }

    @Bean
    public UnifiedStockAnalysisEngine unifiedStockAnalysisEngine() {
        return new DefaultUnifiedStockAnalysisEngine(com.example.stockanalysis.engine.screener.ScreenerConfig.defaults(),
                log::isDebugEnabled, t -> log.debug(
                        "Unified engine stages: stockCode={}, prepareMs={}, screenerMs={}, suitabilityMs={}, signalMs={}, dailyBars={}, weeklyBars={}, signalCount={}, completeness={}, scoreAvailable={}, suitabilityStatus={}",
                        t.stockCode(), t.prepareNanos()/1_000_000.0, t.screenerNanos()/1_000_000.0,
                        t.suitabilityNanos()/1_000_000.0, t.signalNanos()/1_000_000.0,
                        t.dailyBars(), t.weeklyBars(), t.signalCount(), t.completeness(), t.scoreAvailable(), t.suitabilityStatus()));
    }
}
