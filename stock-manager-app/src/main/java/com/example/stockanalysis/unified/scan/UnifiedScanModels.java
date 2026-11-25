package com.example.stockanalysis.unified.scan;

import com.example.stockanalysis.screener.domain.ScreenerRunRequest.Market;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

public final class UnifiedScanModels {
    private UnifiedScanModels() {}
    public enum PriceMode { EOD, PREVIOUS_CLOSE }
    public record Request(@Schema(description = "분석 시장. 생략 시 ALL", example = "ALL") Market market,
                          @Schema(description = "마감 확인 또는 전일까지 완료 일봉. 생략 시 EOD") PriceMode priceMode) {
        public Request(Market market) { this(market, PriceMode.EOD); }
        public Request { if (priceMode == null) priceMode = PriceMode.EOD; }
    }
    public record Run(String runId, String requestKey, String market, LocalDate baseDate, String engineVersion,
                      String status, boolean initialized, long createdEpoch, Long completedEpoch, String errorCode,
                      long total, long completed, long failed, long skipped, EodRun eod) {
        public Run(String runId, String requestKey, String market, LocalDate baseDate, String engineVersion,
                   String status, boolean initialized, long createdEpoch, Long completedEpoch, String errorCode,
                   long total, long completed, long failed, long skipped) {
            this(runId, requestKey, market, baseDate, engineVersion, status, initialized, createdEpoch, completedEpoch,
                    errorCode, total, completed, failed, skipped, null);
        }
    }
    public record EodRun(String priceBasis, long priceCutoffEpoch, long eligibleEpoch, long deadlineEpoch,
                         Long startedEpoch, long updatedEpoch, int recoveryCount, long pending,
                         long retrying, long dataUnavailable, String readinessPolicy, String calendarVersion) {}
    public record Item(String stockCode, String stockName, String market, String status, LocalDate priceDate,
                       Integer stage, Integer previousStage, String transition, Integer barsSinceTransition,
                       Integer barsInStage, Boolean allSlopesUp, BigDecimal totalScore, String buySignal,
                       String sellSignal, String activeSignals, String entryStatus, String quality, String screenerQuality,
                       BigDecimal currentPrice, BigDecimal entryFrom, BigDecimal entryTo, BigDecimal targetOne,
                       BigDecimal stopPrice, BigDecimal rewardRisk, String errorCode, Long volume, BigDecimal tradingAmountMillionWon,
                       BigDecimal riseScore, BigDecimal entryScore, BigDecimal riskScore, String buyState,
                       String sellState, String setupType, BigDecimal targetTwo) {}
    public record Page(Run run, long totalElements, int page, int size, List<Item> content) {}
    public record Query(String search, String status, Integer stage, String transition, Boolean allSlopesUp,
                        Integer maxBarsSinceTransition, String signal, String quality, BigDecimal minScore,
                        BigDecimal maxScore, BigDecimal minRewardRisk, String sort, boolean ascending, int page, int size,
                        String entryStatus, String market, Long minVolume, BigDecimal minTradingAmountEok, String setup) {
        public Query(String search, String status, Integer stage, String transition, Boolean allSlopesUp,
                     Integer maxBarsSinceTransition, String signal, String quality, BigDecimal minScore,
                     BigDecimal maxScore, BigDecimal minRewardRisk, String sort, boolean ascending, int page, int size,
                     String entryStatus, String market, Long minVolume, BigDecimal minTradingAmountEok) {
            this(search, status, stage, transition, allSlopesUp, maxBarsSinceTransition, signal, quality, minScore,
                    maxScore, minRewardRisk, sort, ascending, page, size, entryStatus, market, minVolume, minTradingAmountEok, null);
        }
        public Query(String search, String status, Integer stage, String transition, Boolean allSlopesUp,
                     Integer maxBarsSinceTransition, String signal, String quality, BigDecimal minScore,
                     BigDecimal maxScore, BigDecimal minRewardRisk, String sort, boolean ascending, int page, int size, String entryStatus) {
            this(search, status, stage, transition, allSlopesUp, maxBarsSinceTransition, signal, quality, minScore,
                    maxScore, minRewardRisk, sort, ascending, page, size, entryStatus, null, null, null);
        }
        public Query {
            search = search == null ? "" : search.trim();
            sort = sort == null ? "totalScore" : sort;
            if ("ALL".equals(market)) market = null;
            allowed(market, List.of("KOSPI", "KOSDAQ"));
            if (minVolume != null && minVolume < 0 || minTradingAmountEok != null && (minTradingAmountEok.signum() < 0
                    || minTradingAmountEok.stripTrailingZeros().scale() > 6 || minTradingAmountEok.precision() - minTradingAmountEok.scale() > 22))
                throw new IllegalArgumentException("최소 거래량·거래대금은 0 이상이어야 하며 거래대금은 소수 6자리까지 지원합니다.");
            if (search.length() > 100 || page < 0 || page > 100000 || size < 1 || size > 100
                    || stage != null && (stage < 1 || stage > 6)
                    || maxBarsSinceTransition != null && maxBarsSinceTransition < 0)
                throw new IllegalArgumentException("검색 조건 또는 페이지 범위가 올바르지 않습니다.");
            if (minScore != null && (minScore.signum() < 0 || minScore.compareTo(BigDecimal.valueOf(100)) > 0)
                    || maxScore != null && (maxScore.signum() < 0 || maxScore.compareTo(BigDecimal.valueOf(100)) > 0)
                    || minScore != null && maxScore != null && minScore.compareTo(maxScore) > 0
                    || minRewardRisk != null && minRewardRisk.signum() < 0)
                throw new IllegalArgumentException("점수 또는 손익비 범위가 올바르지 않습니다.");
            allowed(status, List.of("PENDING", "RETRY_WAIT", "DATA_UNAVAILABLE", "COMPLETED", "FAILED", "SKIPPED"));
            allowed(transition, List.of("FORWARD", "REVERSE", "SKIPPED", "UNKNOWN"));
            allowed(signal, List.of("BUY", "SELL", "BUY_PRELIMINARY", "SELL_PRELIMINARY", "BREAKOUT", "PULLBACK_RECOVERY", "BREAKOUT_RETEST", "TREND_BREAKDOWN", "BREAKOUT_FAILURE"));
            allowed(quality, List.of("COMPLETE", "PARTIAL", "INVALID"));
            allowed(entryStatus, List.of("IN_RANGE", "CHASE_BLOCKED", "OUTSIDE_ENTRY_ZONE", "SCENARIO_UNAVAILABLE", "SELL_SIGNAL_CONFLICT", "NONE"));
            allowed(setup, List.of("PULLBACK", "BREAKOUT", "CONTINUATION", "RANGE", "UNCLASSIFIED"));
            if (!List.of("totalScore", "entryScore", "stage", "rewardRisk", "stockCode", "barsSinceTransition").contains(sort))
                throw new IllegalArgumentException("지원하지 않는 정렬 조건입니다.");
        }
        private static void allowed(String value, List<String> options) {
            if (value != null && !value.isBlank() && !options.contains(value)) throw new IllegalArgumentException("지원하지 않는 검색 조건입니다.");
        }
    }
}
