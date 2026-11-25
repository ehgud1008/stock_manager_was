package com.example.stockanalysis.engine.factor.internal;

import com.example.stockanalysis.engine.api.StockAnalysisEngine;
import com.example.stockanalysis.engine.model.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Versioned heuristic, not a calibrated probability. Short-term analysis never uses this engine. */
public final class SwingSuitabilityEngine implements StockAnalysisEngine {
    public static final String VERSION = "analysis-v7-swing";
    private final boolean profitability;
    public SwingSuitabilityEngine() { this(true); }
    private SwingSuitabilityEngine(boolean profitability) { this.profitability = profitability; }
    public static SwingSuitabilityEngine legacyV6() { return new SwingSuitabilityEngine(false); }
    private String version() { return profitability ? VERSION : "analysis-v6-swing"; }
    private static final Map<String, BigDecimal> RISE_WEIGHTS = Map.of(
            "TREND", n(.18), "MOMENTUM", n(.12), "VOLUME", n(.09),
            "SUPPLY_DEMAND", n(.09), "RELATIVE_STRENGTH", n(.12));

    @Override
    public AnalysisResult analyze(AnalysisInput input) {
        if (input.mode() != AnalysisMode.SWING) throw new IllegalArgumentException("SWING only");
        List<DailyCandle> daily = FactorMath.orderedCandles(input);
        List<DailyCandle> weekly = FactorMath.orderedCandles(input.weeklyCandles());
        List<FactorResult> factors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        boolean sufficient = daily.size() >= 60 && weekly.size() >= 20;
        boolean tradableVolume = daily.stream().skip(Math.max(0, daily.size() - 20)).anyMatch(c -> c.volume() > 0);
        if (!sufficient || !tradableVolume) {
            String missingReason = !sufficient ? "핵심 표본 부족: 일봉 60개·주봉 20개 필요" : "최근 20일 거래량이 없어 거래 가능성 확인 필요";
            for (String code : List.of("TREND", "MOMENTUM", "VOLUME", "SUPPLY_DEMAND",
                    "RELATIVE_STRENGTH", "ENTRY_LOCATION", "ENTRY_REWARD_RISK", "ENTRY_STOP_DISTANCE", "RISK_STABILITY", "LIQUIDITY")) {
                factors.add(missing(code, missingReason));
            }
            return new AnalysisResult(version(), input.mode(), null, factors,
                    new StrategyDecision("SWING_V5:UNCLASSIFIED:DATA_MISSING:WAIT", "스윙 분석 보류 · 핵심 데이터 부족", StrategyAction.WAIT,
                            List.of(missingReason + ": 일봉 " + daily.size() + "개, 주봉 " + weekly.size() + "개")),
                    null, List.of(missingReason + ". 점수와 가격 시나리오를 산출하지 않았습니다."));
        }

        BigDecimal dailyTrend = structure(daily);
        BigDecimal weeklyTrend = structure(weekly);
        factors.add(factor("TREND", dailyTrend, dailyTrend.multiply(n(.7)).add(weeklyTrend.multiply(n(.3))), .18,
                "일봉 가격 구조 70%·주봉 가격 구조 30%: 고점·저점과 지지·저항 유지 평가"));
        BigDecimal r20 = change(daily, 20);
        BigDecimal r5 = change(daily, 5);
        BigDecimal momentum = clamp(n(50).add(r20.multiply(n(2)))).multiply(n(.7))
                .add(clamp(n(50).add(r5.subtract(r20.divide(n(4), 8, RoundingMode.HALF_UP)).multiply(n(4)))).multiply(n(.3)));
        factors.add(factor("MOMENTUM", r20, momentum, .12,
                "20일 수익률과 5일 상승 동력 변화 평가. 과열은 진입 위치에서 별도 반영"));
        factors.add(volume(daily));
        factors.add(supply(input.observations()));
        if (!input.observations().containsKey(RelativeStrengthFactorCalculator.MARKET_RETURN_20)
                && !input.observations().containsKey(RelativeStrengthFactorCalculator.INDUSTRY_RETURN_20)) {
            factors.add(missing("RELATIVE_STRENGTH", "시장·업종 20일 비교 수익률 미수집"));
        } else {
            FactorResult relative = new RelativeStrengthFactorCalculator().calculate(input);
            factors.add(factor(relative.factorCode(), relative.rawValue(), relative.score(), .12, relative.reason()));
        }
        // Missing optional rise factors are excluded only within the 60% rise budget.
        BigDecimal availableWeight = factors.stream().filter(f -> f.score() != null)
                .map(FactorResult::weight).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal assigned = BigDecimal.ZERO;
        int lastAvailable = -1;
        for (int i = 0; i < factors.size(); i++) if (factors.get(i).score() != null) lastAvailable = i;
        for (int i = 0; i < factors.size(); i++) {
            FactorResult f = factors.get(i);
            if (f.score() == null) {
                warnings.add(f.factorCode() + ": " + f.reason() + ". 상승 영역 내 가중치를 재배분한 잠정 점수입니다.");
                continue;
            }
            BigDecimal weight = i == lastAvailable ? n(.6).subtract(assigned)
                    : RISE_WEIGHTS.get(f.factorCode()).multiply(n(.6)).divide(availableWeight, 10, RoundingMode.HALF_UP);
            assigned = assigned.add(weight);
            factors.set(i, new FactorResult(f.factorCode(), f.rawValue(), f.score(), weight, f.reason()));
        }

        var lastDate=daily.get(daily.size()-1).businessDate();
        boolean confirmed=input.confirmedThrough()!=null ? !lastDate.isAfter(input.confirmedThrough())
                : !lastDate.plusDays(1).atStartOfDay(java.time.ZoneId.of("Asia/Seoul")).toInstant().isAfter(input.dataCutoffAt());
        SwingScenarioEvaluator.Evaluation setup = new SwingScenarioEvaluator(profitability).evaluate(daily, input.stock().currentPrice(), dailyTrend,confirmed);
        if (setup.syntheticTarget()) warnings.add("SYNTHETIC_TARGET: " + setup.reason());
        TradeScenario scenario = setup.scenario();
        BigDecimal atr = RiskStabilityFactorCalculator.averageTrueRange(daily, 14);
        BigDecimal current = input.stock().currentPrice();
        BigDecimal mean = FactorMath.average(daily.subList(daily.size() - 20, daily.size()).stream().map(DailyCandle::close).toList());
        BigDecimal extension = current.subtract(mean).max(BigDecimal.ZERO).divide(atr.max(n(.01)), 8, RoundingMode.HALF_UP);
        BigDecimal location = clamp(n(100).subtract(extension.multiply(n(20))));
        factors.add(factor("ENTRY_LOCATION", extension, location, .1,
                "20일 중심가격 대비 ATR 단위 추격 부담. 시나리오 산출 여부와 독립적으로 평가"));
        BigDecimal rr = scenario == null ? null : scenario.targets().get(0).subtract(scenario.entryTo())
                .divide(scenario.entryTo().subtract(scenario.stopLoss()), 8, RoundingMode.HALF_UP);
        BigDecimal stopAtr = scenario == null ? null : scenario.entryTo().subtract(scenario.stopLoss())
                .divide(atr.max(n(.01)), 8, RoundingMode.HALF_UP);
        if (scenario == null) {
            factors.add(missing("ENTRY_REWARD_RISK", "상단 목표 또는 지지가 없어 손익비 산출 불가; 0점으로 간주하지 않음"));
            factors.add(missing("ENTRY_STOP_DISTANCE", "시나리오가 없어 손절 거리 산출 불가"));
            warnings.add(setup.reason() + ". 부분 팩터만 표시하고 대표점수·매매 판정은 보류합니다.");
        } else {
            factors.add(factor("ENTRY_REWARD_RISK", rr, rewardRiskScore(rr), .14,
                    (setup.syntheticTarget() ? "구조적 가정 목표까지 손익비 " : "가장 가까운 관측 저항까지 손익비 ") + rr.setScale(2, RoundingMode.HALF_UP)
                            + ". 손절 거리 감점을 곱하지 않고 독립 평가"));
            factors.add(factor("ENTRY_STOP_DISTANCE", stopAtr, stopDistanceScore(stopAtr), .06,
                    "진입 상단에서 구조적 손절까지 " + stopAtr.setScale(2, RoundingMode.HALF_UP)
                            + " ATR. 종목 자체 변동폭 대비 거리 평가"));
        }

        BigDecimal atrPercent = atr.multiply(n(100)).divide(current, 8, RoundingMode.HALF_UP);
        BigDecimal drawdown = drawdown(daily);
        BigDecimal riskScore = clamp(n(100).subtract(atrPercent.multiply(n(10)))).multiply(n(.5))
                .add(clamp(n(100).subtract(drawdown.multiply(n(2)))).multiply(n(.5)));
        factors.add(factor("RISK_STABILITY", atrPercent, riskScore, .07,
                "ATR 비율 " + atrPercent.setScale(1, RoundingMode.HALF_UP) + "%, 60일 최대 낙폭 "
                        + drawdown.setScale(1, RoundingMode.HALF_UP) + "%. 단독 회피 조건으로 사용하지 않음"));
        BigDecimal turnover = FactorMath.average(daily.subList(daily.size() - 20, daily.size()).stream()
                .map(c -> c.close().multiply(n(c.volume()))).toList());
        factors.add(factor("LIQUIDITY", turnover, turnover.divide(n(100_000_000), 8, RoundingMode.HALF_UP), .03,
                "20일 평균 종가×거래량 기반 유동성 근사치. 100억원 이상 100점, 호가·체결 보장은 아님"));
        addReference(factors, "DAILY_TECHNICAL", dailyTrend, "일봉 가격 구조 참고값; 추세 항목에 이미 반영");
        addReference(factors, "WEEKLY_TECHNICAL", weeklyTrend, "주봉 가격 구조 참고값; 추세 항목에 이미 반영");
        addReference(factors, "MONTHLY_TECHNICAL", input.monthlyCandles().size() < 20 ? null
                : structure(FactorMath.orderedCandles(input.monthlyCandles())), "월봉 가격 구조 배경 정보; 종합점수 미반영");
        boolean valuePresent = List.of("roe", "per", "pbr").stream().allMatch(input.observations()::containsKey)
                && input.observations().get("per").signum() > 0 && input.observations().get("pbr").signum() > 0;
        if (valuePresent) {
            FactorResult value = new ValueQualityFactorCalculator().calculate(input);
            addReference(factors, "VALUE_QUALITY", value.score(), "ROE·PER·PBR 참고 점수; 스윙 적합도 미반영");
        } else factors.add(missing("VALUE_QUALITY", "가치·재무 자료 부족 또는 PER/PBR 평가 불가; 참고 항목"));

        BigDecimal total = scenario == null ? null : factors.stream().filter(f -> f.score() != null)
                .map(f -> f.score().multiply(f.weight())).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
        StrategyAction action = total == null ? StrategyAction.WAIT : actionFor(total);
        boolean recoveryConfirmed = current.compareTo(daily.get(daily.size() - 2).close()) > 0
                && current.compareTo(daily.get(daily.size() - 1).open()) >= 0;
        String detail = scenario == null ? "SCENARIO_UNAVAILABLE" : detailFor(setup, location, rr, stopAtr, dailyTrend, recoveryConfirmed);
        List<String> reasons = new ArrayList<>();
        reasons.add(total == null ? "대표점수 산출 보류: 목표 시나리오 미확정. 진입 조건이 나쁘다는 0점과 구분합니다."
                : "스윙 적합도 " + total + "점: 80 이상 매수 검토, 65 이상 관찰, 45 이상 진입 대기, 45 미만 회피");
        reasons.add(setup.reason());
        reasons.add(detailLabel(detail));
        factors.stream().filter(f -> f.score() != null && f.weight().signum() > 0)
                .sorted(Comparator.comparing((FactorResult f) -> n(100).subtract(f.score()).multiply(f.weight())).reversed())
                .limit(2).forEach(f -> reasons.add(f.factorCode() + " " + f.score() + "점: " + f.reason()));
        if (!warnings.isEmpty()) reasons.add("누락 항목 또는 시나리오 제한은 경고와 데이터 상태를 함께 확인하세요.");
        String name = switch (action) {
            case BUY_CANDIDATE -> "매수 검토";
            case WATCH -> "관찰";
            case WAIT -> "진입 대기";
            case AVOID -> "회피";
        };
        return new AnalysisResult(version(), input.mode(), total, factors,
                new StrategyDecision("SWING_V5:" + setup.type() + ":" + detail + ":" + action,
                        "스윙 " + (total == null ? "분석 보류" : name) + " · " + detailLabel(detail), action, reasons), scenario, warnings);
    }

