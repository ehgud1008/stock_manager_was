package com.example.stockanalysis.unified.model;

import com.example.stockanalysis.engine.model.PositionContext;
import com.example.stockanalysis.engine.screener.ScreenerBar;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** SWING daily bars only. All local times are Asia/Seoul; observations must match the cutoff. */
public record UnifiedAnalysisInput(
        String stockCode,
        String stockName,
        String marketCode,
        LocalDateTime asOf,
        List<ScreenerBar> dailyBars,
        Map<String, BigDecimal> observations,
        PositionContext position,
        java.time.LocalDate confirmedThrough
) {
    public UnifiedAnalysisInput {
        if (stockCode == null || !stockCode.matches("[0-9]{6}")) {
            throw new IllegalArgumentException("stockCode must contain exactly six digits");
        }
        if (stockName == null || stockName.isBlank() || marketCode == null || marketCode.isBlank()) {
            throw new IllegalArgumentException("stockName and marketCode must not be blank");
        }
        Objects.requireNonNull(asOf, "asOf");
        if (confirmedThrough != null && confirmedThrough.isAfter(asOf.toLocalDate()))
            throw new IllegalArgumentException("confirmedThrough exceeds asOf");
        dailyBars = List.copyOf(Objects.requireNonNull(dailyBars, "dailyBars"));
        observations = Map.copyOf(Objects.requireNonNull(observations, "observations"));
        if (dailyBars.isEmpty() || dailyBars.size() > 20_000) {
            throw new IllegalArgumentException("dailyBars must contain 1 to 20000 bars");
        }
        if (position != null && position.held()
                && position.openedAt().isAfter(asOf.atZone(ZoneId.of("Asia/Seoul")).toInstant())) {
            throw new IllegalArgumentException("position opens after asOf");
        }
    }

    public UnifiedAnalysisInput(String stockCode, String stockName, String marketCode,
                                LocalDateTime asOf, List<ScreenerBar> dailyBars,
                                Map<String, BigDecimal> observations, PositionContext position) {
        this(stockCode, stockName, marketCode, asOf, dailyBars, observations, position, null);
    }

    public UnifiedAnalysisInput(String stockCode, String stockName, String marketCode,
                                LocalDateTime asOf, List<ScreenerBar> dailyBars,
                                Map<String, BigDecimal> observations) {
        this(stockCode, stockName, marketCode, asOf, dailyBars, observations, null);
    }
}
