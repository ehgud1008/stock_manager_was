package com.example.stockanalysis.unified.portfolio;

import static com.example.stockanalysis.unified.portfolio.PortfolioFixtures.*;
import static com.example.stockanalysis.unified.portfolio.PortfolioModels.*;
import static org.assertj.core.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PortfolioEngineTest {
    final PortfolioEngine engine=new PortfolioEngine();
    @Test void tierComparisonPreservesRecordedTierAndCannotLeakIntoBaseline() {
        var original=stock("002");
        var common=new com.example.stockanalysis.judgment.NewEntryAssessment("new-entry-v2-tier",com.example.stockanalysis.judgment.NewEntryAssessment.Status.WATCH,List.of("B 비교"),null,original.scenario(),bd("2"),List.of(),
                new com.example.stockanalysis.judgment.SuitabilityTier("suitability-tier-v1","B",bd("75"),List.of()),
                new com.example.stockanalysis.judgment.NewEntryAssessment.Timing(com.example.stockanalysis.judgment.NewEntryAssessment.Status.ELIGIBLE,List.of(),false));
        var b=new com.example.stockanalysis.unified.report.ReportModels.StockDecision(original.code(),original.name(),original.market(),original.disposition(),original.strategy(),original.evidence(),original.cautions(),original.scenario(),original.netRewardRisk(),original.relativeReturn20(),original.liquidity(),original.dailyReturn(),original.aboveEma20(),original.stage(),original.transition61(),common);
        var c=cohort("a",DAY,stock("001"),b);
        var expanded=new Cohort(c.report(),List.of(stock("001")),List.of(stock("001")),c.anchors(),Map.of("TIER_A_B",List.of(stock("001"),b),"TIER_B_ONLY",List.of(b)),Map.of(),null);
        var in=input(5,"20",expanded);var result=engine.evaluate(in,observations(in),end(),false);
        assertThat(result.variants().get(0).metrics().entered()).isEqualTo(1);
        var both=result.variants().stream().filter(v->v.name().equals("TIER_A_B")).findFirst().orElseThrow();
        assertThat(both.metrics().entered()).isEqualTo(2);assertThat(both.groups().get("TIER/B").entered()).isEqualTo(1);
        var only=result.variants().stream().filter(v->v.name().equals("TIER_B_ONLY")).findFirst().orElseThrow();
        assertThat(only.trades()).singleElement().satisfies(t->assertThat(t.evidenceGroups()).containsEntry("TIER","B"));
        assertThat(only.maximumDrawdownPercent()).isNotNull();
    }
    @Test void extraVariantsUseSameCashRulesAndMissingPricesNeverBecomeZeroReturn() {
        var c=cohort("a",DAY,stock("001"),stock("002"));
        var expanded=new Cohort(c.report(),List.of(stock("001")),List.of(stock("001")),c.anchors(),
                Map.of("NO_RELATIVE_STRENGTH",List.of(stock("001"),stock("002"))),Map.of(),null);
        var in=input(5,"20",expanded);var data=observations(in);var result=engine.evaluate(in,data,end(),false);
        assertThat(result.variants()).hasSize(6);
        var variant=result.variants().stream().filter(v->v.name().equals("NO_RELATIVE_STRENGTH")).findFirst().orElseThrow();
        assertThat(variant.metrics().entered()).isEqualTo(2);assertThat(variant.marketFilter()).isTrue();
        assertThat(result.variants().get(0).metrics().entered()).isEqualTo(1);
        data.removeIf(o->o.key().equals("a:STOCK:002:"+dates().get(0)));
        var missing=engine.evaluate(in,data,end(),true);
        assertThat(missing.variants().get(0).returnPercent()).isNotNull();
        assertThat(missing.variants().stream().filter(v->v.name().equals("NO_RELATIVE_STRENGTH")).findFirst().orElseThrow().returnPercent()).isNull();
    }
    @Test void comparisonRequiresAllCohortsAndLegacyVersionKeepsOriginalFourVariants() throws Exception {
        var c=cohort("a",DAY,stock("001"));
        var expanded=new Cohort(c.report(),c.baseline(),c.withoutMarketFilter(),c.anchors(),Map.of("SIGNAL_AGE4",c.baseline()),Map.of(),null);
        var in=input(5,"20",expanded,cohort("b",DAY.plusDays(1),stock("002")));
        assertThat(engine.evaluate(in,observations(in),end(),false).variants()).hasSize(4);
        var one=input(5,"20",expanded);var p=one.policy();
        var legacy=new Input(new Policy("portfolio-replay-v1",p.initialCash(),p.maximumPositions(),p.positionPercent(),p.roundTripFeeBps(),p.slippageBps(),p.neutralBandPercent(),p.holdingDays(),p.calendarVersion()),one.cohorts(),one.dates(),one.registeredAt(),one.captureMode());
        assertThat(engine.evaluate(legacy,observations(legacy),end(),false).variants()).hasSize(4);
        var tree=JSON.valueToTree(legacy);var cohortTree=(com.fasterxml.jackson.databind.node.ObjectNode)tree.path("cohorts").get(0);
        cohortTree.remove(List.of("comparisons","unavailableComparisons","audit"));
        var restored=JSON.treeToValue(tree,Input.class);
        assertThat(restored.cohorts().get(0).comparisons()).isEmpty();
        assertThat(engine.evaluate(restored,observations(restored),end(),false).variants()).hasSize(4);
    }
    @Test void repeatedSelectionOfNewCandidateDoesNotDuplicateAHolding() {
        var first=cohort("a",DAY,stock("001"));var second=cohort("b",DAY.plusDays(1),stock("001"));
        var a=new Cohort(first.report(),List.of(),List.of(),first.anchors(),Map.of("SIGNAL_AGE4",first.baseline()),Map.of(),null);
        var b=new Cohort(second.report(),List.of(),List.of(),second.anchors(),Map.of("SIGNAL_AGE4",second.baseline()),Map.of(),null);
        var in=input(5,"20",a,b);var variant=engine.evaluate(in,observations(in),end(),false).variants().stream().filter(v->v.name().equals("SIGNAL_AGE4")).findFirst().orElseThrow();
        assertThat(variant.metrics().entered()).isEqualTo(1);
        assertThat(variant.trades().get(1).reason()).isEqualTo("DUPLICATE_HOLDING");
    }
    @Test void replaysOneCashAccountWithoutDuplicateHoldingsAndCostsReduceReturns(){
        var in=input(5,"20",cohort("a",DAY,stock("001")),cohort("b",DAY.plusDays(1),stock("001"),stock("002")));
        var result=engine.evaluate(in,observations(in),end(),false);var base=result.variants().get(0);
        assertThat(result.state()).isEqualTo("COMPLETE");assertThat(base.metrics().closed()).isEqualTo(2);
        assertThat(base.trades().get(1).reason()).isEqualTo("DUPLICATE_HOLDING");
        assertThat(base.finalEquity()).isEqualByComparingTo(bd("10000").add(base.metrics().realizedProfit()));
        assertThat(base.returnPercent()).isLessThan(base.metrics().meanReturnPercent());
        assertThat(result.variants().get(1).finalEquity()).isLessThan(base.finalEquity());
        assertThat(base.groups().get("EVIDENCE/EMA5_ACCELERATION/TRUE").closed()).isEqualTo(2);
        assertThat(base.curve()).allSatisfy(p->assertThat(p.cash()).isGreaterThanOrEqualTo(bd("0")));
    }
    @Test void sameDaySaleProceedsCannotFundNextOrderAndSharesAreIntegers(){
        var in=input(5,"100",cohort("a",DAY,stock("001")),cohort("b",DAY.plusDays(1),stock("002")));
        var data=observations(in);replace(data,"a","001",bar(dates().get(1),"125","130","121","125",1000));
        var base=engine.evaluate(in,data,end(),false).variants().get(0);
        assertThat(base.trades().get(0).quantity()).isEqualTo(99);
        assertThat(base.trades().get(0).reason()).isEqualTo("TARGET");
        assertThat(base.trades().get(1).reason()).isEqualTo("INSUFFICIENT_CASH");
    }
    @Test void stopWinsAmbiguousBarAndGapStopUsesOpeningPrice(){
        var in=input(5,"20",cohort("a",DAY,stock("001"),stock("002")));var data=observations(in);
        replace(data,"a","001",bar(dates().get(0),"100","125","85","100",1000));
        replace(data,"a","002",bar(dates().get(1),"80","85","75","80",1000));
        var base=engine.evaluate(in,data,end(),false).variants().get(0);
        assertThat(base.trades().get(0).ambiguous()).isTrue();assertThat(base.trades().get(0).exitPrice()).isEqualByComparingTo("89.955");
        assertThat(base.trades().get(1).reason()).isEqualTo("GAP_STOP");assertThat(base.trades().get(1).exitPrice()).isEqualByComparingTo("79.96");
        assertThat(base.metrics().consecutiveLosingExitDates()).isEqualTo(2);assertThat(base.maximumDrawdownPercent()).isPositive();
        assertThat(base.metrics().stopRatePercent()).isEqualByComparingTo("100");
    }
    @Test void missingHoldingBarsBlockEquityAndClearStaleMarksInsteadOfHidingLoss(){
        var in=input(5,"20",cohort("a",DAY,stock("001"),stock("002")));var data=observations(in);
        data.removeIf(o->o.key().equals("a:STOCK:001:"+dates().get(1)));
        var result=engine.evaluate(in,data,end(),true);var base=result.variants().get(0);
        assertThat(result.state()).isEqualTo("DATA_INCOMPLETE");assertThat(base.state()).isEqualTo("DATA_GAP");
        assertThat(base.finalEquity()).isNull();assertThat(base.returnPercent()).isNull();assertThat(base.metrics().winRatePercent()).isNull();
        assertThat(base.trades()).allSatisfy(t->{assertThat(t.markedProfit()).isNull();assertThat(t.state()).isEqualTo("DATA_GAP");});
        assertThat(base.curve().get(1).equity()).isNull();
    }
    @Test void positionLimitUncertainFillAndEntryGapAreSeparateNonTrades(){
        var in=input(1,"20",cohort("a",DAY,stock("001"),stock("002")));var data=observations(in);
        assertThat(engine.evaluate(in,data,end(),false).variants().get(0).trades().get(1).reason()).isEqualTo("POSITION_LIMIT");
        replace(data,"a","001",bar(dates().get(0),"110","115","109","112",1000));
        replace(data,"a","002",bar(dates().get(0),"100","100","100","100",0));
        var base=engine.evaluate(in,data,end(),false).variants().get(0);
        assertThat(base.trades()).extracting(Trade::reason).containsExactly("ENTRY_GAP","UNCERTAIN_FILL");
        assertThat(base.metrics().winRatePercent()).isNull();assertThat(base.returnPercent()).isZero();
        assertThat(base.metrics().stopRatePercent()).isNull();
    }
    @Test void cannotInventExitAtHoldingLimitOnSuspendedStock(){
        var in=input(5,"20",cohort("a",DAY,stock("001")));var data=observations(in);
        for(int i=19;i<21;i++)replace(data,"a","001",bar(dates().get(i),"102","102","102","102",0));
        var base=engine.evaluate(in,data,end(),true).variants().get(0);
        assertThat(base.state()).isEqualTo("OPEN_UNLIQUIDATED");assertThat(base.metrics().closed()).isZero();assertThat(base.trades().get(0).exitDate()).isNull();
    }
    @Test void marketComparisonUsesSamePricesButWatchListDoesNotTrade(){
        var c=cohort("a",DAY,stock("001"));var watch=stock("001","WATCH","BREAKOUT");
        var in=input(5,"20",new Cohort(c.report(),List.of(watch),c.withoutMarketFilter(),c.anchors()));
        var result=engine.evaluate(in,observations(in),end(),false);
        assertThat(result.variants().get(0).metrics().entered()).isZero();assertThat(result.variants().get(2).metrics().entered()).isEqualTo(1);
    }
    @Test void rejectsFutureAndDuplicateObservationsAndPendingDaysHaveNoInventedZeroWinrate(){
        var in=input(5,"20",cohort("a",DAY,stock("001")));var data=observations(in);
        assertThatThrownBy(()->engine.evaluate(in,data,time(DAY),false)).isInstanceOf(IllegalArgumentException.class);
        data.add(data.get(0));assertThatThrownBy(()->engine.evaluate(in,data,end(),false)).isInstanceOf(IllegalArgumentException.class);
        var pending=engine.evaluate(in,List.of(),time(DAY),false);assertThat(pending.state()).isEqualTo("WAITING_DATA");
        assertThat(pending.variants().get(0).finalEquity()).isNull();assertThat(pending.variants().get(0).metrics().winRatePercent()).isNull();
    }
}
