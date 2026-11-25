package com.example.stockanalysis.engine.factor.internal;

import com.example.stockanalysis.engine.factor.FactorCalculator;
import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.engine.model.FactorResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class TrendFactorCalculator implements FactorCalculator {

    public static final String CODE = "TREND";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public FactorResult calculate(AnalysisInput input) {
        List<DailyCandle> candles = FactorMath.orderedCandles(input);
        if (candles.size() < 7) {
            return new FactorResult(
                    CODE,
                    BigDecimal.ZERO.setScale(1),
                    BigDecimal.valueOf(50).setScale(4),
                    FactorMath.weight(input, "0.20", "0.20"),
                    "가격 구조를 판정할 표본이 부족하여 중립 점수를 적용했습니다."
            );
        }

        int window = Math.min(20, Math.max(3, (candles.size() - 1) / 2));
        int recentFrom = candles.size() - 1 - window;
        int previousFrom = Math.max(0, recentFrom - window);
        List<DailyCandle> previous = candles.subList(previousFrom, recentFrom);
        List<DailyCandle> recent = candles.subList(recentFrom, candles.size() - 1);
        DailyCandle latest = candles.get(candles.size() - 1);

        BigDecimal previousHigh = highest(previous);
        BigDecimal previousLow = lowest(previous);
        BigDecimal recentHigh = highest(recent);
        BigDecimal recentLow = lowest(recent);
        boolean higherHigh = recentHigh.compareTo(previousHigh) > 0;
        boolean higherLow = recentLow.compareTo(previousLow) > 0;
        boolean breakout = latest.close().compareTo(recentHigh) > 0;
        boolean breakdown = latest.close().compareTo(recentLow) < 0;
        boolean failedBreakout = latest.high().compareTo(recentHigh) > 0
                && latest.close().compareTo(recentHigh) <= 0;

        BigDecimal structureScore = structureScore(higherHigh, higherLow)
                .add(breakout ? BigDecimal.valueOf(15) : BigDecimal.ZERO)
                .subtract(breakdown ? BigDecimal.valueOf(15) : BigDecimal.ZERO)
                .subtract(failedBreakout ? BigDecimal.TEN : BigDecimal.ZERO);

        List<BigDecimal> recentCloses = candles.stream()
                .skip(Math.max(candles.size() - 20L, 0L))
                .map(DailyCandle::close)
                .toList();
        BigDecimal movingAverage = FactorMath.average(recentCloses);
        BigDecimal distance = FactorMath.percentChange(latest.close(), movingAverage);
        BigDecimal pastAverage = averageBeforeLatest(candles, 20, 5);
        BigDecimal slope = FactorMath.percentChange(movingAverage, pastAverage);
        BigDecimal slopeScore = FactorMath.clampScore(
                BigDecimal.valueOf(50).add(slope.multiply(BigDecimal.valueOf(12)))
        );
        BigDecimal rangePosition = rangePosition(latest.close(), recentLow, recentHigh);
        BigDecimal locationScore = breakout
                ? BigDecimal.valueOf(90)
                : breakdown ? BigDecimal.valueOf(10) : rangePosition;

        BigDecimal score = FactorMath.clampScore(
                structureScore.multiply(new BigDecimal("0.50"))
                        .add(slopeScore.multiply(new BigDecimal("0.25")))
                        .add(locationScore.multiply(new BigDecimal("0.25")))
        );
        BigDecimal displayPosition = FactorMath.displayValue(rangePosition);
        String structure = higherHigh && higherLow ? "고점·저점 상승"
                : !higherHigh && !higherLow ? "고점·저점 하락"
                : higherHigh ? "고점 상승·저점 하락" : "고점 하락·저점 상승";
        String event = breakout ? "저항 돌파"
                : breakdown ? "지지 이탈"
                : failedBreakout ? "돌파 실패" : "박스권 내부";
        return new FactorResult(
                CODE,
                displayPosition,
                score,
                FactorMath.weight(input, "0.20", "0.20"),
                structure + ", " + event + "이며 최근 가격 범위 내 위치는 "
                        + displayPosition.toPlainString() + "%입니다. 20일 중심가격 괴리율 "
                        + FactorMath.displayValue(distance).toPlainString() + "%, 기울기 "
                        + FactorMath.displayValue(slope).toPlainString() + "%입니다."
        );
    }

    private BigDecimal structureScore(boolean higherHigh, boolean higherLow) {
        if (higherHigh && higherLow) return BigDecimal.valueOf(85);
        if (!higherHigh && !higherLow) return BigDecimal.valueOf(15);
        if (higherHigh) return BigDecimal.valueOf(45);
        return BigDecimal.valueOf(55);
    }

    private BigDecimal highest(List<DailyCandle> candles) {
        return candles.stream().map(DailyCandle::high).max(BigDecimal::compareTo).orElseThrow();
    }

    private BigDecimal lowest(List<DailyCandle> candles) {
        return candles.stream().map(DailyCandle::low).min(BigDecimal::compareTo).orElseThrow();
    }

    private BigDecimal averageBeforeLatest(List<DailyCandle> candles, int period, int offset) {
        int end = Math.max(1, candles.size() - offset);
        int start = Math.max(0, end - period);
        return FactorMath.average(candles.subList(start, end).stream()
                .map(DailyCandle::close)
                .toList());
    }

    private BigDecimal rangePosition(
            BigDecimal price,
            BigDecimal low,
            BigDecimal high
    ) {
        BigDecimal range = high.subtract(low);
        if (range.signum() == 0) return BigDecimal.valueOf(50);
        return FactorMath.clampScore(price.subtract(low)
                .multiply(BigDecimal.valueOf(100))
                .divide(range, 8, RoundingMode.HALF_UP));
    }
}
