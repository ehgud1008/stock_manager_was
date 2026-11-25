package com.example.stockanalysis.engine.signal;

import com.example.stockanalysis.engine.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static com.example.stockanalysis.engine.model.TradeSignal.State.*;

class SwingSignalEngineTest {
    @Test void explicitEodInputConfirmsTodayWhileLegacyInputRemainsPreliminary() {
        var original=input(breakout(),false,null,134);
        var eod=new AnalysisInput(original.stock(),original.mode(),original.baseDate(),original.baseDate().atTime(20,11).atZone(SEOUL).toInstant(),
                original.dailyCandles(),original.weeklyCandles(),original.monthlyCandles(),original.observations(),null,original.baseDate());
        assertThat(find(engine.evaluate(eod,scenario()),"BREAKOUT").state()).isEqualTo(CONFIRMED);
        assertThat(find(engine.evaluate(original,scenario()),"BREAKOUT").state()).isEqualTo(PRELIMINARY);
    }
    private final SwingSignalEngine engine = new SwingSignalEngine();
    private static final LocalDate FIRST = LocalDate.of(2026, 1, 1);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static BigDecimal n(double v) { return BigDecimal.valueOf(v); }
    private List<DailyCandle> base() {
        List<DailyCandle> bars = new ArrayList<>();
        for (int i = 0; i < 65; i++) add(bars, 100 + i * .5, 1000);
        return bars;
    }
    private void add(List<DailyCandle> b, double close, long volume) {
        b.add(new DailyCandle(FIRST.plusDays(b.size()), n(close), n(close + .4), n(close - .8), n(close), volume));
    }
    private List<DailyCandle> breakout() { var b = base(); add(b, 134, 1800); return b; }
    private AnalysisInput input(List<DailyCandle> b, boolean complete, PositionContext p, double current) {
        var date = b.get(b.size() - 1).businessDate();
        var cutoff = date.plusDays(complete ? 1 : 0).atTime(12, 0).atZone(SEOUL).toInstant();
        return new AnalysisInput(new StockSnapshot("005930", "test", "KOSPI", n(current)), AnalysisMode.SWING,
                date, cutoff, b, List.of(), List.of(), Map.of(), p);
    }
    private TradeScenario scenario() { return new TradeScenario(n(132), n(135), List.of(n(145)), n(128)); }
    private SignalReport report(List<DailyCandle> b) { return engine.evaluate(input(b, true, null, b.get(b.size()-1).close().doubleValue()), scenario()); }
    private TradeSignal find(SignalReport r, String type) { return r.signals().stream().filter(s -> s.type().equals(type)).findFirst().orElseThrow(); }
    private PositionContext position(PositionContext.StopPolicy policy, double stop, List<BigDecimal> targets) {
        return new PositionContext(true, FIRST.plusDays(64).atTime(10, 0).atZone(SEOUL).toInstant(), n(132), n(stop), targets, policy);
    }

