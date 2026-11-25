package com.example.stockanalysis.unified.application;

import com.example.stockanalysis.engine.model.*;
import com.example.stockanalysis.unified.model.UnifiedAnalysisResult;
import com.example.stockanalysis.unified.model.UnifiedWarning;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Candidate eligibility is separate from market breadth and the underlying factor scores. */
public final class UnifiedTradeEligibility {
    public static final String VERSION = "open-price-v1";
    public static final BigDecimal MINIMUM_OPEN_EXCLUSIVE = new BigDecimal("2000");
    public static final String EXCLUSION_REASON = "분석 기준일 시가가 2,000원 이하이므로 매매 후보에서 제외했습니다. 시장 통계에는 포함합니다.";

    private UnifiedTradeEligibility() {}

    public static boolean eligible(BigDecimal openingPrice) {
        return openingPrice != null && openingPrice.compareTo(MINIMUM_OPEN_EXCLUSIVE) > 0;
    }

    static UnifiedAnalysisResult apply(UnifiedAnalysisResult result, List<DailyCandle> candles) {
        var openingPrice = candles.stream().filter(c -> c.businessDate().equals(result.priceDate()))
                .map(DailyCandle::open).findFirst().orElse(null);
        if (eligible(openingPrice)) return result;
        String reason = openingPrice == null ? "분석 기준일 시가를 확인할 수 없어 매매 후보에서 제외했습니다." : EXCLUSION_REASON;
        var original = result.analysis();
        var signalReport = original.signalReport();
        var signalWarnings = new ArrayList<>(signalReport.warnings());
        signalWarnings.add(reason);
        var signals = new SignalReport(signalReport.version(), signalReport.evaluatedAt(), signalReport.positionScope(),
                signalReport.completeness(), signalReport.signals().stream().filter(s -> s.side() != TradeSignal.Side.BUY).toList(), signalWarnings);
        var analysisWarnings = new ArrayList<>(original.warnings());
        analysisWarnings.add(reason);
        var analysis = new AnalysisResult(original.engineVersion(), original.mode(), original.totalScore(), original.factors(),
                new StrategyDecision("OPEN_PRICE_EXCLUDED", "시가 기준 매매 제외", StrategyAction.AVOID, List.of(reason)),
                null, analysisWarnings, signals);
        var warnings = new ArrayList<>(result.warnings());
        warnings.add(new UnifiedWarning(UnifiedWarning.Source.ANALYSIS, reason));
        return new UnifiedAnalysisResult(result.engineVersion(), result.stock(), result.asOf(), result.priceDate(),
                result.dailyBarCount(), result.weeklyBarCount(), result.monthlyBarCount(), analysis, result.screener(), warnings);
    }
}