    static BigDecimal rewardRiskScore(BigDecimal ratio) { return clamp(ratio.multiply(n(40))); }
    static BigDecimal stopDistanceScore(BigDecimal atrDistance) {
        return clamp(n(100).subtract(atrDistance.subtract(n(1.5)).max(BigDecimal.ZERO).multiply(n(20))));
    }
    private String detailFor(SwingScenarioEvaluator.Evaluation setup, BigDecimal location, BigDecimal rr,
                             BigDecimal stopAtr, BigDecimal trend, boolean recoveryConfirmed) {
        if (rr.compareTo(n(1)) < 0) return "LOW_REWARD_RISK";
        if (stopAtr.compareTo(n(4)) > 0) return "WIDE_STOP";
        if (location.compareTo(n(50)) < 0) return "EXTENDED";
        if ("BREAKOUT".equals(setup.type()) && !setup.breakoutVolumeConfirmed()) return "BREAKOUT_CONFIRM";
        if (trend.compareTo(n(60)) < 0) return "TREND_CONFIRM";
        if ("PULLBACK".equals(setup.type()) && !recoveryConfirmed) return "PULLBACK_CONFIRM";
        return "SETUP_REVIEW";
    }
    public static String detailLabel(String code) {
        return switch (code) {
            case "DATA_MISSING" -> "핵심 데이터 부족";
            case "SCENARIO_UNAVAILABLE" -> "목표 시나리오 산출 불가";
            case "LOW_REWARD_RISK" -> "손익비 부족";
            case "WIDE_STOP" -> "손절 거리 부담";
            case "EXTENDED" -> "추격 부담";
            case "BREAKOUT_CONFIRM" -> "돌파 거래량 확인 필요";
            case "PULLBACK_CONFIRM" -> "눌림 회복 확인 필요";
            case "TREND_CONFIRM" -> "추세 확인 필요";
            default -> "시나리오 조건 검토";
        };
    }

