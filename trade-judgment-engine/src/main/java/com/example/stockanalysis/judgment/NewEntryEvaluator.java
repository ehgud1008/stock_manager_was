package com.example.stockanalysis.judgment;

import com.example.stockanalysis.engine.model.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.*;
import java.util.*;
import static com.example.stockanalysis.judgment.NewEntryAssessment.Status.*;

/** One source of new-entry eligibility for the judgment card and daily report. No IO or indicator recalculation. */
public final class NewEntryEvaluator {
    public static final String VERSION = "new-entry-v3-tier";
    // Preserve the report's existing age cap. The signal engine's earlier expiry always wins.
    public static final int MAX_SIGNAL_AGE = 3;
    public static final BigDecimal MIN_SCORE = new BigDecimal("80");
    public static final BigDecimal MIN_TREND = new BigDecimal("60");
    private static final Set<String> PATTERNS = Set.of("BREAKOUT", "PULLBACK_RECOVERY", "BREAKOUT_RETEST");
    private final int maximumSignalAge;
    private final boolean strictTime;
    private final boolean profitability;
    public NewEntryEvaluator() { this(MAX_SIGNAL_AGE,false,true); }
    private NewEntryEvaluator(int maximumSignalAge,boolean strictTime,boolean profitability) { this.maximumSignalAge=maximumSignalAge;this.strictTime=strictTime;this.profitability=profitability; }
    public static NewEntryEvaluator legacyTier(int age) { return new NewEntryEvaluator(age,true,false); }
    public static NewEntryEvaluator legacyV6() { return legacyV6(MAX_SIGNAL_AGE); }
    public static NewEntryEvaluator legacyV6(int age) { return new NewEntryEvaluator(age,false,false); }
    /** Research-only alternative; does not extend the signal engine's own expiry. */
    public static NewEntryEvaluator researchAge4() { return new NewEntryEvaluator(4,false,true); }
    public NewEntryAssessment evaluateLegacy(AnalysisResult analysis, BigDecimal price, LocalDate date, Instant asOf, String quality) {
        return evaluateInternal(analysis,price,date,asOf,quality,true);
    }

    public static boolean live(TradeSignal signal) {
        return signal.state() == TradeSignal.State.CONFIRMED || signal.state() == TradeSignal.State.ACTIVE;
    }

    public static boolean validContext(AnalysisResult analysis, BigDecimal price, LocalDate date, Instant asOf, String quality) {
        if (analysis == null || asOf == null || analysis.mode() != AnalysisMode.SWING) return false;
        var today = asOf.atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
        return price != null && price.signum() > 0 && date != null && !date.isAfter(today)
                && !date.isBefore(today.minusDays(7)) && !"INVALID".equals(quality)
                && analysis.signalReport() != null && sameSnapshotTime(asOf, analysis.signalReport().evaluatedAt());
    }

    // Persisted LocalDateTime values may lose fractions; accept only that specific precision loss.
    private static boolean sameSnapshotTime(Instant asOf, Instant evaluatedAt) {
        return evaluatedAt != null && (asOf.equals(evaluatedAt) || asOf.getNano()==0
                && asOf.equals(evaluatedAt.truncatedTo(java.time.temporal.ChronoUnit.SECONDS)));
    }

    public NewEntryAssessment evaluate(AnalysisResult analysis, BigDecimal price, LocalDate date, Instant asOf, String quality) {
        var raw=evaluateInternal(analysis,price,date,asOf,quality,false);
        var tier=SuitabilityTier.from(analysis);
        boolean waiting=raw.status()==WATCH && raw.selectedSignal()==null && raw.reasons().size()==1
                && analysis.signalReport().signals().stream().noneMatch(s->s.side()==TradeSignal.Side.BUY&&live(s)
                && s.ageBars()>=0&&s.ageBars()<=maximumSignalAge&&s.ageBars()<s.validForBars());
        var status=raw.status();var reasons=raw.reasons();
        if(status!=AVOID && "UNSCORED".equals(tier.tier())) {
            status=DEFER;reasons=List.of("점수 미산정: 자료·가격 시나리오를 확인하세요.");
        }
        boolean qualifiedTierB=profitability && qualifiedTierB(analysis,raw);
        if(status==ELIGIBLE && !"A".equals(tier.tier()) && !qualifiedTierB) {
            status=WATCH;reasons=List.of("점수 외 진입 조건 충족 · "+tier.tier()+"등급은 운영 신규 진입 대상이 아닙니다.");
        }
        if(status==ELIGIBLE && qualifiedTierB) reasons=List.of("B등급 조건부 진입: 75점 이상·눌림 회복/돌파 재지지·총손익비 2.0 이상·추세 70점 이상. 리포트의 비용·유동성 조건은 추가 검사합니다.");
        return new NewEntryAssessment((profitability?VERSION:"new-entry-v2-tier")+(maximumSignalAge==MAX_SIGNAL_AGE?"":"/research-age4-v1"),status,reasons,
                raw.selectedSignal(),raw.effectiveScenario(),raw.grossRewardRisk(),raw.signals(),tier,
                new NewEntryAssessment.Timing(raw.status(),raw.reasons(),waiting));
    }

