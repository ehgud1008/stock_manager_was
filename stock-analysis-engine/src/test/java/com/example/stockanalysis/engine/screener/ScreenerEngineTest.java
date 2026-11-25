package com.example.stockanalysis.engine.screener;

import static org.assertj.core.api.Assertions.*;
import static com.example.stockanalysis.engine.screener.ScreenerTypes.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ScreenerEngineTest {
    private final DefaultScreenerEngine engine = new DefaultScreenerEngine(ScreenerConfig.defaults());
    private static BigDecimal n(String value) { return new BigDecimal(value); }
    private static ScreenerBar bar(LocalDate date, int close) {
        BigDecimal price = BigDecimal.valueOf(close);
        return new ScreenerBar(date.atStartOfDay(), date.plusDays(1).atStartOfDay(), price, price.add(BigDecimal.ONE), price.subtract(BigDecimal.ONE), price, 100, n("1000"));
    }
    private static List<ScreenerBar> rising(int count) {
        return IntStream.range(0, count).mapToObj(i -> bar(LocalDate.of(2024, 1, 1).plusDays(i), 1000 + i)).toList();
    }
    private ScreenerResult analyze(List<ScreenerBar> bars, LocalDateTime cutoff) {
        return engine.analyze(new ScreenerInput("005930", new ScreenerTimeframe(Mode.SWING, 0), cutoff, bars, List.of()));
    }
    @Test void allSixPermutationsAndDirectionAreExplicit() {
        int[][] orders = {{3,2,1},{2,3,1},{1,3,2},{1,2,3},{2,1,3},{3,1,2}};
        StageAnalyzer analyzer = new StageAnalyzer();
        for (int i = 0; i < orders.length; i++) {
            int[] values = orders[i];
            assertThat(analyzer.analyze(BigDecimal.valueOf(values[0]), BigDecimal.valueOf(values[1]), BigDecimal.valueOf(values[2]), BigDecimal.ZERO).stage()).isEqualTo(i + 1);
            assertThat(analyzer.transition(i + 1, (i + 1) % 6 + 1)).isEqualTo(Transition.FORWARD);
        }
        assertThat(analyzer.transition(1, 6)).isEqualTo(Transition.REVERSE);
        assertThat(analyzer.transition(1, 3)).isEqualTo(Transition.SKIPPED);
        assertThat(analyzer.transition(null, 1)).isEqualTo(Transition.UNKNOWN);
    }
    @Test void toleranceIsNotDisplayRounding() {
        var analyzer = new StageAnalyzer();
        assertThat(analyzer.analyze(n("100.0000001"), n("100"), n("90"), n("0.000001")).quality()).isEqualTo(Quality.BOUNDARY);
        assertThat(analyzer.analyze(n("100.00001"), n("100"), n("90"), n("0.000001")).stage()).isEqualTo(1);
        assertThat(analyzer.analyze(n("100"), n("100"), n("90"), BigDecimal.ZERO).quality()).isEqualTo(Quality.BOUNDARY);
        assertThat(analyzer.analyze(null, n("100"), n("90"), BigDecimal.ZERO).quality()).isEqualTo(Quality.INSUFFICIENT_DATA);
    }
    @Test void emaUsesSmaSeedAndFullPrecisionRecurrence() {
        var result = new EmaCalculator().calculate(List.of(n("1"), n("2"), n("3"), n("4"), n("8")), 3);
        assertThat(result.subList(0, 2)).containsOnlyNulls();
        assertThat(result.get(2)).isEqualByComparingTo("2");
        assertThat(result.get(3)).isEqualByComparingTo("3");
        assertThat(result.get(4)).isEqualByComparingTo("5.5");
        var thirds = new EmaCalculator().calculate(List.of(n("1"), n("2"), n("2")), 3).get(2);
        assertThat(thirds.precision()).isEqualTo(34);
    }
    @Test void slopesAndSignedGapDistanceDoNotConfuseCrossingWithExpansion() {
        assertThat(MovingAverageSlopeCalculator.percentChange().calculate(n("100"), n("101"))).isEqualByComparingTo("1");
        assertThat(MovingAverageSlopeCalculator.direction(n("0.01"), engine.config())).isEqualTo(Direction.FLAT);
        assertThat(MovingAverageSlopeCalculator.direction(n("-0.5"), engine.config())).isEqualTo(Direction.STRONG_DOWN);
        var analyzer = new MovingAverageGapAnalyzer();
        var contraction = analyzer.analyze(n("-1"), n("-2"), n("0.01"), false);
        assertThat(contraction.signedPercent()).isEqualByComparingTo("-1");
        assertThat(contraction.distancePercent()).isEqualByComparingTo("1");
        assertThat(contraction.state()).isEqualTo(GapState.CONTRACTING);
        assertThat(analyzer.analyze(n("1"), n("-1"), n("0.01"), false).state()).isEqualTo(GapState.CROSSING);
        assertThat(analyzer.analyze(n("2"), n("1"), n("0.01"), true).state()).isEqualTo(GapState.CROSSING);
    }
    private List<ScreenerResult.StagePoint> points(Integer... stages) {
        return IntStream.range(0, stages.length).mapToObj(i -> new ScreenerResult.StagePoint(LocalDate.of(2024,1,1).plusDays(i).atStartOfDay(), stages[i], stages[i] == null ? Quality.BOUNDARY : Quality.VALID)).toList();
    }
    @Test void previousBarAndPreviousDistinctStageAreDifferent() {
        var history = new StageHistoryAnalyzer().analyze(points(5, 6, 1, 1));
        assertThat(history.previousBarStage()).isEqualTo(1);
        assertThat(history.previousDistinctStage()).isEqualTo(6);
        assertThat(history.barsInStage()).isEqualTo(2);
        assertThat(history.barsSinceTransition()).isEqualTo(1);
        assertThat(history.transition()).isEqualTo(Transition.SAME);
        assertThat(history.lastTransition()).isEqualTo(Transition.FORWARD);
        assertThat(history.changed()).isFalse();
        assertThat(history.durationComplete()).isTrue();
    }
    @Test void boundaryAndWarmupBreakHistoryContinuity() {
        var history = new StageHistoryAnalyzer().analyze(points(6, null, 1, 1));
        assertThat(history.previousDistinctStage()).isNull();
        assertThat(history.barsInStage()).isEqualTo(2);
        assertThat(history.startedAt()).isNull();
        assertThat(history.lastTransition()).isEqualTo(Transition.UNKNOWN);
        assertThat(history.durationComplete()).isFalse();
        assertThat(new StageHistoryAnalyzer().analyze(points(1, 6)).lastTransition()).isEqualTo(Transition.REVERSE);
    }
    @Test void futureAndIncompleteBarsCannotChangeResultAndDuplicatesAreStable() {
        List<ScreenerBar> bars = rising(400);
        var cutoff = bars.get(299).closedAt();
        var base = analyze(bars.subList(0, 300), cutoff);
        assertThat(analyze(bars, cutoff)).isEqualTo(base);
        var shuffled = new ArrayList<>(bars); shuffled.add(bars.get(0)); Collections.reverse(shuffled);
        assertThat(analyze(shuffled, cutoff)).isEqualTo(base);
        assertThat(base.stage()).isEqualTo(1);
        assertThat(base.history()).hasSize(60);
        assertThat(base.trendScore()).isNull();
        assertThat(base.entryScore()).isNull();
        assertThat(base.signal()).isNull();
        assertThat(base.evaluationStatus()).isEqualTo(EvaluationStatus.NOT_EVALUATED);
    }
    @Test void conflictingDuplicatesAndMalformedBarsAreRejected() {
        var date = LocalDate.of(2024, 1, 1);
        assertThatThrownBy(() -> analyze(List.of(bar(date, 100), bar(date, 200)), date.plusDays(1).atStartOfDay())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ScreenerBar(date.atStartOfDay(), date.plusDays(1).atStartOfDay(), n("100"), n("90"), n("80"), n("85"), 1, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ScreenerTimeframe(Mode.SHORT_TERM, 2)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void warmupIsExplicitInsteadOfStageZero() {
        var result = analyze(rising(199), LocalDate.of(2026,1,1).atStartOfDay());
        assertThat(result.stage()).isNull();
        assertThat(result.quality()).isEqualTo(Quality.INSUFFICIENT_DATA);
        assertThat(result.slopes().shortSlope()).isNull();
        assertThat(result.shortMiddleGap().state()).isEqualTo(GapState.UNKNOWN);
    }
    @Test void calendar52WeeksIncludesLeapDayAndExcludesTodayFromBreakoutReference() {
        var date = LocalDate.of(2024, 3, 1);
        var boundary = date.minusWeeks(52);
        List<ScreenerBar> bars = new ArrayList<>();
        for (int i = 0; i <= 364; i++) bars.add(bar(boundary.plusDays(i), 100 + i));
        var result = analyze(bars, date.plusDays(1).atStartOfDay());
        var high = result.features().high52Week();
        assertThat(high.inclusiveWindowStart()).isEqualTo(boundary.plusDays(1));
        assertThat(high.referenceDate()).isEqualTo(date);
        assertThat(high.coverage()).isEqualTo(Coverage.FULL_WINDOW);
        assertThat(high.high()).isEqualByComparingTo("465");
        assertThat(high.previousHigh()).isEqualByComparingTo("464");
        assertThat(high.breakout()).isFalse(); // current close equals yesterday's high
        assertThat(result.features().volumeRatio20()).isEqualByComparingTo("1");
        assertThat(analyze(bars.subList(1, bars.size()), date.plusDays(1).atStartOfDay()).features().high52Week().coverage()).isEqualTo(Coverage.HISTORY_SHORT);
    }
    @Test void swingAndMinuteModesShareTheSameEmaButMinuteBarsMustBeClosed() {
        var daily = rising(300);
        LocalDateTime start = LocalDate.of(2025, 1, 2).atTime(9, 0);
        var minute = IntStream.range(0, daily.size()).mapToObj(i -> {
            var b = daily.get(i); var t = start.plusMinutes(i * 5L);
            return new ScreenerBar(t, t.plusMinutes(5), b.open(), b.high(), b.low(), b.close(), b.volume(), null);
        }).toList();
        var result = engine.analyze(new ScreenerInput("005930", new ScreenerTimeframe(Mode.SHORT_TERM, 5), minute.get(299).closedAt(), minute, List.of()));
        assertThat(result.ema()).isEqualTo(analyze(daily, daily.get(299).closedAt()).ema());
        assertThat(result.features().tradingAmountMillionWon()).isNull();
        assertThat(result.features().high52Week().coverage()).isEqualTo(Coverage.NO_DATA);
        var beforeClose = engine.analyze(new ScreenerInput("005930", new ScreenerTimeframe(Mode.SHORT_TERM, 5), minute.get(299).closedAt().minusSeconds(1), minute, List.of()));
        assertThat(beforeClose.availableBars()).isEqualTo(299);
    }
}