    public static StrategyAction actionFor(BigDecimal score) {
        if (score.compareTo(n(80)) >= 0) return StrategyAction.BUY_CANDIDATE;
        if (score.compareTo(n(65)) >= 0) return StrategyAction.WATCH;
        if (score.compareTo(n(45)) >= 0) return StrategyAction.WAIT;
        return StrategyAction.AVOID;
    }

    private FactorResult volume(List<DailyCandle> c) {
        List<DailyCandle> recent = c.subList(c.size() - 20, c.size());
        BigDecimal up = BigDecimal.ZERO, down = BigDecimal.ZERO;
        int upDays = 0, downDays = 0;
        for (int i = c.size() - 20; i < c.size(); i++) {
            int direction = c.get(i).close().compareTo(c.get(i - 1).close());
            if (direction > 0) { up = up.add(n(c.get(i).volume())); upDays++; }
            if (direction < 0) { down = down.add(n(c.get(i).volume())); downDays++; }
        }
        if (recent.stream().allMatch(x -> x.volume() == 0)) return missing("VOLUME", "최근 20일 거래량 없음");
        BigDecimal upAverage = upDays == 0 ? BigDecimal.ZERO : up.divide(n(upDays), 8, RoundingMode.HALF_UP);
        BigDecimal downAverage = downDays == 0 ? BigDecimal.ZERO : down.divide(n(downDays), 8, RoundingMode.HALF_UP);
        BigDecimal balance = upAverage.add(downAverage).signum() == 0 ? n(50)
                : upAverage.multiply(n(100)).divide(upAverage.add(downAverage), 8, RoundingMode.HALF_UP);
        return factor("VOLUME", balance, balance, .09, "20일 상승일·하락일 평균 거래량 균형; 하루 폭증 중복 가산 제외");
    }