    @Test void breakoutIsConfirmedOnlyAfterDailyClosure() {
        var b = breakout();
        assertThat(find(report(b), "BREAKOUT").state()).isEqualTo(CONFIRMED);
        var preliminary = find(engine.evaluate(input(b, false, null, 134), scenario()), "BREAKOUT");
        assertThat(preliminary.state()).isEqualTo(PRELIMINARY);
        assertThat(preliminary.entryStatus()).isEqualTo("AWAIT_CLOSE");
    }
    @Test void maintainsStableIdAndFrozenPricesThenExpiresAfterFiveClosedBars() {
        var b = breakout(); var first = find(report(b), "BREAKOUT");
        add(b, 134, 1000); var active = find(report(b), "BREAKOUT");
        assertThat(active.state()).isEqualTo(ACTIVE);
        assertThat(active.id()).isEqualTo(first.id());
        assertThat(active.invalidationPrice()).isEqualTo(first.invalidationPrice());
        assertThat(active.entryTo()).isEqualTo(first.entryTo());
        for (int i = 0; i < 4; i++) add(b, 134, 1000);
        var expired = find(report(b), "BREAKOUT");
        assertThat(expired.state()).isEqualTo(EXPIRED);
        assertThat(expired.ageBars()).isEqualTo(5);
        assertThat(expired.endedOn()).isEqualTo(FIRST.plusDays(70));
    }
    @Test void ongoingBreakoutConditionDoesNotGenerateDailyNewEvents() {
        var b = breakout(); var first = find(report(b), "BREAKOUT"); add(b, 136, 2000);
        assertThat(find(report(b), "BREAKOUT").id()).isEqualTo(first.id());
        assertThat(find(report(b), "BREAKOUT").state()).isEqualTo(ACTIVE);
    }
    @Test void brokenSupportInvalidatesBuyAndConfirmsBreakoutFailure() {
        var b = breakout(); add(b, 129, 1800);
        var r = report(b);
        assertThat(find(r, "BREAKOUT").state()).isEqualTo(INVALIDATED);
        assertThat(find(r, "BREAKOUT_FAILURE").state()).isEqualTo(CONFIRMED);
        assertThat(r.positionScope()).isEqualTo("UNKNOWN");
        assertThat(r.signals()).noneMatch(s -> s.type().equals("POSITION_STOP"));
    }
    @Test void weakVolumeCannotConfirmBreakout() {
        var b = base(); add(b, 134, 1000);
        assertThat(report(b).signals()).noneMatch(s -> s.type().equals("BREAKOUT"));
    }
    @Test void breakoutRequiresBothVolumeAndUpperCloseWithInclusiveBoundaries() {
        for(long volume:List.of(1499L,1500L)) {
            var b=base();
            b.add(new DailyCandle(FIRST.plusDays(b.size()),n(133),n(135),n(132.5),n(134),volume));
            assertThat(report(b).signals().stream().anyMatch(s->s.type().equals("BREAKOUT"))).isEqualTo(volume==1500);
        }
        for(double high:List.of(135.01,140.0,134.0)) {
            var b=base();
            b.add(new DailyCandle(FIRST.plusDays(b.size()),n(134),n(high),n(high==134?134:132.5),n(134),2000));
            assertThat(report(b).signals()).noneMatch(s->s.type().equals("BREAKOUT"));
        }
    }
    @Test void pullbackMustRecoverRecentHighInsideAnUptrend() {
        var b = base(); add(b, 130.5, 900); add(b, 130, 900); add(b, 131, 900); add(b, 133, 1000);
        assertThat(find(report(b), "PULLBACK_RECOVERY").state()).isEqualTo(CONFIRMED);
    }
    @Test void retestRequiresASeparateSupportBarAfterBreakout() {
        var b = breakout();
        assertThat(report(b).signals()).noneMatch(s -> s.type().equals("BREAKOUT_RETEST"));
        add(b, 132.6, 900); add(b, 134, 1100);
        assertThat(find(report(b), "BREAKOUT_RETEST").state()).isEqualTo(CONFIRMED);
    }
    @Test void bearishTrendAndSupportBreakConfirmStructuralSell() {
        var b = new ArrayList<DailyCandle>();
        for (int i = 0; i < 65; i++) add(b, 160 - i * .3, 1000);
        add(b, 137, 1500);
        assertThat(find(report(b), "TREND_BREAKDOWN").side()).isEqualTo(TradeSignal.Side.SELL);
        assertThat(find(report(b), "TREND_BREAKDOWN").state()).isEqualTo(CONFIRMED);
    }
    @Test void chaseAndMissingScenarioAreExplicitlyBlocked() {
        var b = breakout();
        assertThat(find(engine.evaluate(input(b, true, null, 150), scenario()), "BREAKOUT").entryStatus()).isEqualTo("CHASE_BLOCKED");
        assertThat(find(engine.evaluate(input(b, true, null, 134), null), "BREAKOUT").entryStatus()).isEqualTo("SCENARIO_UNAVAILABLE");
    }
    @Test void duplicateDatesAndShortHistoryDoNotProduceSignals() {
        var b = breakout(); b.add(b.get(b.size()-1));
        assertThat(report(b).completeness()).isEqualTo("INSUFFICIENT_DATA");
        assertThat(report(b).signals()).isEmpty();
        assertThat(report(base().subList(0, 60)).signals()).isEmpty();
    }
    @Test void touchAndCloseStopPoliciesAreDifferent() {
        var b = breakout();
        var touch = engine.evaluate(input(b, false, position(PositionContext.StopPolicy.TOUCH, 133.8, List.of()), 134), scenario());
        assertThat(find(touch, "POSITION_STOP").state()).isEqualTo(CONFIRMED);
        assertThat(find(touch, "BREAKOUT").entryStatus()).isEqualTo("AWAIT_CLOSE");
        var close = engine.evaluate(input(b, false, position(PositionContext.StopPolicy.CLOSE, 133.8, List.of()), 134), scenario());
        assertThat(close.signals()).noneMatch(s -> s.type().equals("POSITION_STOP"));
    }
    @Test void closeStopOnUnfinishedBarRemainsPreliminary() {
        var b = breakout();
        var r = engine.evaluate(input(b, false, position(PositionContext.StopPolicy.CLOSE, 135, List.of()), 134), scenario());
        assertThat(find(r, "POSITION_STOP").state()).isEqualTo(PRELIMINARY);
    }
    @Test void stopDominatesSameBarTargetAndBlocksLiveBuy() {
        var b = breakout();
        var r = engine.evaluate(input(b, true, position(PositionContext.StopPolicy.TOUCH, 133.8, List.of(n(134.2))), 134), scenario());
        assertThat(find(r, "POSITION_STOP").state()).isEqualTo(CONFIRMED);
        assertThat(find(r, "BREAKOUT").entryStatus()).isEqualTo("SELL_SIGNAL_CONFLICT");
        assertThat(r.signals()).noneMatch(s -> s.type().equals("TARGET_REACHED"));
    }
    @Test void targetReachedIsNotABearishSell() {
        var b = breakout();
        var r = engine.evaluate(input(b, true, position(PositionContext.StopPolicy.TOUCH, 125, List.of(n(134.2))), 134), scenario());
        assertThat(find(r, "TARGET_REACHED").side()).isEqualTo(TradeSignal.Side.TAKE_PROFIT);
    }
    @Test void entryDayExtremesAreExcludedEvenForMidnightEntry() {
        var b = breakout();
        var p = new PositionContext(true, FIRST.plusDays(65).atStartOfDay(SEOUL).toInstant(), n(132), n(135), List.of(n(134.2)), PositionContext.StopPolicy.TOUCH);
        var r = engine.evaluate(input(b, true, p, 134), scenario());
        assertThat(r.signals()).noneMatch(s -> s.type().equals("POSITION_STOP") || s.type().equals("TARGET_REACHED"));
    }
    @Test void notHeldNeverProducesPositionEvents() {
        var b = breakout(); var p = new PositionContext(false, null, null, null, List.of(), null);
        var r = engine.evaluate(input(b, true, p, 134), scenario());
        assertThat(r.positionScope()).isEqualTo("NOT_HELD");
        assertThat(r.signals()).noneMatch(s -> s.type().equals("POSITION_STOP") || s.type().equals("TARGET_REACHED"));
    }
    @Test void shortHistoryStillChecksUserProvidedPositionStops() {
        var b = breakout(); var recent = b.subList(65, 66);
        var r = engine.evaluate(input(recent, true, position(PositionContext.StopPolicy.TOUCH, 133.8, List.of()), 134), scenario());
        assertThat(r.completeness()).isEqualTo("INSUFFICIENT_DATA");
        assertThat(find(r, "POSITION_STOP").state()).isEqualTo(CONFIRMED);
    }
    @Test void incompleteFutureBarCannotInvalidateAnAlreadyConfirmedBuy() {
        var b = breakout(); add(b, 129, 1800);
        var r = engine.evaluate(input(b, false, null, 129), scenario());
        assertThat(find(r, "BREAKOUT").state()).isEqualTo(CONFIRMED);
        assertThat(find(r, "BREAKOUT_FAILURE").state()).isEqualTo(PRELIMINARY);
    }
}
