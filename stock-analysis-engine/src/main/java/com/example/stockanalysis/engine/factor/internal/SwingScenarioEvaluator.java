package com.example.stockanalysis.engine.factor.internal;

import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.engine.model.TradeScenario;
import com.example.stockanalysis.engine.scenario.ProfitabilityEntryPolicy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Observed targets take precedence; new-high projections are explicit, fixed risk multiples. */
final class SwingScenarioEvaluator {
    record Evaluation(String type, TradeScenario scenario, String reason, boolean breakoutVolumeConfirmed, boolean syntheticTarget) {}
    private final boolean profitability;
    SwingScenarioEvaluator() { this(true); }
    SwingScenarioEvaluator(boolean profitability) { this.profitability = profitability; }

    Evaluation evaluate(List<DailyCandle> bars, BigDecimal current, BigDecimal trend) {
        return evaluate(bars,current,trend,true);
    }

    Evaluation evaluate(List<DailyCandle> bars, BigDecimal current, BigDecimal trend, boolean confirmed) {
        BigDecimal atr = RiskStabilityFactorCalculator.averageTrueRange(bars, 14);
        int end = bars.size() - 1;
        List<DailyCandle> recent = bars.subList(end - 20, end);
        BigDecimal resistance20 = recent.stream().map(DailyCandle::high).max(BigDecimal::compareTo).orElseThrow();
        BigDecimal mean20 = FactorMath.average(recent.stream().map(DailyCandle::close).toList());
        BigDecimal volume20 = FactorMath.average(recent.stream().map(c -> BigDecimal.valueOf(c.volume())).toList());
        boolean breakout = current.compareTo(resistance20) > 0;
        boolean pullback = !breakout && trend.compareTo(BigDecimal.valueOf(60)) >= 0
                && resistance20.subtract(current).compareTo(atr) >= 0
                && current.compareTo(mean20.subtract(atr)) >= 0;
        String type = breakout ? "BREAKOUT" : pullback ? "PULLBACK"
                : trend.compareTo(BigDecimal.valueOf(60)) >= 0 ? "CONTINUATION" : "RANGE";
        boolean volumeConfirmed = volume20.signum() > 0 && BigDecimal.valueOf(bars.get(end).volume())
                .compareTo(volume20.multiply(new BigDecimal(profitability ? "1.5" : "1.2"))) >= 0;
        BigDecimal support = breakout ? resistance20 : null;
        List<BigDecimal> targets = new ArrayList<>();
        // Longer observed history can supply an overhead target for a 20-day breakout.
        // A pivot must have two subsequent completed bars, and an invalidated level is not reused.
        for (int i = Math.max(2, end - 252); i <= end - 3; i++) {
            DailyCandle pivot = bars.get(i);
            boolean low = true, high = true;
            for (int j = i - 2; j <= i + 2; j++) {
                low &= pivot.low().compareTo(bars.get(j).low()) <= 0;
                high &= pivot.high().compareTo(bars.get(j).high()) >= 0;
            }
            boolean supportIntact = low;
            boolean resistanceIntact = high;
            for (int j = i + 1; j <= end; j++) {
                supportIntact &= bars.get(j).close().compareTo(pivot.low()) >= 0;
                resistanceIntact &= bars.get(j).close().compareTo(pivot.high()) <= 0;
            }
            // The latest intact swing low, not an arbitrarily distant low chosen for a desired R multiple.
            if (!breakout && supportIntact && i >= end - 60 && pivot.low().compareTo(current) < 0)
                support = pivot.low();
            if (resistanceIntact && pivot.high().compareTo(current) > 0) targets.add(pivot.high());
        }
        String reason = switch (type) {
            case "BREAKOUT" -> "20일 고점 돌파형: 돌파 가격을 무효화 기준으로 사용";
            case "PULLBACK" -> "상승 구조의 눌림목형: 최근 미이탈 피벗 저점을 무효화 기준으로 사용";
            case "CONTINUATION" -> "추세 지속형: 최근 미이탈 피벗 저점과 상단 저항 사용";
            default -> "방향 미확정 구간형: 지지·저항 범위만 평가";
        };
        if (support == null) return new Evaluation(type, null, reason + "; 확인된 지지 없음", volumeConfirmed, false);
        BigDecimal from = current.subtract(atr.multiply(new BigDecimal("0.25"))).max(support)
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal to = profitability ? ProfitabilityEntryPolicy.upperEntry(current, atr) : current.setScale(0, RoundingMode.HALF_UP);
        BigDecimal stop = support.subtract(atr.multiply(new BigDecimal(breakout ? "0.5" : "0.25")))
                .setScale(0, RoundingMode.HALF_UP);
        // Missing pivots alone do not prove a 252-bar high. Require a full prior window.
        boolean newHigh = end >= 252 && bars.subList(end - 252, end).stream()
                .allMatch(b -> current.compareTo(b.high()) > 0);
        boolean synthetic = profitability && confirmed && targets.isEmpty() && breakout && newHigh
                && trend.compareTo(BigDecimal.valueOf(60)) >= 0 && atr.signum() > 0;
        if (synthetic) {
            BigDecimal unitRisk = to.subtract(stop).max(atr);
            targets.add(to.add(unitRisk.multiply(new BigDecimal("2.0"))));
            targets.add(to.add(unitRisk.multiply(new BigDecimal("3.5"))));
            reason = "252봉 신고가 돌파형: 상단 관측 저항 부재로 ATR·구조적 위험 대비 2.0R·3.5R 가정 목표 사용 (관측 저항·가격 예측 아님)";
        }
        if (targets.isEmpty()) return new Evaluation(type, null, reason + "; 관측된 상단 목표 저항 없음", volumeConfirmed, false);
        if (profitability && targets.stream().map(t->t.setScale(0,RoundingMode.HALF_UP)).min(BigDecimal::compareTo).orElseThrow().compareTo(to)<=0)
            return new Evaluation(type,null,reason + "; 진입 상단 버퍼가 첫 목표 저항에 도달",volumeConfirmed,synthetic);
        List<BigDecimal> observedTargets = targets.stream().map(t -> t.setScale(0, RoundingMode.HALF_UP))
                .filter(t -> t.compareTo(to) > 0).distinct().sorted().limit(2).toList();
        if (from.compareTo(to) > 0 || stop.signum() <= 0 || stop.compareTo(from) >= 0 || observedTargets.isEmpty())
            return new Evaluation(type, null, reason + "; 반올림 후 가격 순서 불성립", volumeConfirmed, synthetic);
        var basis=!profitability?null:synthetic?TradeScenario.TargetBasis.STRUCTURAL_RISK_MULTIPLE:TradeScenario.TargetBasis.OBSERVED_RESISTANCE;
        return new Evaluation(type, new TradeScenario(from, to, observedTargets, stop,basis), reason, volumeConfirmed, synthetic);
    }
}
