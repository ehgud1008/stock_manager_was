package com.example.stockanalysis.backtest;

import com.example.stockanalysis.engine.model.DailyCandle;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SwingBacktestModels {
    private SwingBacktestModels() {}

    public static final String STRATEGY = "20_DAY_BREAKOUT_V1";
    public static final int SESSIONS = 25;

    public record Request(List<String> stockCodes, LocalDate from, LocalDate to,
                          int holdingDays, BigDecimal capital, int maxPositions,
                          int feeBps, int slippageBps) {}

    public record Run(UUID id, String strategy, Request settings, String calendarVersion,
                      List<LocalDate> warmup, List<LocalDate> sessions, List<LocalDate> followUp,
                      Instant createdAt) {}

    public record Frame(Instant capturedAt, Map<String, List<DailyCandle>> histories) {}

    public record Signal(LocalDate date, String stockCode, BigDecimal close, BigDecimal breakoutPrice) {}

    public record Trade(Signal signal, LocalDate entryDate, BigDecimal entryPrice, Long quantity,
                        LocalDate exitDate, BigDecimal exitPrice, BigDecimal netPnl, String status) {}

    public record Equity(LocalDate date, BigDecimal value) {}

    public record Coverage(LocalDate date, String status, List<String> unavailableStocks) {}

    public record Summary(int selected, int entered, int closed, int wins, int open,
                          BigDecimal winRate, BigDecimal averageNetPnl,
                          BigDecimal returnPercent, BigDecimal drawdownPercent) {}

    public record Result(Run run, String mode, String status, LocalDate asOf, int settledSessions,
                         int capturedSessions, Summary summary, List<Equity> equity,
                         List<Trade> trades, List<Trade> followUpTrades, List<Coverage> coverage) {}

    public record PaperStatus(boolean enabled, List<Run> runs) {}
}
