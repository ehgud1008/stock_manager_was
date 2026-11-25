package com.example.stockanalysis.unified.performance;

import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.engine.model.TradeScenario;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class ScenarioMonitoringModels {
    private ScenarioMonitoringModels() {}
    public record Day(LocalDate date,String quality,DailyCandle candle,boolean targetTouched,boolean stopTouched,
                      String status,String sourceRunId,Instant collectedAt,String note) {}
    public record Stock(String code,String name,String market,String disposition,String strategy,TradeScenario scenario,
                        BigDecimal selectionClose,String status,String dataState,LocalDate entryDate,LocalDate eventDate,
                        LocalDate latestPriceDate,BigDecimal latestClose,BigDecimal selectionReturnPercent,
                        BigDecimal targetDistancePercent,BigDecimal stopDistancePercent,int observedDays,int expectedDays,
                        List<Day> history,List<String> notes,Outcome outcome,SelectionOutcome selectionOutcome) {}
    public record SelectionOutcome(String policyVersion,String result,LocalDate eventDate,LocalDate throughDate,
                                   BigDecimal eventCloseReturnPercent,String summary) {}
    public record Outcome(String policyVersion,String result,String summary,LocalDate entryDate,BigDecimal entryPrice,
                          LocalDate exitDate,BigDecimal exitPrice,String exitReason,BigDecimal grossReturnPercent,
                          BigDecimal netReturnPercent,BigDecimal markedReturnPercent,BigDecimal conservativeReturnPercent,
                          int holdingDays,BigDecimal roundTripFeeBps,BigDecimal slippageBps) {}
    public record View(String state,String runId,LocalDate selectionDate,LocalDate firstTrackingDate,LocalDate lastTrackingDate,
                       Instant checkedAt,List<Stock> stocks,List<String> notes) {}
    public record Selection(String runId,LocalDate selectionDate,LocalDate firstTrackingDate,LocalDate lastTrackingDate,
                            String code,String name,Stock stock,String errorCode) {}
    public record Overview(LocalDate from,LocalDate to,int page,int size,long totalElements,boolean hasMore,
                           Instant checkedAt,List<Selection> rows,List<String> notes,boolean deletionEnabled) {}
}
