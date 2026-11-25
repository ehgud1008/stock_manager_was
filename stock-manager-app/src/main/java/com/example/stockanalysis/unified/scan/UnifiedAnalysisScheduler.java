package com.example.stockanalysis.unified.scan;

import com.example.stockanalysis.unified.eod.EodCoordinator;
import com.example.stockanalysis.unified.eod.EodExecutionDays;
import java.time.Clock;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;

/** Registration only. The durable worker also handles manual runs when scheduling is off. */
public final class UnifiedAnalysisScheduler {
    private final EodCoordinator coordinator;
    private final Clock clock;
    private volatile boolean ready;
    public UnifiedAnalysisScheduler(EodCoordinator coordinator,Clock clock) { this.coordinator = coordinator;this.clock=clock; }
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() { ready = true; runScheduledAnalysis(); }
    @Scheduled(cron = "${unified.analysis-schedule.cron:0 0 20 * * MON-FRI}", zone = "Asia/Seoul")
    public void runScheduledAnalysis() {
        if (!ready || !EodExecutionDays.isWeekday(clock)) return;
        try { coordinator.ensureScheduled(); }
        catch (RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("EOD registration unavailable; retry on next check: type={}", e.getClass().getSimpleName());
        }
    }
    @Scheduled(cron = "0 * * * * MON-FRI", zone = "Asia/Seoul")
    public void catchUp() { runScheduledAnalysis(); }
}