    private FactorResult supply(Map<String, BigDecimal> o) {
        BigDecimal volume = o.get("swingFlowVolume20"), net = o.get("swingNetBuy20");
        if (volume == null || volume.signum() <= 0 || net == null || !o.containsKey("swingPositiveFlowDays20"))
            return missing("SUPPLY_DEMAND", "같은 20거래일의 수량 기준 수급·거래량 자료 부족");
        BigDecimal rate = net.multiply(n(100)).divide(volume, 8, RoundingMode.HALF_UP);
        BigDecimal score = clamp(n(50).add(rate.multiply(n(2)))).multiply(n(.7))
                .add(o.get("swingPositiveFlowDays20").multiply(n(5)).multiply(n(.3)));
        return factor("SUPPLY_DEMAND", rate, score, .09, "동일 20거래일 순매수/총거래량 비율과 순매수 일수 평가");
    }

    private BigDecimal structure(List<DailyCandle> c) {
        int end = c.size() - 1;
        List<DailyCandle> recent = c.subList(end - 8, end), previous = c.subList(end - 16, end - 8);
        BigDecimal high = recent.stream().map(DailyCandle::high).max(BigDecimal::compareTo).orElseThrow();
        BigDecimal low = recent.stream().map(DailyCandle::low).min(BigDecimal::compareTo).orElseThrow();
        int highDirection = high.compareTo(previous.stream().map(DailyCandle::high).max(BigDecimal::compareTo).orElseThrow());
        int lowDirection = low.compareTo(previous.stream().map(DailyCandle::low).min(BigDecimal::compareTo).orElseThrow());
        BigDecimal score = n(50 + Integer.signum(highDirection) * 20 + Integer.signum(lowDirection) * 20);
        if (c.get(end).close().compareTo(high) > 0) score = score.add(n(10));
        if (c.get(end).close().compareTo(low) < 0) score = score.subtract(n(20));
        return clamp(score);
    }

