package com.example.stockanalysis.backtest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "swing.backtest.paper-enabled", havingValue = "true")
class SwingBacktestWorker {
    private static final Logger log = LoggerFactory.getLogger(SwingBacktestWorker.class);
    private final SwingBacktestService service;
    private final AtomicBoolean active = new AtomicBoolean();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    SwingBacktestWorker(SwingBacktestService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 */5 20-23 * * *", zone = "Asia/Seoul")
    void capture() {
        if (executor.isShutdown() || !active.compareAndSet(false, true)) return;
        executor.execute(() -> {
            try {
                service.captureToday();
            } catch (RuntimeException error) {
                log.warn("Swing paper capture will retry", error);
            } finally {
                active.set(false);
            }
        });
    }

    @PreDestroy
    void stop() {
        executor.shutdownNow();
    }
}
