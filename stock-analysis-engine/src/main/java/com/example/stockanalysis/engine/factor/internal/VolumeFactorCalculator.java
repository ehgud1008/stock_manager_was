package com.example.stockanalysis.engine.factor.internal;

import com.example.stockanalysis.engine.factor.FactorCalculator;
import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.engine.model.FactorResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class VolumeFactorCalculator implements FactorCalculator {

    public static final String CODE = "VOLUME";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public FactorResult calculate(AnalysisInput input) {
        List<DailyCandle> candles = FactorMath.orderedCandles(input);
        DailyCandle latest = candles.get(candles.size() - 1);
        int from = Math.max(candles.size() - 21, 0);
        List<DailyCandle> baseline = candles.subList(from, Math.max(from, candles.size() - 1));
        BigDecimal averageVolume = FactorMath.average(baseline.stream()
                .map(candle -> BigDecimal.valueOf(candle.volume()))
                .toList());
        BigDecimal latestVolume = BigDecimal.valueOf(latest.volume());
        BigDecimal ratio = averageVolume.signum() == 0
                ? BigDecimal.ONE
                : latestVolume.divide(averageVolume, 8, RoundingMode.HALF_UP);
        BigDecimal priceChange = candles.size() < 2
                ? BigDecimal.ZERO
                : FactorMath.percentChange(
                        latest.close(),
                        candles.get(candles.size() - 2).close()
                );
        BigDecimal confirmationBase = priceChange.signum() > 0
                ? BigDecimal.valueOf(55) : priceChange.signum() < 0
                ? BigDecimal.valueOf(45) : BigDecimal.valueOf(50);
        BigDecimal confirmationDirection = priceChange.signum() < 0
                ? BigDecimal.valueOf(-45) : BigDecimal.valueOf(45);
        BigDecimal confirmationScore = FactorMath.clampScore(
                confirmationBase.add(ratio.subtract(BigDecimal.ONE)
                        .multiply(confirmationDirection))
        );
        BigDecimal accumulationScore = accumulationScore(baseline);
        BigDecimal score = FactorMath.clampScore(
                confirmationScore.multiply(new BigDecimal("0.65"))
                        .add(accumulationScore.multiply(new BigDecimal("0.35")))
        );
        BigDecimal displayRatio = FactorMath.displayValue(ratio);
        return new FactorResult(
                CODE,
                displayRatio,
                score,
                FactorMath.weight(input, "0.10", "0.10"),
                "최근 거래량은 직전 20일 평균의 " + displayRatio.toPlainString()
                        + "배입니다. 당일 등락률 "
                        + FactorMath.displayValue(priceChange).toPlainString()
                        + "%와 상승일·하락일 거래량을 함께 반영했습니다."
        );
    }

    private BigDecimal accumulationScore(List<DailyCandle> candles) {
        BigDecimal upVolume = BigDecimal.ZERO;
        BigDecimal downVolume = BigDecimal.ZERO;
        int upDays = 0;
        int downDays = 0;
        for (int index = 1; index < candles.size(); index++) {
            int direction = candles.get(index).close().compareTo(candles.get(index - 1).close());
            if (direction > 0) {
                upVolume = upVolume.add(BigDecimal.valueOf(candles.get(index).volume()));
                upDays++;
            } else if (direction < 0) {
                downVolume = downVolume.add(BigDecimal.valueOf(candles.get(index).volume()));
                downDays++;
            }
        }
        if (upDays == 0 || downDays == 0) return BigDecimal.valueOf(50);
        BigDecimal upAverage = upVolume.divide(BigDecimal.valueOf(upDays), 8, RoundingMode.HALF_UP);
        BigDecimal downAverage = downVolume.divide(
                BigDecimal.valueOf(downDays), 8, RoundingMode.HALF_UP
        );
        if (downAverage.signum() == 0) return BigDecimal.valueOf(75);
        BigDecimal ratio = upAverage.divide(downAverage, 8, RoundingMode.HALF_UP);
        return FactorMath.clampScore(
                BigDecimal.valueOf(50).add(
                        ratio.subtract(BigDecimal.ONE).multiply(BigDecimal.valueOf(35))
                )
        );
    }
}
