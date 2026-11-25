package com.example.stockanalysis.engine.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AnalysisInput(
        StockSnapshot stock,
        AnalysisMode mode,
        LocalDate baseDate,
        Instant dataCutoffAt,
        List<DailyCandle> dailyCandles,
        List<DailyCandle> weeklyCandles,
        List<DailyCandle> monthlyCandles,
        Map<String, BigDecimal> observations,
        PositionContext position,
        LocalDate confirmedThrough
) {

    public AnalysisInput {
        Objects.requireNonNull(stock, "stock");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(baseDate, "baseDate");
        Objects.requireNonNull(dataCutoffAt, "dataCutoffAt");
        if (confirmedThrough != null && (confirmedThrough.isAfter(baseDate)
                || confirmedThrough.isAfter(dataCutoffAt.atZone(java.time.ZoneId.of("Asia/Seoul")).toLocalDate())))
            throw new IllegalArgumentException("confirmedThrough exceeds input cutoff");
        dailyCandles = List.copyOf(Objects.requireNonNull(dailyCandles, "dailyCandles"));
        weeklyCandles = List.copyOf(Objects.requireNonNull(weeklyCandles, "weeklyCandles"));
        monthlyCandles = List.copyOf(Objects.requireNonNull(monthlyCandles, "monthlyCandles"));
        observations = Map.copyOf(Objects.requireNonNull(observations, "observations"));
        if (position != null && position.held() && position.openedAt().isAfter(dataCutoffAt))
            throw new IllegalArgumentException("Position opens after data cutoff");
        if (dailyCandles.isEmpty()) {
            throw new IllegalArgumentException("dailyCandles must not be empty");
        }
        if (containsFutureData(dailyCandles, baseDate)
                || containsFutureData(weeklyCandles, baseDate)
                || containsFutureData(monthlyCandles, baseDate)) {
            throw new IllegalArgumentException("candles must not contain future data");
        }
    }

    public AnalysisInput(
            StockSnapshot stock, AnalysisMode mode, LocalDate baseDate, Instant dataCutoffAt,
            List<DailyCandle> dailyCandles, List<DailyCandle> weeklyCandles, List<DailyCandle> monthlyCandles,
            Map<String, BigDecimal> observations, PositionContext position
    ) { this(stock, mode, baseDate, dataCutoffAt, dailyCandles, weeklyCandles, monthlyCandles, observations, position, null); }

    public AnalysisInput(
            StockSnapshot stock, AnalysisMode mode, LocalDate baseDate, Instant dataCutoffAt,
            List<DailyCandle> dailyCandles, List<DailyCandle> weeklyCandles, List<DailyCandle> monthlyCandles,
            Map<String, BigDecimal> observations
    ) { this(stock, mode, baseDate, dataCutoffAt, dailyCandles, weeklyCandles, monthlyCandles, observations, null); }

    public AnalysisInput(
            StockSnapshot stock,
            AnalysisMode mode,
            LocalDate baseDate,
            Instant dataCutoffAt,
            List<DailyCandle> dailyCandles,
            Map<String, BigDecimal> observations
    ) {
        this(
                stock,
                mode,
                baseDate,
                dataCutoffAt,
                dailyCandles,
                List.of(),
                List.of(),
                observations
        );
    }

    public List<DailyCandle> candles(AnalysisTimeframe timeframe) {
        return switch (Objects.requireNonNull(timeframe, "timeframe")) {
            case DAILY -> dailyCandles;
            case WEEKLY -> weeklyCandles;
            case MONTHLY -> monthlyCandles;
        };
    }

    private static boolean containsFutureData(List<DailyCandle> candles, LocalDate baseDate) {
        return candles.stream().anyMatch(candle -> candle.businessDate().isAfter(baseDate));
    }
}
