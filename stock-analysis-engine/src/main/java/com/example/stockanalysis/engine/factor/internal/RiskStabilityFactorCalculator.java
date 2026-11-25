package com.example.stockanalysis.engine.factor.internal;

import com.example.stockanalysis.engine.factor.FactorCalculator;
import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.engine.model.FactorResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** 위험이 낮을수록 높은 점수를 반환한다. */
public final class RiskStabilityFactorCalculator implements FactorCalculator {

    public static final String CODE = "RISK_STABILITY";
    public static final String CREDIT_BALANCE_RATIO = "creditBalanceRatio";
    public static final String SHORT_SELLING_RATIO = "shortSellingRatio";
    public static final String LENDING_BALANCE_CHANGE_RATE = "lendingBalanceChangeRate";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public FactorResult calculate(AnalysisInput input) {
        List<DailyCandle> candles = FactorMath.orderedCandles(input);
        BigDecimal atr = averageTrueRange(candles, 14);
        BigDecimal latest = candles.get(candles.size() - 1).close();
        BigDecimal atrPercent = latest.signum() == 0
                ? BigDecimal.ZERO
                : atr.multiply(BigDecimal.valueOf(100))
                        .divide(latest, 8, RoundingMode.HALF_UP);
        BigDecimal drawdown = maximumDrawdown(candles, 60);

        BigDecimal atrScore = decliningScore(atrPercent, "2", "4", "8");
        BigDecimal drawdownScore = decliningScore(drawdown, "5", "15", "30");
        List<BigDecimal> positioningScores = new ArrayList<>();
        addRiskScore(positioningScores, input.observations().get(CREDIT_BALANCE_RATIO),
                "3", "7", "12");
        addRiskScore(positioningScores, input.observations().get(SHORT_SELLING_RATIO),
                "2", "7", "15");
        addRiskScore(positioningScores, input.observations().get(LENDING_BALANCE_CHANGE_RATE),
                "0", "10", "30");
        BigDecimal positioningScore = positioningScores.isEmpty()
                ? BigDecimal.valueOf(50) : FactorMath.average(positioningScores);
        BigDecimal score = FactorMath.clampScore(
                atrScore.multiply(new BigDecimal("0.40"))
                        .add(drawdownScore.multiply(new BigDecimal("0.40")))
                        .add(positioningScore.multiply(new BigDecimal("0.20")))
        );

        BigDecimal displayAtr = FactorMath.displayValue(atrPercent);
        return new FactorResult(
                CODE,
                displayAtr,
                score,
                FactorMath.weight(input, "0.10", "0.10"),
                "ATR 비율은 " + displayAtr.toPlainString() + "%이며 최근 최대 낙폭은 "
                        + FactorMath.displayValue(drawdown).toPlainString()
                        + "%입니다. 신용·공매도·대차 자료가 없으면 해당 부분은 중립 처리합니다."
        );
    }

    public static BigDecimal averageTrueRange(List<DailyCandle> candles, int period) {
        if (candles.isEmpty()) return BigDecimal.ZERO;
        int from = Math.max(0, candles.size() - period);
        BigDecimal total = BigDecimal.ZERO;
        for (int index = from; index < candles.size(); index++) {
            DailyCandle candle = candles.get(index);
            BigDecimal range = candle.high().subtract(candle.low());
            if (index > 0) {
                BigDecimal previousClose = candles.get(index - 1).close();
                range = range.max(candle.high().subtract(previousClose).abs())
                        .max(candle.low().subtract(previousClose).abs());
            }
            total = total.add(range);
        }
        return total.divide(
                BigDecimal.valueOf(candles.size() - from),
                8,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal maximumDrawdown(List<DailyCandle> candles, int period) {
        int from = Math.max(0, candles.size() - period);
        BigDecimal peak = null;
        BigDecimal maximum = BigDecimal.ZERO;
        for (int index = from; index < candles.size(); index++) {
            BigDecimal close = candles.get(index).close();
            peak = peak == null ? close : peak.max(close);
            BigDecimal drawdown = peak.subtract(close)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(peak, 8, RoundingMode.HALF_UP);
            maximum = maximum.max(drawdown);
        }
        return maximum;
    }

    private void addRiskScore(
            List<BigDecimal> scores,
            BigDecimal value,
            String safe,
            String caution,
            String dangerous
    ) {
        if (value != null) scores.add(decliningScore(value.abs(), safe, caution, dangerous));
    }

    private BigDecimal decliningScore(
            BigDecimal value,
            String safe,
            String caution,
            String dangerous
    ) {
        BigDecimal safeValue = new BigDecimal(safe);
        BigDecimal cautionValue = new BigDecimal(caution);
        BigDecimal dangerousValue = new BigDecimal(dangerous);
        if (value.compareTo(safeValue) <= 0) return BigDecimal.valueOf(85);
        if (value.compareTo(cautionValue) <= 0) {
            return interpolate(value, safeValue, cautionValue, 85, 55);
        }
        if (value.compareTo(dangerousValue) <= 0) {
            return interpolate(value, cautionValue, dangerousValue, 55, 15);
        }
        return BigDecimal.valueOf(10);
    }

    private BigDecimal interpolate(
            BigDecimal value,
            BigDecimal from,
            BigDecimal to,
            int fromScore,
            int toScore
    ) {
        BigDecimal ratio = value.subtract(from)
                .divide(to.subtract(from), 8, RoundingMode.HALF_UP);
        return FactorMath.clampScore(BigDecimal.valueOf(fromScore).add(
                BigDecimal.valueOf(toScore - fromScore).multiply(ratio)
        ));
    }
}
