package com.example.stockanalysis.unified.application;

import com.example.stockanalysis.analysis.application.port.out.AnalysisDataProvider;
import com.example.stockanalysis.analysis.domain.DataQualityStatus;
import com.example.stockanalysis.engine.model.AnalysisMode;
import com.example.stockanalysis.engine.screener.ScreenerBar;
import com.example.stockanalysis.screener.infrastructure.ScreenerStockTypeCatalog;
import com.example.stockanalysis.shared.exception.BusinessException;
import com.example.stockanalysis.shared.exception.ErrorCode;
import com.example.stockanalysis.unified.api.UnifiedStockAnalysisEngine;
import com.example.stockanalysis.unified.model.UnifiedAnalysisInput;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UnifiedAnalysisService {
    private static final Logger log = LoggerFactory.getLogger(UnifiedAnalysisService.class);
    private final AnalysisDataProvider provider;
    private final UnifiedStockAnalysisEngine engine;
    private final ScreenerStockTypeCatalog stockTypes;
    private final Clock clock;

    public UnifiedAnalysisService(AnalysisDataProvider provider, UnifiedStockAnalysisEngine engine,
                                  ScreenerStockTypeCatalog stockTypes, Clock clock) {
        this.provider = provider;
        this.engine = engine;
        this.stockTypes = stockTypes;
        this.clock = clock;
    }

    private com.example.stockanalysis.holdings.HoldingService holdings;
    @org.springframework.beans.factory.annotation.Autowired
    public void setHoldings(com.example.stockanalysis.holdings.HoldingService holdings) {this.holdings=holdings;}
    public com.example.stockanalysis.holdings.HoldingModels.Snapshot holdingSnapshot(String code) {return holdings==null?null:holdings.single(code);}
    public com.example.stockanalysis.holdings.HoldingModels.Snapshot holdingBatch(String id) {return holdings==null?null:holdings.batch("UNIFIED:"+id);}
    public UnifiedAnalysisResponse personalize(UnifiedAnalysisResponse data,com.example.stockanalysis.holdings.HoldingModels.Snapshot snapshot) {
        var r=data.result();var scenario=r.analysis().scenario();
        return data.withHolding(com.example.stockanalysis.holdings.HoldingModels.evaluate(snapshot,r.stock().stockCode(),r.stock().currentPrice(),r.priceDate(),
            scenario==null?null:scenario.stopLoss(),scenario==null?java.util.List.of():scenario.targets(),data.tradeJudgment()));
    }
    public UnifiedAnalysisResponse execute(String stockCode) {
        var holding=holdingSnapshot(stockCode);
        return personalize(executeAt(stockCode, LocalDateTime.ofInstant(clock.instant(), ZoneId.of("Asia/Seoul"))),holding);
    }

    public UnifiedAnalysisResponse executeAt(String stockCode, LocalDateTime asOf) {
        return captureAt(stockCode, asOf).analysis();
    }

    public record CapturedAnalysis(UnifiedAnalysisResponse analysis,
                                   com.example.stockanalysis.analysis.application.model.CollectedAnalysisData collected) {}

    public CapturedAnalysis captureAt(String stockCode, LocalDateTime asOf) {
        if (stockCode == null || !stockCode.matches("[0-9]{6}") || !"ST".equals(stockTypes.typeOf(stockCode))) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "종합분석은 국내 일반주식 종목만 지원합니다.");
        }
        // Freeze before collection, including when collection crosses Korean midnight.
        var baseDate = asOf.toLocalDate().minusDays(1);
        var collected = provider.collect(stockCode, AnalysisMode.SWING, baseDate);
        return analyzeCollected(stockCode, baseDate, asOf, collected, null);
    }

    /** The EOD coordinator has already frozen the ST universe and verifies readiness separately. */
    public com.example.stockanalysis.analysis.application.model.CollectedAnalysisData collectEod(String code, java.time.LocalDate date) {
        if (code == null || !code.matches("[0-9]{6}")) throw new IllegalArgumentException("Invalid stock code");
        return provider.collectDaily(code, date);
    }

    public com.example.stockanalysis.analysis.application.model.CollectedAnalysisData completeEod(
            com.example.stockanalysis.analysis.application.model.CollectedAnalysisData daily) {
        return provider.completeDaily(daily);
    }

    public CapturedAnalysis analyzeEod(String code, java.time.LocalDate date, LocalDateTime collectedAt,
            com.example.stockanalysis.analysis.application.model.CollectedAnalysisData collected, LocalDateTime priceCutoff) {
        if (priceCutoff.toLocalDate().equals(date) && !collectedAt.isBefore(priceCutoff))
            return analyzeCollected(code, date, collectedAt, collected, priceCutoff);
        throw new IllegalArgumentException("EOD cutoff has not passed");
    }

    private CapturedAnalysis analyzeCollected(String stockCode, java.time.LocalDate baseDate, LocalDateTime asOf,
            com.example.stockanalysis.analysis.application.model.CollectedAnalysisData collected, LocalDateTime priceCutoff) {
        var source = collected.input();
        if (!source.stock().stockCode().equals(stockCode) || source.mode() != AnalysisMode.SWING
                || !source.baseDate().equals(baseDate)) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "요청한 종목과 기준일에 맞는 분석 데이터를 받지 못했습니다.");
        }
        var warnings = new ArrayList<>(collected.warnings());
        warnings.add("기본정보·수급 등 부가 자료는 수집 시점 기준입니다. 과거 시점 재현용 데이터가 아닙니다.");
        boolean truncated = collected.sources().stream().anyMatch(s -> "ka10081".equals(s.apiId())
                && s.continuation() != null && s.continuation().hasNext());
        if (truncated) warnings.add("일봉은 기존 수집기의 조회 범위만 사용했습니다. 추가 이력이 있을 수 있으므로 52주 범위와 표본 수를 확인하세요.");
        var bars = source.dailyCandles().stream().map(c -> new ScreenerBar(c.businessDate().atStartOfDay(),
                priceCutoff != null && c.businessDate().equals(baseDate) ? priceCutoff : c.businessDate().plusDays(1).atStartOfDay(),
                c.open(), c.high(), c.low(), c.close(), c.volume(), null)).toList();
        try {
            var engineInput = new UnifiedAnalysisInput(stockCode, source.stock().stockName(),
                    source.stock().marketCode(), asOf, bars, source.observations(), null, priceCutoff == null ? null : baseDate);
            long engineStarted = System.nanoTime();
            var calculated = engine.analyze(engineInput);
            long engineNanos = System.nanoTime() - engineStarted;
            var result = UnifiedTradeEligibility.apply(calculated, source.dailyCandles());
            Long volume = source.dailyCandles().stream().filter(c -> c.businessDate().equals(result.priceDate()))
                    .map(c -> c.volume()).findFirst().orElse(null);
            // Use the normalized ka10081 amount (million KRW), never close * volume or a live quote.
            var amounts = collected.sources().stream().filter(s -> "ka10081".equals(s.apiId()))
                    .flatMap(s -> s.records().stream())
                    .filter(r -> stockCode.equals(r.stockCode()) && result.priceDate().equals(r.businessDate()))
                    .map(r -> nonNegativeAmount(r.value("tradingAmount"))).filter(java.util.Objects::nonNull)
                    .map(BigDecimal::stripTrailingZeros).distinct().toList();
            BigDecimal amount = amounts.size() == 1 ? amounts.get(0) : null;
            if (amount == null) warnings.add("분석 가격일의 거래대금을 확인할 수 없습니다. 최소 거래대금 검색에서는 제외됩니다.");
            var completed = source.dailyCandles().stream().filter(c -> !c.businessDate().isAfter(result.priceDate()))
                    .sorted(java.util.Comparator.comparing(com.example.stockanalysis.engine.model.DailyCandle::businessDate)).toList();
            var history = completed.subList(Math.max(0, completed.size() - 300), completed.size());
            // More available history is a coverage warning, not missing required input.
            // The collector and signal engine separately validate required data and sample size.
            var quality = collected.dataQualityStatus();
            var missingFactors = result.analysis().factors().stream()
                    .filter(f -> f.score() == null && !java.util.Set.of("VALUE_QUALITY", "MONTHLY_TECHNICAL").contains(f.factorCode()))
                    .map(f -> f.factorCode()).toList();
            if (quality != DataQualityStatus.COMPLETE || result.analysis().totalScore() == null
                    || !result.screener().quality().name().equals("VALID") || !missingFactors.isEmpty()) {
                log.warn("Unified analysis limited: stockCode={}, baseDate={}, priceDate={}, collectionQuality={}, stage={}, screenerQuality={}, scoreAvailable={}, missingFactors={}, dailyBars={}, weeklyBars={}, screenerReasons={}",
                        stockCode, baseDate, result.priceDate(), quality, result.screener().stage(), result.screener().quality(),
                        result.analysis().totalScore() != null, missingFactors, result.dailyBarCount(), result.weeklyBarCount(), result.screener().warnings());
            }
            var judgmentEngine = new com.example.stockanalysis.judgment.TradeJudgmentEngine();
            var judgmentInput = new com.example.stockanalysis.judgment.TradeJudgmentEngine.Input(result.analysis(), result.stock().currentPrice(),
                    result.priceDate(), result.asOf().atZone(ZoneId.of("Asia/Seoul")).toInstant(), quality.name());
            long judgmentStarted = System.nanoTime();
            var judgment = judgmentEngine.evaluate(judgmentInput);
            long judgmentNanos = System.nanoTime() - judgmentStarted;
            // One summary per successful analysis; exclude collection, input preparation and persistence.
            log.info("Unified engine timing: stockCode={}, baseDate={}, mode={}, result=SUCCESS, engineVersion={}, dailyBars={}, engineMs={}, judgmentMs={}",
                    stockCode, baseDate, priceCutoff == null ? "MANUAL" : "EOD", calculated.engineVersion(), bars.size(),
                    TimeUnit.NANOSECONDS.toMicros(engineNanos) / 1000.0,
                    TimeUnit.NANOSECONDS.toMicros(judgmentNanos) / 1000.0);
            return new CapturedAnalysis(new UnifiedAnalysisResponse(result, quality, warnings, volume, amount, history, null, judgment), collected);
        } catch (IllegalArgumentException | ArithmeticException exception) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "수집된 일봉의 날짜·가격·거래량을 확인할 수 없어 종합분석을 완료하지 못했습니다.", exception);
        }
    }

    private static BigDecimal nonNegativeAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            var value = new BigDecimal(raw.trim().replace(",", ""));
            return value.signum() < 0 || value.stripTrailingZeros().scale() > 4
                    || value.precision() - value.scale() > 24 ? null : value;
        } catch (NumberFormatException e) { return null; }
    }
}