    public static boolean qualifiedTierB(AnalysisResult analysis, NewEntryAssessment assessment) {
        var tier=SuitabilityTier.from(analysis);
        return "B".equals(tier.tier()) && tier.score()!=null && tier.score().compareTo(new BigDecimal("75"))>=0
                && assessment.selectedSignal()!=null
                && Set.of("PULLBACK_RECOVERY","BREAKOUT_RETEST").contains(assessment.selectedSignal().type())
                && assessment.grossRewardRisk()!=null && assessment.grossRewardRisk().compareTo(new BigDecimal("2.0"))>=0
                && analysis.factors().stream().anyMatch(f->"TREND".equals(f.factorCode()) && f.score()!=null
                        && f.score().compareTo(new BigDecimal("70"))>=0);
    }

    private NewEntryAssessment evaluateInternal(AnalysisResult analysis, BigDecimal price, LocalDate date, Instant asOf, String quality, boolean legacy) {
        if (!validContext(analysis, price, date, asOf, quality)
                || ((legacy || strictTime) && !asOf.equals(analysis.signalReport().evaluatedAt())))
            return result(DEFER, List.of("가격·신호 기준 시점 또는 데이터 유효성을 확인해야 합니다."), null, null, List.of());
        var report = analysis.signalReport();
        var ordered = report.signals().stream().filter(s -> s.side() == TradeSignal.Side.BUY)
                .sorted(Comparator.comparing(TradeSignal::occurredOn, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparingInt(s -> profitability && Set.of("PULLBACK_RECOVERY","BREAKOUT_RETEST").contains(s.type()) ? 0 : 1)
                        .thenComparing(TradeSignal::type).thenComparing(TradeSignal::id)).toList();
        var checks = new ArrayList<NewEntryAssessment.SignalCheck>();
        TradeSignal selected = null;
        TradeScenario effective = null;
        for (var signal : ordered) {
            var reasons = new ArrayList<String>();
            if (!PATTERNS.contains(signal.type())) reasons.add("지원하는 매수 패턴이 아닙니다.");
            if (!live(signal)) reasons.add("확정·유지 상태가 아닙니다.");
            if (signal.ageBars() < 0 || signal.ageBars() > maximumSignalAge
                    || signal.validForBars() <= 0 || signal.ageBars() >= signal.validForBars())
                reasons.add("선정 허용 기간 또는 신호 자체 유효기간을 벗어났습니다.");
            if (!date.equals(signal.evaluatedOn()) || signal.occurredOn() == null || signal.occurredOn().isAfter(date))
                reasons.add("신호 평가일·발생일이 분석 기준일과 맞지 않습니다.");
            if (!"IN_RANGE".equals(signal.entryStatus())) reasons.add("신호의 진입 구간 판정을 통과하지 못했습니다.");
            BigDecimal from = null, to = null;
            var scenario = analysis.scenario();
            if (scenario == null || signal.entryFrom() == null || signal.entryTo() == null) {
                reasons.add("신호 또는 시나리오의 진입 구간을 확인할 수 없습니다.");
            } else {
                from = scenario.entryFrom().max(signal.entryFrom());
                to = scenario.entryTo().min(signal.entryTo());
                if (from.compareTo(to) > 0) reasons.add("신호와 시나리오의 진입 구간이 겹치지 않습니다.");
                else if (price.compareTo(from) < 0 || price.compareTo(to) > 0)
                    reasons.add("현재 가격이 두 진입 구간의 교집합 밖에 있습니다.");
            }
            boolean eligible = reasons.isEmpty();
            checks.add(new NewEntryAssessment.SignalCheck(signal.id(), signal.type(), signal.ageBars(), eligible, from, to, reasons));
            if (eligible && selected == null) {
                selected = signal;
                // Preserve the target basis and stop; only narrow the permitted entry interval.
                effective = new TradeScenario(from, to, scenario.targets(), scenario.stopLoss(),scenario.targetBasis());
            }
        }
        boolean sell = report.signals().stream().anyMatch(s -> live(s) && s.side() == TradeSignal.Side.SELL
                && !"POSITION_STOP".equals(s.type()));
        if (sell) return result(AVOID, List.of("유효한 매도 신호가 있어 신규 진입을 회피합니다."), selected, effective, checks);
        var strategy = analysis.strategy();
        if (strategy != null && "OPEN_PRICE_EXCLUDED".equals(strategy.strategyCode()))
            return result(AVOID, List.of("종합분석 시가 기준 매매 제외 대상입니다."), selected, effective, checks);
        var trend = analysis.factors().stream().filter(f -> "TREND".equals(f.factorCode()))
                .map(FactorResult::score).filter(Objects::nonNull).findFirst().orElse(null);
        var missing = new ArrayList<String>();
        if (!"COMPLETE".equals(quality)) missing.add("수집 자료가 완전하지 않아 신규 진입을 보류합니다.");
        if (!"READY".equals(report.completeness())) missing.add("패턴 신호 표본이 부족합니다.");
        if ((legacy && analysis.totalScore() == null) || trend == null || strategy == null) missing.add("종합점수·추세·전략 자료가 부족합니다.");
        if (!missing.isEmpty()) return result(DEFER, missing, selected, effective, checks);
        if (strategy.action() == StrategyAction.AVOID && (legacy || !strategy.strategyCode().startsWith("SWING_V5:")))
            return result(AVOID, List.of("기존 분석의 신규 진입 회피 조건이 적용됩니다."), selected, effective, checks);
        var reasons = new ArrayList<String>();
        // Only the known score-derived SWING action may be bypassed by the independent timing assessment.
        if (!legacy && strategy.action()!=StrategyAction.BUY_CANDIDATE && !strategy.strategyCode().startsWith("SWING_V5:"))
            reasons.add("점수 이외의 기존 전략 보류 조건을 확인해야 합니다.");
        if (legacy && (analysis.totalScore().compareTo(MIN_SCORE) < 0 || strategy.action() != StrategyAction.BUY_CANDIDATE))
            reasons.add("스윙 적합도 80점 이상·매수 검토 판정을 충족하지 못했습니다.");
        if (trend.compareTo(MIN_TREND) < 0) reasons.add("추세 점수가 신규 진입 기준 60점 미만입니다.");
        if (selected == null) reasons.add("유효기간·기준일·두 진입 구간을 모두 충족하는 매수 신호가 없습니다.");
        if (!reasons.isEmpty()) return result(WATCH, reasons, selected, effective, checks);
        return result(ELIGIBLE, List.of("공통 신규 진입 조건을 충족했습니다. 리포트의 유동성·비용·시장 조건은 추가 검사합니다."), selected, effective, checks);
    }

    public static BigDecimal rewardRisk(TradeScenario scenario, BigDecimal roundTripCostBps) {
        if (scenario == null) return null;
        if (roundTripCostBps == null || roundTripCostBps.signum() < 0) throw new IllegalArgumentException("Invalid cost");
        var cost = scenario.entryTo().multiply(roundTripCostBps).movePointLeft(4);
        return scenario.targets().get(0).subtract(scenario.entryTo()).subtract(cost)
                .divide(scenario.entryTo().subtract(scenario.stopLoss()).add(cost), MathContext.DECIMAL128);
    }

    private NewEntryAssessment result(NewEntryAssessment.Status status, List<String> reasons, TradeSignal signal,
                                              TradeScenario scenario, List<NewEntryAssessment.SignalCheck> checks) {
        return new NewEntryAssessment("new-entry-v1"+(maximumSignalAge==MAX_SIGNAL_AGE?"":"/research-age4-v1"), status, reasons, signal, scenario, rewardRisk(scenario, BigDecimal.ZERO), checks);
    }
}
