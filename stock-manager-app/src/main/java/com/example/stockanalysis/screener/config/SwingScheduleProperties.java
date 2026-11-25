package com.example.stockanalysis.screener.config;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("screener.swing-schedule")
public record SwingScheduleProperties(LocalTime startTime, LocalTime latestStartTime, List<LocalDate> excludedDates) {
    public SwingScheduleProperties {
        startTime = startTime == null ? LocalTime.of(4, 0) : startTime;
        latestStartTime = latestStartTime == null ? LocalTime.of(8, 0) : latestStartTime;
        excludedDates = excludedDates == null ? List.of() : List.copyOf(excludedDates);
        if (!startTime.isBefore(latestStartTime)) throw new IllegalArgumentException("swing schedule start-time must precede latest-start-time");
    }
}
