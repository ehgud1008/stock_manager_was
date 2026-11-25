package com.example.stockanalysis.unified.performance;

import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.unified.report.ReportModels.DailyReport;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class PerformanceModels {
    private PerformanceModels() {}
    public record Policy(String version, BigDecimal roundTripFeeBps, BigDecimal slippageBps,
                         BigDecimal neutralBandPercent, int holdingDays, String calendarVersion) {}
    public record Anchor(String code, String market, String priceBasis, DailyCandle candle) {}
    public record Input(DailyReport report, Policy policy, List<LocalDate> dates,
                        List<Anchor> anchors, String captureMode, Instant registeredAt) {}
    public record Observation(String kind, String code, LocalDate date, String quality,
                              DailyCandle candle, String sourceRunId, Instant collectedAt, String detail) {
        public String key() { return kind+":"+code+":"+date; }
    }
    public record Horizon(int days, String state, BigDecimal priceReturnPercent,
                          String marketState, BigDecimal marketReturnPercent, BigDecimal excessReturnPercent,
                          String pathState, BigDecimal maximumRisePercent, BigDecimal maximumFallPercent) {
        public Horizon(int days,String state,BigDecimal priceReturnPercent) {
            this(days,state,priceReturnPercent,null,null,null,null,null,null);
        }
    }
    public record Trade(String state, LocalDate entryDate, BigDecimal entryPrice, LocalDate exitDate,
                        BigDecimal exitPrice, String exitReason, BigDecimal netReturnPercent,
                        BigDecimal markedReturnPercent, boolean ambiguousBar, List<BigDecimal> equityPath) {}
    public record StockResult(String code, String name, String strategy, String disposition,
                              List<Horizon> horizons, Trade trade) {}
    public record ForecastResult(String market, String predicted, LocalDate date, String state,
                                 BigDecimal returnPercent, String actual, Boolean matched) {}
    public record Metrics(int candidates, int eligible, int entered, int closed, int wins, int losses,
                          int flat, int open, int unresolved, int noEntry, BigDecimal winRatePercent,
                          BigDecimal fillRatePercent, BigDecimal meanClosedReturnPercent,
                          BigDecimal meanWinPercent, BigDecimal meanLossPercent, BigDecimal profitFactor,
                          int maxConsecutiveLosingExitDates, BigDecimal cohortReturnPercent, BigDecimal cohortMaxDrawdownPercent) {}
    public record Result(String runId, String state, String reportVersion, Policy policy, String captureMode,
                         Instant evaluatedAt, LocalDate throughDate, List<StockResult> stocks,
                         List<ForecastResult> forecasts, Metrics metrics, Map<String,Metrics> byStrategy,
                         List<String> limitations) {}
    public record View(String state, String errorCode, int revision, Result result) {}
}
