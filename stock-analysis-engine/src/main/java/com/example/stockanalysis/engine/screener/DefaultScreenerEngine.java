package com.example.stockanalysis.engine.screener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import static com.example.stockanalysis.engine.screener.EmaCalculator.PRECISION;
import static com.example.stockanalysis.engine.screener.ScreenerResult.*;
import static com.example.stockanalysis.engine.screener.ScreenerTypes.*;

/** Deterministic, framework-free analysis. It never fetches data or decides a trade. */
public final class DefaultScreenerEngine {
    public static final String VERSION = "screener-ema-v1";
    private final ScreenerConfig config;
    private final MovingAverageSlopeCalculator slopeCalculator;
    private final StageAnalyzer stages = new StageAnalyzer();
    private final MovingAverageGapAnalyzer gaps = new MovingAverageGapAnalyzer();

    public DefaultScreenerEngine(ScreenerConfig config) { this(config, MovingAverageSlopeCalculator.percentChange()); }
    public DefaultScreenerEngine(ScreenerConfig config, MovingAverageSlopeCalculator slopeCalculator) {
        this.config = java.util.Objects.requireNonNull(config);
        this.slopeCalculator = java.util.Objects.requireNonNull(slopeCalculator);
    }
    public ScreenerConfig config() { return config; }

    public ScreenerResult analyze(ScreenerInput input) {
        List<ScreenerBar> bars = normalize(input.bars(), input.asOf());
        validateTimeframe(bars, input.timeframe());
        List<ScreenerBar> daily = input.timeframe().mode() == Mode.SWING ? bars : normalize(input.dailyBars(), input.asOf());
        validateTimeframe(daily, new ScreenerTimeframe(Mode.SWING, 0));
        EmaCalculator calculator = new EmaCalculator();
        List<BigDecimal> closes = bars.stream().map(ScreenerBar::close).toList();
        List<BigDecimal> shortValues = calculator.calculate(closes, config.shortPeriod());
        List<BigDecimal> middleValues = calculator.calculate(closes, config.middlePeriod());
        List<BigDecimal> longValues = calculator.calculate(closes, config.longPeriod());
        List<StagePoint> points = new ArrayList<>();
        for (int i = 0; i < bars.size(); i++) {
            StageAnalyzer.Stage stage = i + 1 < config.warmupBars()
                    ? new StageAnalyzer.Stage(null, Quality.INSUFFICIENT_DATA)
                    : stages.analyze(shortValues.get(i), middleValues.get(i), longValues.get(i), config.boundaryPercent());
            points.add(new StagePoint(bars.get(i).openTime(), stage.stage(), stage.quality()));
        }
        int last = bars.size() - 1;
        StagePoint current = last < 0 ? new StagePoint(null, null, Quality.INSUFFICIENT_DATA) : points.get(last);
        StageHistoryAnalyzer.History history = new StageHistoryAnalyzer().analyze(points);
        EmaValues ema = new EmaValues(value(shortValues, last), value(middleValues, last), value(longValues, last));
        boolean warmedUp = bars.size() >= config.warmupBars();
        BigDecimal shortSlope = warmedUp ? slope(shortValues, last) : null;
        BigDecimal middleSlope = warmedUp ? slope(middleValues, last) : null;
        BigDecimal longSlope = warmedUp ? slope(longValues, last) : null;
        Slopes slopes = new Slopes(shortSlope, middleSlope, longSlope,
                MovingAverageSlopeCalculator.direction(shortSlope, config), MovingAverageSlopeCalculator.direction(middleSlope, config),
                MovingAverageSlopeCalculator.direction(longSlope, config));
        List<String> warnings = new ArrayList<>();
        if (!warmedUp) warnings.add("INSUFFICIENT_EMA_WARMUP");
        if (current.quality() == Quality.BOUNDARY) warnings.add("EMA_BOUNDARY");
        if (current.stage() != null && !history.durationComplete()) warnings.add("STAGE_DURATION_LOWER_BOUND");
        if (input.timeframe().mode() == Mode.SHORT_TERM) warnings.add("INTRADAY_EMA_CONTINUES_ACROSS_SESSIONS");
        MarketFeatures features = features(bars, daily, ema.middleEma(), input.asOf().toLocalDate());
        if (features.high52Week().coverage() != Coverage.FULL_WINDOW) warnings.add("INCOMPLETE_52_WEEK_HISTORY");
        List<String> reasons = new ArrayList<>();
        if (current.stage() != null) reasons.add("EMA_STAGE_" + current.stage());
        if (history.lastTransition() != Transition.UNKNOWN) reasons.add("LAST_TRANSITION_" + history.previousDistinctStage() + "_" + current.stage() + "_" + history.lastTransition());
        if (shortSlope != null && shortSlope.signum() > 0 && middleSlope != null && middleSlope.signum() > 0 && longSlope != null && longSlope.signum() > 0) reasons.add("ALL_EMA_SLOPES_POSITIVE");
        if (Boolean.TRUE.equals(features.breakout20())) reasons.add("CLOSE_ABOVE_PREVIOUS_20_BAR_HIGH");
        return new ScreenerResult(input.stockCode(), input.timeframe(), input.asOf(), last < 0 ? null : bars.get(last).closedAt(),
                VERSION, config, bars.size(), current.quality(), current.stage(), history.previousBarStage(), history.previousDistinctStage(),
                history.barsInStage(), history.durationComplete(), history.startedAt(), history.changed(), history.transition(), history.lastTransition(),
                history.barsSinceTransition(), ema, slopes,
                gap(shortValues, middleValues, last, warmedUp), gap(middleValues, longValues, last, warmedUp),
                points.subList(Math.max(0, points.size() - config.historyBars()), points.size()), features,
                null, null, EvaluationStatus.NOT_EVALUATED, null, reasons, warnings);
    }

