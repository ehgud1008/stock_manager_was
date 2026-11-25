package com.example.stockanalysis.judgment;

import com.example.stockanalysis.engine.model.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static com.example.stockanalysis.judgment.TradeJudgment.Action.*;

class TradeJudgmentEngineTest {
    private final TradeJudgmentEngine engine = new TradeJudgmentEngine();
    private static final Instant NOW = Instant.parse("2026-10-22T15:00:00Z");
    private static final LocalDate DAY = LocalDate.of(2026, 10, 22);
    private static BigDecimal n(int v) { return BigDecimal.valueOf(v); }
    private TradeSignal signal(String type, TradeSignal.Side side, TradeSignal.State state, String entry) {
        return new TradeSignal(type, type, side, state, DAY, DAY, 0, 5, n(100), n(90), n(95), n(105), entry, null, List.of("근거"));
    }
    private TradeSignal buy() { return signal("BREAKOUT", TradeSignal.Side.BUY, TradeSignal.State.CONFIRMED, "IN_RANGE"); }
    private AnalysisResult analysis(String scope, String completeness, int trend, TradeSignal... signals) {
        return new AnalysisResult("analysis-v6-swing", AnalysisMode.SWING, n(85),
                List.of(new FactorResult("TREND", n(trend), n(trend), new BigDecimal("0.18"), "추세")),
                new StrategyDecision("SWING", "매수", StrategyAction.BUY_CANDIDATE, List.of()),
                new TradeScenario(n(95), n(105), List.of(n(140)), n(90)), List.of(),
                new SignalReport("swing-signals-v1", NOW, scope, completeness, List.of(signals), List.of()));
    }
    private TradeJudgment evaluate(AnalysisResult a, int price, String quality) {
        return engine.evaluate(new TradeJudgmentEngine.Input(a, n(price), DAY, NOW, quality));
    }
    @Test void sameSnapshotIsDeterministicAndScoreAloneCannotBuy() {
        var a = analysis("UNKNOWN", "READY", 70, buy());
        var result = evaluate(a, 100, "COMPLETE");
        assertThat(result).isEqualTo(evaluate(a, 100, "COMPLETE"));
        assertThat(result.notHeld().action()).isEqualTo(STRONG_BUY_CANDIDATE);
        assertThat(result.held().action()).isEqualTo(HOLD);
        assertThat(result.positionScope()).isEqualTo("UNKNOWN");
        assertThat(evaluate(analysis("UNKNOWN", "READY", 70), 100, "COMPLETE").notHeld().action()).isEqualTo(WATCH);
    }
    @Test void partialAndInsufficientDataNeverBecomeHoldOrBuy() {
        for (String quality : List.of("PARTIAL", "PENDING", "INVALID")) {
            var result = evaluate(analysis("UNKNOWN", "READY", 70, buy()), 100, quality);
            assertThat(result.notHeld().action()).isEqualTo(DEFER);
            assertThat(result.held().action()).isEqualTo(DEFER);
        }
        assertThat(evaluate(analysis("UNKNOWN", "INSUFFICIENT_DATA", 70, buy()), 100, "COMPLETE").held().action()).isEqualTo(DEFER);
    }
    @Test void stopOverridesProfitEvenWhenPatternSamplesAreInsufficient() {
        var stop = signal("POSITION_STOP", TradeSignal.Side.SELL, TradeSignal.State.CONFIRMED, "NOT_APPLICABLE");
        var target = signal("TARGET_REACHED", TradeSignal.Side.TAKE_PROFIT, TradeSignal.State.CONFIRMED, "NOT_APPLICABLE");
        assertThat(evaluate(analysis("HELD", "INSUFFICIENT_DATA", 70, stop, target), 100, "PARTIAL").held().action()).isEqualTo(EXIT_TRIGGERED);
        assertThat(evaluate(analysis("UNKNOWN", "READY", 70, stop, target), 100, "COMPLETE").held().action()).isEqualTo(HOLD);
        assertThat(evaluate(analysis("HELD", "READY", 70, target), 100, "COMPLETE").held().action()).isEqualTo(TAKE_PROFIT_REVIEW);
    }
    @Test void activeSellWinsAgainstBuyWithoutTreatingLowScoreAsSell() {
        var sell = signal("TREND_BREAKDOWN", TradeSignal.Side.SELL, TradeSignal.State.ACTIVE, "NOT_APPLICABLE");
        var result = evaluate(analysis("UNKNOWN", "READY", 70, buy(), sell), 100, "COMPLETE");
        assertThat(result.notHeld().action()).isEqualTo(AVOID);
        assertThat(result.held().action()).isEqualTo(SELL_PRIORITY);
        assertThat(evaluate(analysis("UNKNOWN", "READY", 30), 100, "COMPLETE").held().action()).isEqualTo(REVIEW_HOLDING);
    }
    @Test void expiredInvalidatedAndPreliminarySignalsDoNotTriggerActions() {
        for (var state : List.of(TradeSignal.State.EXPIRED, TradeSignal.State.INVALIDATED, TradeSignal.State.PRELIMINARY)) {
            var result = evaluate(analysis("HELD", "READY", 70,
                    signal("BREAKOUT", TradeSignal.Side.BUY, state, "IN_RANGE"),
                    signal("POSITION_STOP", TradeSignal.Side.SELL, state, "NOT_APPLICABLE")), 100, "COMPLETE");
            assertThat(result.notHeld().action()).isEqualTo(WATCH);
            assertThat(result.held().action()).isEqualTo(HOLD);
        }
    }
    @Test void bothEntryRangesAndSignalRestrictionsMustPass() {
        for (String restriction : List.of("CHASE_BLOCKED", "OUTSIDE_ENTRY_ZONE", "SELL_SIGNAL_CONFLICT", "SCENARIO_UNAVAILABLE")) {
            assertThat(evaluate(analysis("UNKNOWN", "READY", 70,
                    signal("BREAKOUT", TradeSignal.Side.BUY, TradeSignal.State.CONFIRMED, restriction)), 100, "COMPLETE").notHeld().action()).isEqualTo(WATCH);
        }
        for (int price : List.of(94, 106)) assertThat(evaluate(analysis("UNKNOWN", "READY", 70, buy()), price, "COMPLETE").notHeld().action()).isEqualTo(WATCH);
        var narrow = new TradeSignal("narrow", "BREAKOUT", TradeSignal.Side.BUY, TradeSignal.State.ACTIVE,
                DAY, DAY, 1, 5, n(99), n(90), n(99), n(100), "IN_RANGE", null, List.of());
        assertThat(evaluate(analysis("UNKNOWN", "READY", 70, narrow), 101, "COMPLETE").notHeld().action()).isEqualTo(WATCH);
    }
    @Test void staleFutureAndMismatchedTimesDefer() {
        var a = analysis("UNKNOWN", "READY", 70, buy());
        for (LocalDate day : List.of(DAY.minusDays(10), DAY.plusDays(2)))
            assertThat(engine.evaluate(new TradeJudgmentEngine.Input(a, n(100), day, NOW, "COMPLETE")).notHeld().action()).isEqualTo(DEFER);
        assertThat(engine.evaluate(new TradeJudgmentEngine.Input(a, n(100), DAY, NOW.plusSeconds(1), "COMPLETE")).held().action()).isEqualTo(DEFER);
    }
    @Test void openPriceExclusionBlocksEntryButDoesNotForceLiquidation() {
        var a = analysis("UNKNOWN", "READY", 70, buy());
        var excluded = new AnalysisResult(a.engineVersion(), a.mode(), a.totalScore(), a.factors(),
                new StrategyDecision("OPEN_PRICE_EXCLUDED", "시가 제외", StrategyAction.AVOID, List.of()), null, a.warnings(), a.signalReport());
        var result = evaluate(excluded, 100, "COMPLETE");
        assertThat(result.notHeld().action()).isEqualTo(AVOID);
        assertThat(result.held().action()).isEqualTo(HOLD);
    }
    @Test void missingScenarioOrScoreCannotProduceBuy() {
        var a = analysis("UNKNOWN", "READY", 70, buy());
        var missingScenario = new AnalysisResult(a.engineVersion(), a.mode(), a.totalScore(), a.factors(), a.strategy(), null, a.warnings(), a.signalReport());
        assertThat(evaluate(missingScenario, 100, "COMPLETE").notHeld().action()).isEqualTo(WATCH);
        var missingScore = new AnalysisResult(a.engineVersion(), a.mode(), null, a.factors(), a.strategy(), a.scenario(), a.warnings(), a.signalReport());
        assertThat(evaluate(missingScore, 100, "COMPLETE").notHeld().action()).isEqualTo(DEFER);
    }
    @Test void rewardRiskBoundaryChangesCandidateStrength() {
        var a = analysis("UNKNOWN", "READY", 70, buy());
        for (int target : List.of(134, 135)) {
            var changed = new AnalysisResult(a.engineVersion(), a.mode(), a.totalScore(), a.factors(), a.strategy(),
                    new TradeScenario(n(95), n(105), List.of(n(target)), n(90)), a.warnings(), a.signalReport());
            assertThat(evaluate(changed, 100, "COMPLETE").notHeld().action()).isEqualTo(target == 135 ? STRONG_BUY_CANDIDATE : BUY_CANDIDATE);
        }
    }
}
