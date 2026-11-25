package com.example.stockanalysis.unified.eod;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/** Versioned local exchange calendar; outside coverage is UNKNOWN, never an assumed weekday. */
@Component
public class TradingCalendar {
    public enum Status { OPEN, CLOSED, UNKNOWN }
    public record Day(LocalDate date, Status status, LocalTime closeTime, String version) {}
    private final EodProperties properties;
    private final ResourceLoader resources;
    public TradingCalendar(EodProperties properties, ResourceLoader resources) { this.properties = properties; this.resources = resources; }
    public Day day(LocalDate date) {
        try (var input = resources.getResource(properties.calendar()).getInputStream();
             var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            LocalDate from = null, to = null; String version = null;
            Set<LocalDate> closed = new HashSet<>(); Map<LocalDate, LocalTime> sessions = new HashMap<>();
            for (String line : reader.lines().toList()) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] fields = line.trim().split(",");
                switch (fields[0]) {
                    case "coverage" -> { from = LocalDate.parse(fields[1]); to = LocalDate.parse(fields[2]); }
                    case "version" -> version = fields[1];
                    case "closed" -> closed.add(LocalDate.parse(fields[1]));
                    case "session" -> sessions.put(LocalDate.parse(fields[1]), LocalTime.parse(fields[2]));
                    default -> throw new IllegalArgumentException("Unknown calendar entry");
                }
            }
            if (from == null || to == null || version == null || from.isAfter(to) || date.isBefore(from) || date.isAfter(to))
                return new Day(date, Status.UNKNOWN, null, version);
            boolean holiday = closed.contains(date) || date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
            return new Day(date, holiday ? Status.CLOSED : Status.OPEN, sessions.getOrDefault(date, EodProperties.START), version);
        } catch (IOException | RuntimeException error) { return new Day(date, Status.UNKNOWN, null, "UNAVAILABLE"); }
    }
    public Day latestDue(LocalDateTime now) {
        for (int i = 0; i < 32; i++) {
            var date = now.toLocalDate().minusDays(i);
            if (date.atTime(EodProperties.START).isAfter(now)) continue;
            var day = day(date);
            if (day.status() != Status.CLOSED) return day;
        }
        return new Day(now.toLocalDate(), Status.UNKNOWN, null, "UNAVAILABLE");
    }
}