    private BigDecimal slope(List<BigDecimal> values, int last) {
        return slopeCalculator.calculate(value(values, last - config.slopeLookback()), value(values, last));
    }
    private MovingAverageGapAnalyzer.Gap gap(List<BigDecimal> a, List<BigDecimal> b, int last, boolean warmedUp) {
        if (!warmedUp) return gaps.analyze(null, null, config.gapFlatPercent(), false);
        int start = last - config.gapLookback();
        BigDecimal current = gaps.percent(value(a, last), value(b, last));
        BigDecimal previous = gaps.percent(value(a, start), value(b, start));
        boolean crossing = false;
        for (int i = Math.max(1, start + 1); i <= last; i++) {
            BigDecimal before = gaps.percent(value(a, i - 1), value(b, i - 1));
            BigDecimal after = gaps.percent(value(a, i), value(b, i));
            if (before != null && after != null && before.signum() != after.signum()) crossing = true;
        }
        return gaps.analyze(current, previous, config.gapFlatPercent(), crossing);
    }
    private BigDecimal value(List<BigDecimal> values, int index) { return index < 0 || index >= values.size() ? null : values.get(index); }

    private MarketFeatures features(List<ScreenerBar> bars, List<ScreenerBar> daily, BigDecimal middleEma, LocalDate referenceDate) {
        if (bars.isEmpty()) return new MarketFeatures(null, null, null, null, null, null, high52(daily, referenceDate, null));
        int last = bars.size() - 1;
        ScreenerBar latest = bars.get(last);
        BigDecimal high20 = null, volumeRatio = null;
        if (last >= 20) {
            List<ScreenerBar> previous = bars.subList(last - 20, last);
            high20 = previous.stream().map(ScreenerBar::high).max(BigDecimal::compareTo).orElseThrow();
            BigDecimal total = previous.stream().map(bar -> BigDecimal.valueOf(bar.volume())).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (total.signum() > 0) volumeRatio = BigDecimal.valueOf(latest.volume()).multiply(BigDecimal.valueOf(20)).divide(total, PRECISION);
        }
        LocalDate date = latest.openTime().toLocalDate();
        return new MarketFeatures(latest.close(), latest.tradingAmountMillionWon(), volumeRatio, high20,
                high20 == null ? null : latest.close().compareTo(high20) > 0, gaps.percent(latest.close(), middleEma), high52(daily, date, latest.close()));
    }

