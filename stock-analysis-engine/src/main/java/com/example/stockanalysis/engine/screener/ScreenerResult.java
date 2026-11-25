package com.example.stockanalysis.engine.screener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static com.example.stockanalysis.engine.screener.ScreenerTypes.*;

public record ScreenerResult(String stockCode, ScreenerTimeframe timeframe, LocalDateTime asOf,
        LocalDateTime effectiveAt, String engineVersion, ScreenerConfig config, int availableBars, Quality quality,
        Integer stage, Integer previousBarStage, Integer previousDistinctStage, Integer barsInStage,
        boolean stageDurationComplete, LocalDateTime stageStartedAt, boolean stageChanged,
        Transition transition, Transition lastTransition, Integer barsSinceTransition,
        EmaValues ema, Slopes slopes, MovingAverageGapAnalyzer.Gap shortMiddleGap,
        MovingAverageGapAnalyzer.Gap middleLongGap, List<StagePoint> history,
        MarketFeatures features, BigDecimal trendScore, BigDecimal entryScore,
        EvaluationStatus evaluationStatus, String signal, List<String> reasons, List<String> warnings) {
    public ScreenerResult { history = List.copyOf(history); reasons = List.copyOf(reasons); warnings = List.copyOf(warnings); }
    public record EmaValues(BigDecimal shortEma, BigDecimal middleEma, BigDecimal longEma) {}
    public record Slopes(BigDecimal shortSlope, BigDecimal middleSlope, BigDecimal longSlope,
                         Direction shortDirection, Direction middleDirection, Direction longDirection) {}
    public record StagePoint(LocalDateTime at, Integer stage, Quality quality) {}
    public record MarketFeatures(BigDecimal close, BigDecimal tradingAmountMillionWon,
            BigDecimal volumeRatio20, BigDecimal previous20BarHigh, Boolean breakout20,
            BigDecimal priceToMiddleEmaPercent, High52Week high52Week) {}
    public record High52Week(LocalDate referenceDate, LocalDate inclusiveWindowStart, BigDecimal high,
            BigDecimal previousHigh, Boolean breakout, BigDecimal distancePercent, Coverage coverage) {}
}
