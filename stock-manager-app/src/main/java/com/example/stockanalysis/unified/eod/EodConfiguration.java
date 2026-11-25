package com.example.stockanalysis.unified.eod;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.*;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.*;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(EodProperties.class)
public class EodConfiguration {
    @Bean(name="eodHeartbeatExecutor", destroyMethod="shutdownNow")
    public ScheduledExecutorService heartbeatExecutor() {
        return Executors.newSingleThreadScheduledExecutor(r -> { var t=new Thread(r,"eod-lease-heartbeat"); t.setDaemon(true); return t; });
    }
    @Bean
    @ConditionalOnProperty(prefix="unified.eod",name="worker-enabled",havingValue="true",matchIfMissing=true)
    public Worker worker(EodCoordinator coordinator, @Qualifier("unifiedScanExecutor") Executor executor, java.time.Clock clock) { return new Worker(coordinator, executor, clock); }
    public static final class Worker {
        private final EodCoordinator coordinator;
        private final Executor executor;
        private final java.time.Clock clock;
        private final AtomicBoolean queued=new AtomicBoolean();
        private volatile boolean ready;
        Worker(EodCoordinator coordinator, Executor executor, java.time.Clock clock) {
            this.coordinator = coordinator;
            this.executor = executor;
            this.clock = clock;
        }
        @EventListener(ApplicationReadyEvent.class) public void start() {
            ready = true;
            if (EodExecutionDays.isWeekday(clock)) poll();
            else pollPreviousCloseOnWeekend();
        }
        @Scheduled(fixedDelayString="${unified.eod.poll-ms:1000}",initialDelayString="${unified.eod.poll-ms:1000}")
        public void poll() { queue(true); }
        @Scheduled(fixedDelayString="${unified.eod.poll-ms:1000}", initialDelayString="${unified.eod.poll-ms:1000}")
        public void pollPreviousCloseOnWeekend() { queue(false); }

        private void queue(boolean weekday) {
            if (!ready || EodExecutionDays.isWeekday(clock) != weekday || !queued.compareAndSet(false, true)) return;
            try { executor.execute(() -> { try { if (EodExecutionDays.isWeekday(clock) == weekday) coordinator.tick(); } finally { queued.set(false); } }); }
            catch (RejectedExecutionException e) { queued.set(false); }
        }
    }
}