    private High52Week high52(List<ScreenerBar> daily, LocalDate date, BigDecimal currentClose) {
        LocalDate boundary = date.minusWeeks(52);
        // Display window: (D-52w, D]. Breakout reference window: [D-52w, D).
        List<ScreenerBar> usable = daily.stream().filter(bar -> !bar.openTime().toLocalDate().isAfter(date)).toList();
        BigDecimal high = usable.stream().filter(bar -> bar.openTime().toLocalDate().isAfter(boundary)).map(ScreenerBar::high).max(BigDecimal::compareTo).orElse(null);
        BigDecimal previous = usable.stream().filter(bar -> !bar.openTime().toLocalDate().isBefore(boundary) && bar.openTime().toLocalDate().isBefore(date)).map(ScreenerBar::high).max(BigDecimal::compareTo).orElse(null);
        Coverage coverage = high == null ? Coverage.NO_DATA : usable.get(0).openTime().toLocalDate().isAfter(boundary) ? Coverage.HISTORY_SHORT : Coverage.FULL_WINDOW;
        // Intraday daily history may not include today's bar. Never label that incomplete window full.
        if (coverage == Coverage.FULL_WINDOW && !usable.get(usable.size() - 1).openTime().toLocalDate().equals(date)) coverage = Coverage.HISTORY_SHORT;
        return new High52Week(date, boundary.plusDays(1), high, previous,
                coverage == Coverage.FULL_WINDOW && previous != null && currentClose != null ? currentClose.compareTo(previous) > 0 : null,
                high == null || currentClose == null ? null : high.subtract(currentClose).multiply(BigDecimal.valueOf(100)).divide(high, PRECISION), coverage);
    }

    public static List<ScreenerBar> normalize(List<ScreenerBar> source, LocalDateTime asOf) {
        TreeMap<LocalDateTime, ScreenerBar> result = new TreeMap<>();
        for (ScreenerBar bar : source) {
            if (bar.closedAt().isAfter(asOf)) continue;
            ScreenerBar previous = result.putIfAbsent(bar.openTime(), bar);
            if (previous != null && !same(previous, bar)) throw new IllegalArgumentException("conflicting duplicate bar at " + bar.openTime());
        }
        return List.copyOf(result.values());
    }
    private static boolean same(ScreenerBar a, ScreenerBar b) {
        return a.closedAt().equals(b.closedAt()) && a.open().compareTo(b.open()) == 0 && a.high().compareTo(b.high()) == 0
                && a.low().compareTo(b.low()) == 0 && a.close().compareTo(b.close()) == 0 && a.volume() == b.volume()
                && (a.tradingAmountMillionWon() == null ? b.tradingAmountMillionWon() == null
                : b.tradingAmountMillionWon() != null && a.tradingAmountMillionWon().compareTo(b.tradingAmountMillionWon()) == 0);
    }
    private void validateTimeframe(List<ScreenerBar> bars, ScreenerTimeframe timeframe) {
        for (int i = 0; i < bars.size(); i++) {
            ScreenerBar bar = bars.get(i);
            if (timeframe.mode() == Mode.SHORT_TERM && bar.closedAt().isBefore(bar.openTime().plusMinutes(timeframe.interval()))) {
                throw new IllegalArgumentException("minute bar closes before its interval ends");
            }
            if (i > 0 && timeframe.mode() == Mode.SWING && bars.get(i - 1).openTime().toLocalDate().equals(bar.openTime().toLocalDate())) {
                throw new IllegalArgumentException("multiple daily bars for one date");
            }
        }
    }
}