    private BigDecimal drawdown(List<DailyCandle> c) {
        BigDecimal peak = BigDecimal.ZERO, worst = BigDecimal.ZERO;
        for (DailyCandle bar : c.subList(c.size() - 60, c.size())) {
            peak = peak.max(bar.close());
            worst = worst.max(peak.subtract(bar.close()).multiply(n(100)).divide(peak, 8, RoundingMode.HALF_UP));
        }
        return worst;
    }
    private BigDecimal change(List<DailyCandle> c, int period) {
        return FactorMath.percentChange(c.get(c.size() - 1).close(), c.get(c.size() - 1 - period).close());
    }
    private static BigDecimal n(double x) { return BigDecimal.valueOf(x); }
    private static BigDecimal n(long x) { return BigDecimal.valueOf(x); }
    private static BigDecimal clamp(BigDecimal x) { return FactorMath.clampScore(x); }
    private static FactorResult factor(String code, BigDecimal raw, BigDecimal score, double weight, String reason) {
        return new FactorResult(code, raw, clamp(score), n(weight), reason);
    }
    private static FactorResult missing(String code, String reason) {
        return new FactorResult(code, null, null, BigDecimal.ZERO, reason);
    }
    private static void addReference(List<FactorResult> factors, String code, BigDecimal score, String reason) {
        factors.add(score == null ? missing(code, reason + "; 표본 부족") : factor(code, score, score, 0, reason));
    }
}
