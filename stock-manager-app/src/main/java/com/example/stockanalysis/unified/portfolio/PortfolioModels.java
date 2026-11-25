package com.example.stockanalysis.unified.portfolio;

import com.example.stockanalysis.unified.performance.PerformanceModels.*;
import com.example.stockanalysis.unified.report.ReportModels.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;

public final class PortfolioModels {
    private PortfolioModels() {}
    public record Request(
            @Schema(description = "평가할 보고서 ID 목록. 중복 없이 1~60개", requiredMode = Schema.RequiredMode.REQUIRED) List<String> reportIds,
            @Schema(description = "초기 모의 자금 (원). 소수 2자리까지", minimum = "10000", maximum = "100000000000", example = "10000000", requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal initialCash,
            @Schema(description = "최대 동시 보유 종목 수", minimum = "1", maximum = "50", example = "10", requiredMode = Schema.RequiredMode.REQUIRED) int maximumPositions,
            @Schema(description = "종목당 투자 비중 (%)", minimum = "0", exclusiveMinimum = true, maximum = "100", example = "10", requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal positionPercent) {
        public Request {
            if(reportIds==null || reportIds.isEmpty() || reportIds.size()>60 || reportIds.stream().anyMatch(Objects::isNull)
                    || new HashSet<>(reportIds).size()!=reportIds.size()
                    || initialCash==null || initialCash.compareTo(new BigDecimal("10000"))<0 || initialCash.compareTo(new BigDecimal("100000000000"))>0
                    || initialCash.scale()>2 || maximumPositions<1 || maximumPositions>50 || positionPercent==null
                    || positionPercent.signum()<=0 || positionPercent.compareTo(new BigDecimal("100"))>0)
                throw new IllegalArgumentException("모의 자금·보유 한도와 중복 없는 보고서 1~60개를 확인하세요.");
            reportIds=List.copyOf(reportIds);
        }
    }
    public record Policy(String version, BigDecimal initialCash, int maximumPositions, BigDecimal positionPercent,
                         BigDecimal roundTripFeeBps, BigDecimal slippageBps, BigDecimal neutralBandPercent,
                         int holdingDays, String calendarVersion) {}
    public record Cohort(DailyReport report, List<StockDecision> baseline, List<StockDecision> withoutMarketFilter,
                         List<Anchor> anchors, Map<String,List<StockDecision>> comparisons,
                         Map<String,String> unavailableComparisons, SelectionDiagnostics.Audit audit) {
        public Cohort(DailyReport report,List<StockDecision> baseline,List<StockDecision> withoutMarketFilter,List<Anchor> anchors) {
            this(report,baseline,withoutMarketFilter,anchors,Map.of(),Map.of(),null);
        }
        public Cohort {
            baseline=List.copyOf(baseline);withoutMarketFilter=List.copyOf(withoutMarketFilter);anchors=List.copyOf(anchors);
            var copy=new TreeMap<String,List<StockDecision>>();if(comparisons!=null)comparisons.forEach((k,v)->copy.put(k,List.copyOf(v)));
            comparisons=Collections.unmodifiableMap(copy);
            unavailableComparisons=unavailableComparisons==null?Map.of():Map.copyOf(unavailableComparisons);
        }
    }
    public record Input(Policy policy, List<Cohort> cohorts, List<LocalDate> dates, Instant registeredAt,
                        String captureMode) {}
    public record Observed(String reportId, Observation observation) {
        public String key(){return reportId+":"+observation.key();}
    }
    public record Trade(String reportId, String code, String name, String strategy, String market, String marketBias,
                        String state, String reason, LocalDate entryDate, LocalDate exitDate, long quantity,
                        BigDecimal entryPrice, BigDecimal exitPrice, BigDecimal invested, BigDecimal netProfit,
                        BigDecimal netReturnPercent, BigDecimal markedProfit, boolean ambiguous,
                        Map<String,String> evidenceGroups) {}
    public record Point(LocalDate date, BigDecimal cash, BigDecimal equity, BigDecimal drawdownPercent, int holdings) {}
    public record Metrics(int orders, int entered, int closed, int wins, int losses, int flat, int open, int unresolved,
                          int skipped, BigDecimal winRatePercent, BigDecimal fillRatePercent,
                          BigDecimal meanReturnPercent, BigDecimal meanWinPercent, BigDecimal meanLossPercent,
                          BigDecimal realizedProfit, BigDecimal profitFactor, int consecutiveLosingExitDates,BigDecimal stopRatePercent) {
        public Metrics(int orders,int entered,int closed,int wins,int losses,int flat,int open,int unresolved,int skipped,
                       BigDecimal winRatePercent,BigDecimal fillRatePercent,BigDecimal meanReturnPercent,BigDecimal meanWinPercent,
                       BigDecimal meanLossPercent,BigDecimal realizedProfit,BigDecimal profitFactor,int consecutiveLosingExitDates) {
            this(orders,entered,closed,wins,losses,flat,open,unresolved,skipped,winRatePercent,fillRatePercent,meanReturnPercent,
                    meanWinPercent,meanLossPercent,realizedProfit,profitFactor,consecutiveLosingExitDates,null);
        }
    }
    public record Variant(String name, boolean marketFilter, BigDecimal slippageBps, String state,
                          BigDecimal finalEquity, BigDecimal returnPercent, BigDecimal maximumDrawdownPercent,
                          List<Point> curve, List<Trade> trades, Metrics metrics, Map<String,Metrics> groups) {}
    public record Forecast(String reportId, String market, LocalDate date, String predicted, String actual,
                           BigDecimal returnPercent, Boolean matched, String state) {}
    public record Result(String state, Instant evaluatedAt, LocalDate throughDate, List<Variant> variants,
                         List<Forecast> forecasts, List<String> limitations) {}
    public record Summary(String id,String state,Instant createdAt,int revision,String errorCode) {}
    public record View(String id,String state,String errorCode,int revision,Input input,Result result) {}
    public record Available(String id,LocalDate tradeDate,String scope,String version,int candidates) {}
}
