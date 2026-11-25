package com.example.stockanalysis.screener.application;

import com.example.stockanalysis.screener.config.SwingScheduleProperties;
import java.time.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;

/** Single-WAS scheduler. Runs on weekdays, using persisted daily request keys, not an in-memory last-run date. */
public final class SwingScreenerScheduler {
    private static final Logger log = LoggerFactory.getLogger(SwingScreenerScheduler.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final ScreenerService service;
    private final SwingScheduleProperties properties;
    private final Clock clock;
    private volatile boolean ready;
    public SwingScreenerScheduler(ScreenerService service, SwingScheduleProperties properties, Clock clock) {
        this.service = service; this.properties = properties; this.clock = clock;
    }
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() { ready = true; checkSchedule(); }

    // Check each minute so startup during the morning window also catches up a missed 04:00 trigger.
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void checkSchedule() {
        if (!ready) return;
        var now = LocalDateTime.now(clock.withZone(SEOUL));
        var day = now.toLocalDate();
        if (day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY
                || properties.excludedDates().contains(day) || now.toLocalTime().isBefore(properties.startTime())
                || !now.toLocalTime().isBefore(properties.latestStartTime())) return;
        try { service.ensureScheduledSwing(day.atTime(properties.startTime())); }
        catch (RuntimeException e) {
            // Do not print exception messages: provider/DB errors may contain connection details.
            log.error("Scheduled swing screening could not be registered: date={}, type={}; check DB and run status", day, e.getClass().getSimpleName());
        }
    }
}
