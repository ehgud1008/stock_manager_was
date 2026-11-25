package com.example.stockanalysis.unified.scan;

import static com.example.stockanalysis.unified.scan.UnifiedScanModels.*;

import com.example.stockanalysis.analysis.api.SuitabilityScoresResponse;
import com.example.stockanalysis.analysis.api.SwingAssessmentResponse;
import com.example.stockanalysis.engine.factor.internal.SwingSuitabilityEngine;
import com.example.stockanalysis.engine.model.TradeSignal;
import com.example.stockanalysis.engine.screener.DefaultScreenerEngine;
import com.example.stockanalysis.engine.screener.ScreenerConfig;
import com.example.stockanalysis.engine.signal.SwingSignalEngine;
import com.example.stockanalysis.shared.exception.BusinessException;
import com.example.stockanalysis.shared.exception.ErrorCode;
import com.example.stockanalysis.unified.application.UnifiedAnalysisResponse;
import com.example.stockanalysis.unified.eod.EodCoordinator;
import com.example.stockanalysis.unified.internal.DefaultUnifiedStockAnalysisEngine;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class UnifiedScanService {
    public static final String VERSION = String.join("/", DefaultUnifiedStockAnalysisEngine.VERSION,
            SwingSuitabilityEngine.VERSION, SwingSignalEngine.VERSION, DefaultScreenerEngine.VERSION, ScreenerConfig.defaults().version(),
            com.example.stockanalysis.unified.application.UnifiedTradeEligibility.VERSION,
            com.example.stockanalysis.judgment.TradeJudgmentEngine.VERSION);
    public static final String PREVIOUS_CLOSE_PREFIX = "previous-close/";
    public static final String PREVIOUS_CLOSE_VERSION = PREVIOUS_CLOSE_PREFIX + VERSION;
    private final UnifiedScanStore store;
    private final EodCoordinator coordinator;

    public UnifiedScanService(UnifiedScanStore store, EodCoordinator coordinator) {
        this.store = store;
        this.coordinator = coordinator;
    }

    public Run submit(Request request, String key) { return coordinator.submit(request, key); }
    public Run resume(String id) { return coordinator.resume(id); }
    public Run recover(String id) { return coordinator.recover(id); }
    public Run get(String id) {
        return store.find(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "종합분석 실행을 찾을 수 없습니다."));
    }
    public List<Run> recent() { return store.recent(); }
    public List<Run> recentArchived() { return store.recentArchived(); }
    public Page query(String id, Query query) { return store.query(get(id), query); }
    public Page queryArchived(String id, Query query) {
        var run = store.findArchived(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "이전 분석 실행을 찾을 수 없습니다."));
        return store.query(run, query, true);
    }
    public long archivedAnalysisId(String id, String code) {
        return store.archivedAnalysisId(id, code).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "저장된 이전 종목분석 결과가 없습니다."));
    }
    public UnifiedAnalysisResponse detail(String id, String code) {
        get(id);
        return store.detail(id, code).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "완료된 종합분석 결과가 없습니다."));
    }
    public List<UnifiedScanStore.ScoreHistory> scoreHistory(String id, String code) {
        detail(id, code);
        return store.scoreHistory(id, code);
    }

    public static Item summarize(Item item, UnifiedAnalysisResponse detail) {
        var r = detail.result(); var a = r.analysis(); var s = r.screener(); var scenario = a.scenario();
        if (!item.stockCode().equals(r.stock().stockCode()) || !item.stockCode().equals(s.stockCode())) throw new IllegalArgumentException("IDENTITY_MISMATCH");
        var signals = a.signalReport().signals().stream()
                .filter(signal -> Set.of(TradeSignal.State.CONFIRMED, TradeSignal.State.ACTIVE, TradeSignal.State.PRELIMINARY).contains(signal.state()))
                .sorted(Comparator.comparing((TradeSignal signal) -> signal.state() == TradeSignal.State.PRELIMINARY)
                        .thenComparing(TradeSignal::occurredOn, Comparator.reverseOrder()).thenComparing(TradeSignal::type)).toList();
        var buy = signals.stream().filter(signal -> signal.side() == TradeSignal.Side.BUY).findFirst().orElse(null);
        var sell = signals.stream().filter(signal -> signal.side() == TradeSignal.Side.SELL).findFirst().orElse(null);
        var activeSignals = signals.stream().filter(signal -> signal.state() != TradeSignal.State.PRELIMINARY)
                .map(TradeSignal::type).distinct().sorted().collect(java.util.stream.Collectors.joining("|", "|", "|"));
        var scores = SuitabilityScoresResponse.from(a);
        var assessment = SwingAssessmentResponse.fromStrategy(a.strategy().strategyCode(), scenario != null);
        BigDecimal rr = scenario == null || scenario.targets().isEmpty() ? null : scenario.targets().get(0).subtract(scenario.entryTo())
                .divide(scenario.entryTo().subtract(scenario.stopLoss()), 6, RoundingMode.HALF_UP);
        var slopes = s.slopes();
        Boolean up = slopes.shortSlope() == null || slopes.middleSlope() == null || slopes.longSlope() == null ? null
                : slopes.shortSlope().signum() > 0 && slopes.middleSlope().signum() > 0 && slopes.longSlope().signum() > 0;
        String quality = detail.collectionQuality().name();
        if (quality.equals("COMPLETE") && (a.totalScore() == null || !s.quality().name().equals("VALID")
                || a.factors().stream().anyMatch(f -> f.score() == null && !Set.of("VALUE_QUALITY", "MONTHLY_TECHNICAL").contains(f.factorCode())))) quality = "PARTIAL";
        return new Item(item.stockCode(), item.stockName(), item.market(), "COMPLETED", r.priceDate(), s.stage(), s.previousDistinctStage(),
                s.lastTransition().name(), s.barsSinceTransition(), s.barsInStage(), up, a.totalScore(), buy == null ? null : buy.type(),
                sell == null ? null : sell.type(), activeSignals,
                buy == null ? "NONE" : buy.entryStatus(), quality, s.quality().name(), r.stock().currentPrice(), scenario == null ? null : scenario.entryFrom(),
                scenario == null ? null : scenario.entryTo(), scenario == null || scenario.targets().isEmpty() ? null : scenario.targets().get(0),
                scenario == null ? null : scenario.stopLoss(), rr, null, detail.volume(), detail.tradingAmountMillionWon(),
                scores == null ? null : scores.rise(), scores == null ? null : scores.entry(), scores == null ? null : scores.risk(),
                buy == null ? null : buy.state().name(), sell == null ? null : sell.state().name(),
                assessment == null ? null : assessment.setupType(),
                scenario == null || scenario.targets().size() < 2 ? null : scenario.targets().get(1));
    }
}
