package com.example.stockanalysis.unified.report;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import com.example.stockanalysis.backdata.validation.model.BackdataRecord;
import com.example.stockanalysis.engine.model.TradeScenario;

public final class ReportModels {
    private ReportModels() {}
    public record IndexBar(LocalDate date, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close) {}
    public record IndexSnapshot(String market, String code, String unit, LocalDate tradeDate, Instant collectedAt,
                                List<IndexBar> bars, List<BackdataRecord> source, List<String> warnings) {}
    public record MarketSnapshot(List<IndexSnapshot> indices, Instant collectedAt) {}
    public record Evidence(String code, String label, BigDecimal value, String unit, String comparison,
                           BigDecimal threshold, Boolean passed, String direction, String role, String quality,
                           String source, LocalDate tradeDate, Instant observedAt, String version) {}
    public record StockDecision(String code, String name, String market, String disposition, String strategy,
                                List<Evidence> evidence, List<String> cautions, TradeScenario scenario,
                                BigDecimal netRewardRisk, BigDecimal relativeReturn20, BigDecimal liquidity,
                                BigDecimal dailyReturn, boolean aboveEma20, Integer stage, boolean transition61,
                                com.example.stockanalysis.judgment.NewEntryAssessment newEntry) {
        public StockDecision(String code, String name, String market, String disposition, String strategy,
                             List<Evidence> evidence, List<String> cautions, TradeScenario scenario,
                             BigDecimal netRewardRisk, BigDecimal relativeReturn20, BigDecimal liquidity,
                             BigDecimal dailyReturn, boolean aboveEma20, Integer stage, boolean transition61) {
            this(code,name,market,disposition,strategy,evidence,cautions,scenario,netRewardRisk,relativeReturn20,
                    liquidity,dailyReturn,aboveEma20,stage,transition61,null);
        }
    }
    public record MarketView(String market, BigDecimal dayReturn, BigDecimal return20, BigDecimal aboveEma20Percent,
                             BigDecimal advancingPercent, long validStocks, long eligibleStocks,
                             Map<Integer,Long> stages, long transitions61, String bias,
                             String baseScenario, String upsideScenario, String downsideScenario,
                             BigDecimal referenceClose, BigDecimal support, BigDecimal resistance, List<String> cautions) {}
    public record DailyReport(String runId, String status, LocalDate tradeDate, LocalDate forecastDate,
                              Instant informationAsOf, Instant finalizedAt, String version, String validationStatus,
                              String scope, List<MarketView> markets, List<StockDecision> candidates,
                              long assessedStocks, long eligibleStocks, List<String> limitations, TierLists tierLists) {
        public DailyReport(String runId,String status,LocalDate tradeDate,LocalDate forecastDate,Instant informationAsOf,
                           Instant finalizedAt,String version,String validationStatus,String scope,List<MarketView> markets,
                           List<StockDecision> candidates,long assessedStocks,long eligibleStocks,List<String> limitations) {
            this(runId,status,tradeDate,forecastDate,informationAsOf,finalizedAt,version,validationStatus,scope,markets,candidates,assessedStocks,eligibleStocks,limitations,null);
        }
        public DailyReport withTierLists(TierLists tiers) {
            return new DailyReport(runId,status,tradeDate,forecastDate,informationAsOf,finalizedAt,version,validationStatus,scope,markets,candidates,assessedStocks,eligibleStocks,limitations,tiers);
        }
    }
    public record TierLists(String policyVersion,List<StockDecision> comparisonB,int comparisonBTotal,
                            List<StockDecision> signalWaiting,int signalWaitingTotal, List<StockDecision> activeSignals,int activeSignalsTotal) {
        public TierLists(String policyVersion,List<StockDecision> comparisonB,int comparisonBTotal,List<StockDecision> signalWaiting,int signalWaitingTotal) {
            this(policyVersion,comparisonB,comparisonBTotal,signalWaiting,signalWaitingTotal,null,0);
        }
        public static TierLists from(List<StockDecision> stocks,int maximum) {
            var order=Comparator.comparing((StockDecision s)->s.newEntry().suitability().score(),Comparator.reverseOrder())
                    .thenComparing(StockDecision::liquidity,Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(StockDecision::code);
            var b=stocks.stream().filter(s->"RESEARCH_B".equals(s.disposition())).sorted(order).toList();
            var waiting=stocks.stream().filter(s->"SIGNAL_WAIT".equals(s.disposition())).sorted(order).toList();
            var active=stocks.stream().filter(s->s.newEntry()!=null && s.newEntry().suitability()!=null
                    && Set.of("A","B").contains(s.newEntry().suitability().tier()) && s.newEntry().selectedSignal()!=null
                    && s.newEntry().signals().stream().anyMatch(c->c.signalId().equals(s.newEntry().selectedSignal().id())&&c.eligible())).sorted(order).toList();
            return new TierLists(com.example.stockanalysis.judgment.SuitabilityTier.VERSION,b.stream().limit(maximum).toList(),b.size(),waiting.stream().limit(maximum).toList(),waiting.size(),active.stream().limit(maximum).toList(),active.size());
        }
    }
    public record View(String state, String errorCode, DailyReport report, String notificationState) {}
    public record Summary(String runId, LocalDate tradeDate, String market, String scanState, String reportState) {}
    public record Library(boolean enabled, LocalDate expectedTradeDate, Summary latestReady, Summary newestRun,
                          List<Summary> reports, int page, boolean hasMore) {}
}
