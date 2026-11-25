package com.example.stockanalysis.unified.performance;

import static com.example.stockanalysis.unified.performance.PerformanceFixtures.*;
import static com.example.stockanalysis.unified.performance.PerformanceModels.*;
import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class PerformanceEngineTest {
    @Test void boundedGapBufferAllowsSmallGapWhileStillChargingSlippage() {
        var original=candidate("000001","ENTRY_CONDITIONAL");
        for(String upper:List.of("100","101")) {
            var scenario=new com.example.stockanalysis.engine.model.TradeScenario(bd("95"),bd(upper),List.of(bd("120")),bd("90"));
            var stock=new com.example.stockanalysis.unified.report.ReportModels.StockDecision(original.code(),original.name(),original.market(),original.disposition(),original.strategy(),original.evidence(),original.cautions(),scenario,original.netRewardRisk(),original.relativeReturn20(),original.liquidity(),original.dailyReturn(),true,1,false);
            var input=input("r",stock);var values=observations(input);
            replace(values,"000001",bar(dates().get(0),"100.5","105","99","102",10000));
            var trade=engine.evaluate(input,values,finalTime(),false).stocks().get(0).trade();
            if(upper.equals("100"))assertThat(trade.state()).isEqualTo("NO_ENTRY_GAP");
            else {
                assertThat(trade.entryPrice()).isEqualByComparingTo("100.55025");
                replace(values,"000001",bar(dates().get(0),"101","105","99","102",10000));
                assertThat(engine.evaluate(input,values,finalTime(),false).stocks().get(0).trade().state()).isEqualTo("NO_ENTRY_GAP");
            }
        }
    }
    final PerformanceEngine engine=new PerformanceEngine();
    @Test void measuresSamePeriodExcessAndExtremesWithoutHidingMissingPath(){
        var input=input("r",candidate("000001","WATCH"));var values=observations(input);
        var result=engine.evaluate(input,values,finalTime(),false);
        assertThat(result.stocks().get(0).horizons()).extracting(Horizon::days).containsExactly(1,3,5,10,20);
        var h=result.stocks().get(0).horizons().get(1);
        assertThat(h.excessReturnPercent()).isEqualByComparingTo("1");
        assertThat(h.maximumRisePercent()).isEqualByComparingTo("5");
        assertThat(h.maximumFallPercent()).isEqualByComparingTo("-5");
        values.removeIf(o->o.kind().equals("STOCK")&&o.date().equals(dates().get(1)));
        result=engine.evaluate(input,values,finalTime(),true);h=result.stocks().get(0).horizons().get(1);
        assertThat(h.priceReturnPercent()).isEqualByComparingTo("2");
        assertThat(h.pathState()).isEqualTo("MISSING");assertThat(h.maximumRisePercent()).isNull();
        assertThat(result.state()).isEqualTo("DATA_INCOMPLETE");
        values.removeIf(o->o.kind().equals("INDEX")&&o.date().equals(dates().get(2)));
        h=engine.evaluate(input,values,finalTime(),true).stocks().get(0).horizons().get(1);
        assertThat(h.marketState()).isEqualTo("MISSING");assertThat(h.excessReturnPercent()).isNull();
    }
    @Test void legacyPolicyKeepsThreeHorizonsAndDoesNotRequireNewIndexHistory(){
        var original=input("r",candidate("000001","WATCH"));var p=original.policy();
        var old=new Input(original.report(),new Policy("forward-evaluation-v1",p.roundTripFeeBps(),p.slippageBps(),p.neutralBandPercent(),p.holdingDays(),p.calendarVersion()),original.dates(),original.anchors(),original.captureMode(),original.registeredAt());
        var values=observations(old);values.removeIf(o->o.kind().equals("INDEX")&&!o.date().equals(dates().get(0)));
        var result=engine.evaluate(old,values,finalTime(),false);
        assertThat(result.state()).isEqualTo("COMPLETE");
        assertThat(result.stocks().get(0).horizons()).extracting(Horizon::days).containsExactly(5,10,20);
        assertThat(result.stocks().get(0).horizons()).allMatch(h->h.excessReturnPercent()==null);
    }
    @Test void startsNextSessionAndSeparatesWatchDiagnosticsFromTrades(){
        var input=input("r",candidate("000001","WATCH"));
        var before=engine.evaluate(input,List.of(),NOW,false);
        assertThat(before.stocks().get(0).trade().state()).isEqualTo("WATCH_ONLY");
        assertThat(before.stocks().get(0).horizons()).allMatch(h->h.state().equals("PENDING"));
        assertThat(before.metrics().winRatePercent()).isNull();
        var done=engine.evaluate(input,observations(input),finalTime(),false);
        assertThat(done.state()).isEqualTo("COMPLETE");assertThat(done.metrics().eligible()).isZero();
        assertThat(done.stocks().get(0).horizons()).allMatch(h->h.priceReturnPercent().compareTo(bd("2"))==0);
        assertThat(done.forecasts().get(0).matched()).isTrue();
    }
    @Test void gapOutsideEntryAndSinglePriceBarsAreNotFilled(){
        var input=input("r",candidate("000001","ENTRY_CONDITIONAL"));var values=observations(input);
        replace(values,"000001",bar(dates().get(0),"110","115","100","112",10000));
        var result=engine.evaluate(input,values,finalTime(),false);
        assertThat(result.stocks().get(0).trade().state()).isEqualTo("NO_ENTRY_GAP");
        assertThat(result.metrics().cohortReturnPercent()).isZero();assertThat(result.metrics().closed()).isZero();
        replace(values,"000001",bar(dates().get(0),"100","100","100","100",10000));
        assertThat(engine.evaluate(input,values,finalTime(),false).stocks().get(0).trade().state()).isEqualTo("NO_ENTRY_UNCERTAIN_FILL");
    }
    @Test void bothBarriersOnOneBarUseStopAndLaterGapStopUsesWorseOpen(){
        var input=input("r",candidate("000001","ENTRY_CONDITIONAL"));var values=observations(input);
        replace(values,"000001",bar(dates().get(0),"100","121","89","101",10000));
        var trade=engine.evaluate(input,values,finalTime(),false).stocks().get(0).trade();
        assertThat(trade.ambiguousBar()).isTrue();assertThat(trade.exitReason()).isEqualTo("STOP");
        assertThat(trade.netReturnPercent()).isLessThan(bd("-10"));
        values=observations(input);replace(values,"000001",bar(dates().get(1),"80","85","75","82",10000));
        trade=engine.evaluate(input,values,finalTime(),false).stocks().get(0).trade();
        assertThat(trade.exitReason()).isEqualTo("GAP_STOP");assertThat(trade.exitPrice()).isLessThan(bd("80"));
        assertThat(trade.netReturnPercent()).isLessThan(bd("-20"));
    }
    @Test void feesAndSlippageApplyOnceAndTimeExitUsesTwentiethSession(){
        var input=input("r",candidate("000001","ENTRY_CONDITIONAL"));var values=observations(input);
        replace(values,"000001",bar(dates().get(19),"100","105","95","100",10000));
        var trade=engine.evaluate(input,values,finalTime(),false).stocks().get(0).trade();
        assertThat(trade.exitDate()).isEqualTo(dates().get(19));assertThat(trade.exitReason()).isEqualTo("TIME_EXIT");
        assertThat(trade.netReturnPercent()).isBetween(bd("-0.41"),bd("-0.39"));
        var free=new Input(input.report(),new Policy(PerformanceEngine.VERSION,BigDecimal.ZERO,BigDecimal.ZERO,bd("0.3"),20,"test"),input.dates(),input.anchors(),input.captureMode(),input.registeredAt());
        var result=engine.evaluate(free,values,finalTime(),false);
        assertThat(result.metrics().flat()).isEqualTo(1);assertThat(result.metrics().wins()).isZero();
        assertThat(result.metrics().winRatePercent()).isZero();
    }
    @Test void missingIntermediateDayCannotTurnIntoLaterWinningExit(){
        var input=input("r",candidate("000001","ENTRY_CONDITIONAL"));var values=observations(input);
        values.removeIf(o->o.kind().equals("STOCK")&&o.date().equals(dates().get(1)));
        replace(values,"000001",bar(dates().get(2),"100","125","95","123",10000));
        var result=engine.evaluate(input,values,finalTime(),true);
        assertThat(result.state()).isEqualTo("DATA_INCOMPLETE");assertThat(result.stocks().get(0).trade().state()).isEqualTo("DATA_GAP");
        assertThat(result.metrics().closed()).isZero();assertThat(result.metrics().unresolved()).isEqualTo(1);
        assertThat(result.metrics().cohortReturnPercent()).isNull();assertThat(result.metrics().cohortMaxDrawdownPercent()).isNull();
    }
    @Test void openLossesRemainInCohortEquityEvenWithPerfectClosedWinRate(){
        var input=input("r",candidate("000001","ENTRY_CONDITIONAL"),candidate("000002","ENTRY_CONDITIONAL"));var values=observations(input);
        replace(values,"000001",bar(dates().get(0),"100","121","95","120",10000));
        replace(values,"000002",bar(dates().get(0),"100","101","91","92",10000));
        values.removeIf(o->o.date().isAfter(dates().get(0)));
        var result=engine.evaluate(input,values,collected(dates().get(0)),false);
        assertThat(result.metrics().winRatePercent()).isEqualByComparingTo("100");assertThat(result.metrics().open()).isEqualTo(1);
        assertThat(result.metrics().cohortReturnPercent()).isLessThan(bd("6"));
        assertThat(result.stocks().get(1).trade().markedReturnPercent()).isNegative();
    }
    @Test void suspendedLastDayDoesNotPretendTimeExitWasFilled(){
        var input=input("r",candidate("000001","ENTRY_CONDITIONAL"));var values=observations(input);
        replace(values,"000001",bar(dates().get(19),"80","80","80","80",0));
        var result=engine.evaluate(input,values,finalTime(),true);
        assertThat(result.stocks().get(0).trade().state()).isEqualTo("OPEN_UNLIQUIDATED");
        assertThat(result.metrics().cohortReturnPercent()).isLessThan(bd("-20"));
        assertThat(result.metrics().cohortMaxDrawdownPercent()).isGreaterThan(bd("20"));
        assertThat(result.metrics().closed()).isZero();assertThat(result.state()).isEqualTo("DATA_INCOMPLETE");
    }
    @Test void neutralBandBoundaryIsInclusiveAndFutureSourcesAreRejected(){
        var input=input("r");var values=observations(input);
        values.set(0,new Observation("INDEX","KOSPI",dates().get(0),"VALID",bar(dates().get(0),"100","101","99","100.3",0),"m",collected(dates().get(0)),"test"));
        assertThat(engine.evaluate(input,values,finalTime(),false).forecasts().get(0).actual()).isEqualTo("중립");
        assertThatThrownBy(()->engine.evaluate(input,values,NOW,false)).hasMessage("FUTURE_OBSERVATION");
    }
}
