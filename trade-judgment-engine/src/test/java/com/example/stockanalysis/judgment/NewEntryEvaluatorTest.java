package com.example.stockanalysis.judgment;

import com.example.stockanalysis.engine.model.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static com.example.stockanalysis.judgment.NewEntryAssessment.Status.*;

class NewEntryEvaluatorTest {
    @Test void narrowingEntryPreservesTargetBasisAndHistoricalJsonRemainsReadable() {
        var a=analysis(signal("valid",0,98,103));
        var scenario=a.scenario();
        var marked=new AnalysisResult(a.engineVersion(),a.mode(),a.totalScore(),a.factors(),a.strategy(),
                new TradeScenario(scenario.entryFrom(),scenario.entryTo(),scenario.targets(),scenario.stopLoss(),TradeScenario.TargetBasis.STRUCTURAL_RISK_MULTIPLE),a.warnings(),a.signalReport());
        assertThat(evaluate(marked).effectiveScenario().targetBasis()).isEqualTo(TradeScenario.TargetBasis.STRUCTURAL_RISK_MULTIPLE);
        assertThat(evaluate(a).effectiveScenario().targetBasis()).isNull();
    }
    @Test void sameDayRecoveryTakesPriorityOverSimpleBreakoutOnlyInNewPolicy() {
        var breakout=signal("breakout",0,98,103);
        var recovery=new TradeSignal("recovery","PULLBACK_RECOVERY",breakout.side(),breakout.state(),DAY,DAY,0,5,n(100),n(90),n(98),n(103),"IN_RANGE",null,List.of());
        var a=analysis(breakout,recovery);
        assertThat(evaluate(a).selectedSignal().id()).isEqualTo("recovery");
        assertThat(NewEntryEvaluator.legacyV6().evaluate(a,n(100),DAY,NOW,"COMPLETE").selectedSignal().id()).isEqualTo("breakout");
    }
    @Test void tierBPromotionRequiresEveryBoundaryAndLegacyV6KeepsWatch() {
        for(String pattern:List.of("PULLBACK_RECOVERY","BREAKOUT_RETEST","BREAKOUT")) {
            for(String score:List.of("74.9999","75","79.9999")) {
                for(String trend:List.of("69.9999","70")) {
                    for(int target:List.of(128,129)) {
                        var original=signal("valid",0,98,103);
                        var signal=new TradeSignal(original.id(),pattern,original.side(),original.state(),original.occurredOn(),original.evaluatedOn(),0,5,n(100),n(90),n(98),n(103),"IN_RANGE",null,List.of());
                        var a=analysis(signal);
                        var candidate=new AnalysisResult(a.engineVersion(),a.mode(),new BigDecimal(score),
                                List.of(new FactorResult("TREND",new BigDecimal(trend),new BigDecimal(trend),BigDecimal.ONE,"test")),
                                new StrategyDecision("SWING_V5:TEST:WATCH","test",StrategyAction.WATCH,List.of()),
                                new TradeScenario(n(95),n(110),List.of(n(target)),n(90)),a.warnings(),a.signalReport());
                        boolean pass=!pattern.equals("BREAKOUT")&&!score.equals("74.9999")&&trend.equals("70")&&target==129;
                        assertThat(evaluate(candidate).status()).as("%s %s %s %d",pattern,score,trend,target).isEqualTo(pass?ELIGIBLE:WATCH);
                        assertThat(NewEntryEvaluator.legacyV6().evaluate(candidate,n(100),DAY,NOW,"COMPLETE").status()).isEqualTo(WATCH);
                        if(pass) assertThat(new NewEntryEvaluator().evaluate(candidate,n(100),DAY,NOW,"PARTIAL").status()).isEqualTo(DEFER);
                    }
                }
            }
        }
    }
    @Test void tiersAreIndependentOfEntryTimingAndLegacyRulesRemainReproducible() {
        var a=analysis(signal("valid",0,98,103));
        for(var pair:Map.of("80","A","79.9999","B","70","B","69.9999","C","65","C","64.9999","D").entrySet()) {
            var score=new BigDecimal(pair.getKey());
            var strategy=new StrategyDecision("SWING_V5:TEST:WATCH","test",StrategyAction.WATCH,List.of());
            var candidate=new AnalysisResult(a.engineVersion(),a.mode(),score,a.factors(),strategy,a.scenario(),a.warnings(),a.signalReport());
            var assessed=evaluate(candidate);
            assertThat(assessed.suitability().tier()).isEqualTo(pair.getValue());
            assertThat(assessed.timing().status()).isEqualTo(ELIGIBLE);
            assertThat(assessed.status()).isEqualTo(pair.getValue().equals("A")?ELIGIBLE:WATCH);
            var old=new NewEntryEvaluator().evaluateLegacy(candidate,n(100),DAY,NOW,"COMPLETE");
            assertThat(old.policyVersion()).isEqualTo("new-entry-v1");assertThat(old.status()).isEqualTo(WATCH);assertThat(old.suitability()).isNull();
        }
        assertThat(SuitabilityTier.grade(null)).isEqualTo("UNSCORED");
        assertThat(SuitabilityTier.grade(n(-1))).isEqualTo("UNSCORED");
        assertThat(SuitabilityTier.grade(n(101))).isEqualTo("UNSCORED");
    }
    @Test void waitingForSignalIsDifferentFromPriceOutsideEntryRange() {
        assertThat(evaluate(analysis()).timing().signalWaiting()).isTrue();
        assertThat(evaluate(analysis(signal("outside",0,105,110))).timing().signalWaiting()).isFalse();
        var a=analysis(signal("valid",0,98,103));
        var missing=new AnalysisResult(a.engineVersion(),a.mode(),null,a.factors(),a.strategy(),a.scenario(),a.warnings(),a.signalReport());
        assertThat(evaluate(missing).suitability().tier()).isEqualTo("UNSCORED");assertThat(evaluate(missing).status()).isEqualTo(DEFER);
        var risk=new AnalysisResult(a.engineVersion(),a.mode(),n(75),a.factors(),new StrategyDecision("RISK_EXCLUSION","risk",StrategyAction.AVOID,List.of()),a.scenario(),a.warnings(),a.signalReport());
        assertThat(evaluate(risk).timing().status()).isEqualTo(AVOID);
        assertThat(new NewEntryEvaluator().evaluate(a,n(100),DAY,NOW,"PARTIAL").timing().status()).isEqualTo(DEFER);
    }
    private static final LocalDate DAY = LocalDate.of(2026,10,22);
    private static final Instant NOW = Instant.parse("2026-10-22T11:00:00Z");
    private static BigDecimal n(int value) { return BigDecimal.valueOf(value); }
    private static TradeSignal signal(String id, int age, int from, int to) {
        return new TradeSignal(id,"BREAKOUT",TradeSignal.Side.BUY,TradeSignal.State.ACTIVE,
                DAY.minusDays(age),DAY,age,5,n(100),n(90),n(from),n(to),"IN_RANGE",null,List.of());
    }
    private static AnalysisResult analysis(TradeSignal... signals) {
        return new AnalysisResult("test",AnalysisMode.SWING,n(85),
                List.of(new FactorResult("TREND",n(70),n(70),BigDecimal.ONE,"test")),
                new StrategyDecision("SWING","test",StrategyAction.BUY_CANDIDATE,List.of()),
                new TradeScenario(n(95),n(110),List.of(n(140)),n(90)),List.of(),
                new SignalReport("test",NOW,"UNKNOWN","READY",List.of(signals),List.of()));
    }
    private static NewEntryAssessment evaluate(AnalysisResult a) {
        return new NewEntryEvaluator().evaluate(a,n(100),DAY,NOW,"COMPLETE");
    }
    @Test void testsEverySignalBeforeSelectingAndKeepsEffectivePricesTogether() {
        var a=analysis(signal("latest-outside",0,105,115),signal("older-valid",2,98,103));
        var result=evaluate(a);
        assertThat(result.status()).isEqualTo(ELIGIBLE);
        assertThat(result.selectedSignal().id()).isEqualTo("older-valid");
        assertThat(result.effectiveScenario()).isEqualTo(new TradeScenario(n(98),n(103),List.of(n(140)),n(90)));
        assertThat(result.signals()).extracting(NewEntryAssessment.SignalCheck::eligible).containsExactly(false,true);
        assertThat(result.grossRewardRisk()).isEqualByComparingTo(NewEntryEvaluator.rewardRisk(result.effectiveScenario(),BigDecimal.ZERO));
        assertThat(result).isEqualTo(evaluate(a));
        var judgment=new TradeJudgmentEngine().evaluate(new TradeJudgmentEngine.Input(a,n(100),DAY,NOW,"COMPLETE"));
        assertThat(judgment.newEntry()).isEqualTo(result);
        assertThat(judgment.notHeld().action()).isEqualTo(TradeJudgment.Action.STRONG_BUY_CANDIDATE);
    }
    @Test void stableTieBreakIsIndependentOfInputOrder() {
        var first=signal("a",1,98,103);var second=signal("b",1,99,104);
        assertThat(evaluate(analysis(second,first))).isEqualTo(evaluate(analysis(first,second)));
        assertThat(evaluate(analysis(second,first)).selectedSignal().id()).isEqualTo("a");
    }
    @Test void disjointIntervalsAndBothBoundariesAreExplicit() {
        var missing=evaluate(analysis(signal("apart",0,111,115)));
        assertThat(missing.status()).isEqualTo(WATCH);
        assertThat(missing.effectiveScenario()).isNull();
        assertThat(missing.signals().get(0).reasons()).anyMatch(s->s.contains("겹치지"));
        for(var range:List.of(new int[]{100,103},new int[]{98,100}))
            assertThat(evaluate(analysis(signal("boundary",0,range[0],range[1]))).status()).isEqualTo(ELIGIBLE);
    }
    @Test void ageCapAndSignalOwnExpiryAreBothEnforced() {
        for(int age:List.of(-1,0,3,4,5)) {
            var result=evaluate(analysis(signal("age",age,98,103)));
            assertThat(result.status()).as("age=%d",age).isEqualTo(age>=0&&age<=3?ELIGIBLE:WATCH);
        }
        var expired=new TradeSignal("expiry","BREAKOUT",TradeSignal.Side.BUY,TradeSignal.State.ACTIVE,
                DAY.minusDays(3),DAY,3,3,n(100),n(90),n(98),n(103),"IN_RANGE",null,List.of());
        assertThat(evaluate(analysis(expired)).status()).isEqualTo(WATCH);
    }
    @Test void staleSignalAndIncompleteDataCannotBecomeEntry() {
        var old=new TradeSignal("old","BREAKOUT",TradeSignal.Side.BUY,TradeSignal.State.ACTIVE,
                DAY.minusDays(1),DAY.minusDays(1),1,5,n(100),n(90),n(98),n(103),"IN_RANGE",null,List.of());
        assertThat(evaluate(analysis(old)).status()).isEqualTo(WATCH);
        for(String quality:List.of("PARTIAL","INVALID","PENDING"))
            assertThat(new NewEntryEvaluator().evaluate(analysis(signal("valid",0,98,103)),n(100),DAY,NOW,quality).status()).isEqualTo(DEFER);
    }
    @Test void lowScoreDoesNotBecomeCandidateEvenIfStrategyIsInconsistent() {
        var a=analysis(signal("valid",0,98,103));
        var low=new AnalysisResult(a.engineVersion(),a.mode(),n(79),a.factors(),a.strategy(),a.scenario(),a.warnings(),a.signalReport());
        assertThat(evaluate(low).status()).isEqualTo(WATCH);
    }
    @Test void researchAge4DoesNotChangeLivePolicyOrReviveExpiredSignals() {
        var a=analysis(signal("four",4,98,103));
        assertThat(evaluate(a).status()).isEqualTo(WATCH);
        var experiment=NewEntryEvaluator.researchAge4().evaluate(a,n(100),DAY,NOW,"COMPLETE");
        assertThat(experiment.status()).isEqualTo(ELIGIBLE);
        assertThat(experiment.policyVersion()).contains("research-age4");
        assertThat(NewEntryEvaluator.researchAge4().evaluate(analysis(signal("five",5,98,103)),n(100),DAY,NOW,"COMPLETE").status()).isEqualTo(WATCH);
        assertThat(evaluate(a).status()).isEqualTo(WATCH);
    }
}
