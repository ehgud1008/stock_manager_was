package com.example.stockanalysis.unified.performance;

import static com.example.stockanalysis.unified.performance.PerformanceFixtures.*;
import static org.assertj.core.api.Assertions.*;
import com.example.stockanalysis.unified.performance.PerformanceModels.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ScenarioMonitoringEngineTest {
    final ScenarioMonitoringEngine engine=new ScenarioMonitoringEngine();
    final Anchor anchor=new Anchor("000001","KOSPI","KIWOOM_KRX_DAILY_ADJUSTED",base());
    ScenarioMonitoringModels.Stock evaluate(List<Observation> observations,Instant now) {
        return engine.evaluate(candidate("000001","ENTRY_CONDITIONAL"),anchor,dates(),observations,now,true,true);
    }
    Observation row(int i,String open,String high,String low,String close) {
        return observation("000001",bar(dates().get(i),open,high,low,close,1000));
    }
    @Test void bComparisonUsesTheSameEntryCostsAndOutcomeRulesAsOperatingSelection(){
        var history=List.of(row(0,"100","110","95","106"),row(1,"106","120","100","119"));
        var a=evaluate(history,collected(dates().get(1)));
        var b=engine.evaluate(candidate("000001","RESEARCH_B"),anchor,dates(),history,collected(dates().get(1)),true,true);
        assertThat(b.status()).isEqualTo("TARGET_REACHED");
        assertThat(b.outcome()).isEqualTo(a.outcome());
        assertThat(b.disposition()).isEqualTo("RESEARCH_B");
    }
    @Test void selectedStockIsVisibleBeforeAnyFollowingSessionWithFrozenScenario() {
        var original=candidate("000001","ENTRY_CONDITIONAL");
        var result=engine.evaluate(original,anchor,dates(),List.of(),NOW,true,true);
        assertThat(result.status()).isEqualTo("WAITING_ENTRY_DAY");
        assertThat(result.scenario()).isSameAs(original.scenario());
        assertThat(result.selectionClose()).isEqualByComparingTo("100");
        assertThat(result.latestClose()).isNull();assertThat(result.history()).isEmpty();
    }
    @Test void inclusiveEntryBoundsAndFirstTargetAreObservedWithoutInventingFills() {
        for(String open:List.of("95","104")) {
            var result=evaluate(List.of(row(0,open,"110","94","106")),collected(dates().get(0)));
            assertThat(result.status()).isEqualTo("IN_PROGRESS");assertThat(result.entryDate()).isEqualTo(dates().get(0));
        }
        var result=evaluate(List.of(row(0,"100","110","95","106"),row(1,"106","120","100","119")),collected(dates().get(1)));
        assertThat(result.status()).isEqualTo("TARGET_REACHED");assertThat(result.eventDate()).isEqualTo(dates().get(1));
        assertThat(result.targetDistancePercent()).isPositive();assertThat(result.stopDistancePercent()).isPositive();
    }
    @Test void outOfRangeEntryNeverTurnsIntoLaterTargetSuccess() {
        var result=evaluate(List.of(row(0,"106","110","100","108"),row(1,"108","125","100","120")),collected(dates().get(1)));
        assertThat(result.status()).isEqualTo("NO_ENTRY");assertThat(result.entryDate()).isNull();
        assertThat(result.history().get(1).targetTouched()).isTrue();assertThat(result.eventDate()).isNull();
    }
    @Test void simultaneousTouchesAreUnknownButAnOpeningGapProvidesOrdering() {
        var result=evaluate(List.of(row(0,"100","121","89","101")),collected(dates().get(0)));
        assertThat(result.status()).isEqualTo("AMBIGUOUS");
        assertThat(result.history().get(0).targetTouched()).isTrue();assertThat(result.history().get(0).stopTouched()).isTrue();
        var first=row(0,"100","110","95","108");
        assertThat(evaluate(List.of(first,row(1,"85","121","80","110")),collected(dates().get(1))).status()).isEqualTo("STOP_REACHED");
        assertThat(evaluate(List.of(first,row(1,"125","130","85","110")),collected(dates().get(1))).status()).isEqualTo("TARGET_REACHED");
    }
    @Test void knownTerminalEventIsNotOverwrittenByLaterOppositeTouchOrMissingData() {
        var result=evaluate(List.of(row(0,"100","121","95","120"),row(1,"100","110","85","88")),collected(dates().get(2)));
        assertThat(result.status()).isEqualTo("TARGET_REACHED");assertThat(result.eventDate()).isEqualTo(dates().get(0));
        assertThat(result.dataState()).isEqualTo("WAITING_DATA");assertThat(result.latestPriceDate()).isEqualTo(dates().get(1));
    }
    @Test void missingMiddleOrChangedPriceBasisCannotBeReportedAsScenarioSuccess() {
        var result=evaluate(List.of(row(0,"100","110","95","108"),row(2,"108","125","100","120")),collected(dates().get(2)));
        assertThat(result.status()).isEqualTo("DATA_GAP");assertThat(result.eventDate()).isNull();
        var invalid=new Observation("STOCK","000001",dates().get(0),"PRICE_BASIS_CHANGED",null,"source",collected(dates().get(0)),"changed");
        assertThat(evaluate(List.of(invalid),collected(dates().get(0))).status()).isEqualTo("DATA_GAP");
    }
    @Test void currentSessionWithoutDataIsWaitingAndDoesNotUseSelectionCloseAsCurrent() {
        var result=evaluate(List.of(),collected(dates().get(0)));
        assertThat(result.status()).isEqualTo("WAITING_DATA");assertThat(result.dataState()).isEqualTo("WAITING_DATA");
        assertThat(result.latestClose()).isNull();assertThat(result.selectionReturnPercent()).isNull();
    }
    @Test void watchOnlyStocksHaveObservedTouchesButNoAssumedEntryOrOutcome() {
        var result=engine.evaluate(candidate("000001","WATCH"),anchor,dates(),List.of(row(0,"100","121","85","110")),collected(dates().get(0)),true,true);
        assertThat(result.status()).isEqualTo("WATCH_ONLY");assertThat(result.entryDate()).isNull();assertThat(result.eventDate()).isNull();
        assertThat(result.history().get(0).targetTouched()).isTrue();
    }
    @Test void haltedOrSinglePriceBarsAndUnverifiedAnchorsAreNotConfirmedEvents() {
        for(var candle:List.of(bar(dates().get(0),"100","125","85","110",0),bar(dates().get(0),"100","100","100","100",1000))) {
            var result=evaluate(List.of(observation("000001",candle)),collected(dates().get(0)));
            assertThat(result.status()).isEqualTo("DATA_GAP");assertThat(result.history().get(0).targetTouched()).isFalse();
        }
        assertThat(engine.evaluate(candidate("000001","ENTRY_CONDITIONAL"),null,dates(),List.of(),NOW,true,true).status()).isEqualTo("BASELINE_MISSING");
        assertThat(engine.evaluate(candidate("000001","ENTRY_CONDITIONAL"),anchor,dates(),List.of(),NOW,false,true).status()).isEqualTo("CALENDAR_UNAVAILABLE");
        assertThat(engine.evaluate(candidate("000001","ENTRY_CONDITIONAL"),anchor,dates(),List.of(),NOW,true,false).status()).isEqualTo("REPORT_LATE");
    }
    @Test void twentyConfirmedSessionsEndObservationWithoutAssumingTargetWasReached() {
        var observations=dates().stream().map(d->observation("000001",bar(d,"100","110","95","105",1000))).toList();
        var result=evaluate(observations,finalTime());
        assertThat(result.status()).isEqualTo("PERIOD_ENDED");assertThat(result.observedDays()).isEqualTo(20);
    }
    @Test void futureOrDuplicateObservationsAreRejected() {
        var first=row(0,"100","110","95","108");
        assertThatThrownBy(()->evaluate(List.of(first),NOW)).hasMessage("MONITORING_OBSERVATION_IDENTITY");
        assertThatThrownBy(()->evaluate(List.of(first,first),collected(dates().get(0)))).hasMessage("DUPLICATE_MONITORING_OBSERVATION");
    }
    @Test void costsEntryExitAndOutcomeAreExplicitAndSharedWithPerformance() {
        var result=evaluate(List.of(row(0,"100","110","95","106"),row(1,"106","120","100","119")),collected(dates().get(1)));
        var o=result.outcome();
        assertThat(o.entryPrice()).isEqualByComparingTo("100.05");
        assertThat(o.exitPrice()).isEqualByComparingTo("119.94");
        assertThat(o.netReturnPercent()).isEqualByComparingTo(TradeOutcomeRules.net(bd("100.05"),bd("119.94"),bd("30")));
        assertThat(o.holdingDays()).isEqualTo(2);assertThat(o.summary()).contains("1차 목표 달성","모의 수익률");
        var gap=evaluate(List.of(row(0,"100","110","95","108"),row(1,"85","110","80","90")),collected(dates().get(1))).outcome();
        assertThat(gap.exitReason()).isEqualTo("GAP_STOP");assertThat(gap.exitPrice()).isEqualByComparingTo("84.9575");
    }
    @Test void ambiguityAndMissingPathNeverBecomeConfirmedProfit() {
        var ambiguous=evaluate(List.of(row(0,"100","121","89","101")),collected(dates().get(0))).outcome();
        assertThat(ambiguous.netReturnPercent()).isNull();assertThat(ambiguous.exitDate()).isNull();
        assertThat(ambiguous.conservativeReturnPercent()).isNegative();
        var missing=evaluate(List.of(row(0,"100","110","95","108"),row(2,"108","125","100","120")),collected(dates().get(2))).outcome();
        assertThat(missing.netReturnPercent()).isNull();assertThat(missing.markedReturnPercent()).isNull();
        assertThat(evaluate(List.of(row(0,"105","110","95","106")),collected(dates().get(0))).status()).isEqualTo("NO_ENTRY");
    }
    @Test void terminalReturnsStayFrozenAndTimeExitUsesNetResult() {
        var target=evaluate(List.of(row(0,"100","121","95","120")),collected(dates().get(0))).outcome();
        var later=evaluate(List.of(row(0,"100","121","95","120"),row(1,"100","110","85","88")),collected(dates().get(2))).outcome();
        assertThat(later).isEqualTo(target);
        var flatPrice=dates().stream().map(d->observation("000001",bar(d,"100","110","95","100",1000))).toList();
        var time=evaluate(flatPrice,finalTime()).outcome();
        assertThat(time.result()).isEqualTo("TIME_LOSS");assertThat(time.exitReason()).isEqualTo("TIME_EXIT");
        assertThat(time.netReturnPercent()).isNegative();
        var watch=engine.evaluate(candidate("000001","WATCH"),anchor,dates(),flatPrice,finalTime(),true,true).outcome();
        assertThat(watch.entryPrice()).isNull();assertThat(watch.netReturnPercent()).isNull();
    }
    @Test void watchSelectionFailsAtStopWithoutCreatingTradeLoss() {
        var result=engine.evaluate(candidate("000001","WATCH"),anchor,dates(),
                List.of(row(0,"100","110","95","105"),row(1,"100","110","90","92")),collected(dates().get(1)),true,true);
        assertThat(result.status()).isEqualTo("WATCH_ONLY");assertThat(result.outcome().netReturnPercent()).isNull();
        assertThat(result.selectionOutcome().result()).isEqualTo("FAILURE_STOP");
        assertThat(result.selectionOutcome().eventDate()).isEqualTo(dates().get(1));
    }
    @Test void noEntryCanStillHaveSuccessfulSelectionAndFirstEventIsFrozen() {
        var result=evaluate(List.of(row(0,"110","120","100","119"),row(1,"100","110","85","88")),collected(dates().get(2)));
        assertThat(result.status()).isEqualTo("NO_ENTRY");assertThat(result.outcome().netReturnPercent()).isNull();
        assertThat(result.selectionOutcome().result()).isEqualTo("SUCCESS");
        assertThat(result.selectionOutcome().eventDate()).isEqualTo(dates().get(0));
        var failed=evaluate(List.of(row(0,"100","110","90","95"),row(1,"100","125","95","120")),collected(dates().get(1)));
        assertThat(failed.selectionOutcome().result()).isEqualTo("FAILURE_STOP");
    }
    @Test void selectionTimeoutIsFailureEvenWhenTradeMadeProfitAndDayNineteenIsNotFinal() {
        var observations=dates().stream().map(d->observation("000001",bar(d,"100","110","95","105",1000))).toList();
        var result=evaluate(observations,finalTime());
        assertThat(result.outcome().result()).isEqualTo("TIME_PROFIT");
        assertThat(result.selectionOutcome().result()).isEqualTo("FAILURE_TIMEOUT");
        assertThat(result.selectionOutcome().eventCloseReturnPercent()).isEqualByComparingTo("5");
        assertThat(result.selectionOutcome().eventDate()).isEqualTo(dates().get(19));
        assertThat(evaluate(observations.subList(0,19),collected(dates().get(18))).selectionOutcome().result()).isEqualTo("IN_PROGRESS");
        var lastTarget=new ArrayList<>(observations);lastTarget.set(19,row(19,"105","120","100","119"));
        assertThat(evaluate(lastTarget,finalTime()).selectionOutcome().result()).isEqualTo("SUCCESS");
    }
    @Test void firstSessionOpeningGapOrdersSelectionButIntradayDoubleTouchDoesNot() {
        for(String open:List.of("125","85")) {
            var result=evaluate(List.of(row(0,open,"130","80","110")),collected(dates().get(0)));
            assertThat(result.selectionOutcome().result()).isEqualTo(open.equals("125")?"SUCCESS":"FAILURE_STOP");
        }
        assertThat(evaluate(List.of(row(0,"100","120","90","105")),collected(dates().get(0))).selectionOutcome().result()).isEqualTo("AMBIGUOUS");
    }
    @Test void selectionMissingPathOrInvalidBaselineNeverBecomesFailureOrSuccess() {
        assertThat(evaluate(List.of(row(1,"100","125","95","120")),collected(dates().get(1))).selectionOutcome().result()).isEqualTo("DEFERRED");
        assertThat(evaluate(List.of(),collected(dates().get(0))).selectionOutcome().result()).isEqualTo("WAITING_DATA");
        assertThat(evaluate(List.of(),NOW).selectionOutcome().result()).isEqualTo("WAITING");
        assertThat(engine.evaluate(candidate("000001","WATCH"),anchor,dates(),List.of(),NOW,true,false).selectionOutcome().result()).isEqualTo("DEFERRED");
        assertThat(engine.evaluate(candidate("000001","WATCH"),null,dates(),List.of(),NOW,true,true).selectionOutcome().result()).isEqualTo("DEFERRED");
    }
}
