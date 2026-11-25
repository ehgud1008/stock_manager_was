package com.example.stockanalysis.engine.factor.internal;

import com.example.stockanalysis.engine.model.DailyCandle;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

final class TechnicalIndicatorMath {

    private static final int SCALE = 10;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private TechnicalIndicatorMath() {
    }

    static BigDecimal simpleMovingAverage(List<BigDecimal> values, int endExclusive, int period) {
        if (period <= 0 || endExclusive < period || endExclusive > values.size()) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (int index = endExclusive - period; index < endExclusive; index++) {
            sum = sum.add(values.get(index));
        }
        return sum.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
    }

    static BigDecimal rsi(List<BigDecimal> closes, int period) {
        if (closes.size() <= period) {
            return null;
        }
        BigDecimal gains = BigDecimal.ZERO;
        BigDecimal losses = BigDecimal.ZERO;
        for (int index = 1; index <= period; index++) {
            BigDecimal change = closes.get(index).subtract(closes.get(index - 1));
            if (change.signum() >= 0) gains = gains.add(change);
            else losses = losses.add(change.abs());
        }
        BigDecimal divisor = BigDecimal.valueOf(period);
        BigDecimal averageGain = gains.divide(divisor, SCALE, RoundingMode.HALF_UP);
        BigDecimal averageLoss = losses.divide(divisor, SCALE, RoundingMode.HALF_UP);
        for (int index = period + 1; index < closes.size(); index++) {
            BigDecimal change = closes.get(index).subtract(closes.get(index - 1));
            BigDecimal gain = change.max(BigDecimal.ZERO);
            BigDecimal loss = change.min(BigDecimal.ZERO).abs();
            averageGain = averageGain.multiply(BigDecimal.valueOf(period - 1))
                    .add(gain).divide(divisor, SCALE, RoundingMode.HALF_UP);
            averageLoss = averageLoss.multiply(BigDecimal.valueOf(period - 1))
                    .add(loss).divide(divisor, SCALE, RoundingMode.HALF_UP);
        }
        if (averageLoss.signum() == 0) {
            return averageGain.signum() == 0 ? BigDecimal.valueOf(50) : ONE_HUNDRED;
        }
        BigDecimal relativeStrength = averageGain.divide(
                averageLoss,
                SCALE,
                RoundingMode.HALF_UP
        );
        return ONE_HUNDRED.subtract(
                ONE_HUNDRED.divide(BigDecimal.ONE.add(relativeStrength), SCALE, RoundingMode.HALF_UP)
        );
    }

    static Macd macd(List<BigDecimal> closes) {
        List<BigDecimal> fast = exponentialMovingAverageSeries(closes, 12);
        List<BigDecimal> slow = exponentialMovingAverageSeries(closes, 26);
        List<BigDecimal> macdValues = new ArrayList<>();
        for (int index = 0; index < closes.size(); index++) {
            if (fast.get(index) != null && slow.get(index) != null) {
                macdValues.add(fast.get(index).subtract(slow.get(index))
                        .setScale(SCALE, RoundingMode.HALF_UP));
            }
        }
        if (macdValues.size() < 9) {
            return null;
        }
        List<BigDecimal> signals = exponentialMovingAverageSeries(macdValues, 9);
        BigDecimal macd = macdValues.get(macdValues.size() - 1);
        BigDecimal signal = signals.get(signals.size() - 1);
        return signal == null ? null : new Macd(
                macd,
                signal,
                macd.subtract(signal).setScale(SCALE, RoundingMode.HALF_UP)
        );
    }

    static Ichimoku ichimoku(List<DailyCandle> candles) {
        int current = candles.size() - 1;
        int cloudSource = current - 26;
        if (current < 25 || cloudSource < 51) {
            return null;
        }
        BigDecimal conversion = midpoint(candles, current + 1, 9);
        BigDecimal base = midpoint(candles, current + 1, 26);
        BigDecimal sourceConversion = midpoint(candles, cloudSource + 1, 9);
        BigDecimal sourceBase = midpoint(candles, cloudSource + 1, 26);
        BigDecimal spanA = sourceConversion.add(sourceBase)
                .divide(BigDecimal.valueOf(2), SCALE, RoundingMode.HALF_UP);
        BigDecimal spanB = midpoint(candles, cloudSource + 1, 52);
        return new Ichimoku(conversion, base, spanA, spanB);
    }

    static CrossSignal recentCross(
            List<BigDecimal> closes,
            int shortPeriod,
            int longPeriod,
            int lookback
    ) {
        CrossSignal latest = CrossSignal.NONE;
        int start = Math.max(longPeriod + 1, closes.size() - lookback);
        for (int end = start; end <= closes.size(); end++) {
            BigDecimal previousShort = simpleMovingAverage(closes, end - 1, shortPeriod);
            BigDecimal previousLong = simpleMovingAverage(closes, end - 1, longPeriod);
            BigDecimal currentShort = simpleMovingAverage(closes, end, shortPeriod);
            BigDecimal currentLong = simpleMovingAverage(closes, end, longPeriod);
            if (previousShort == null || previousLong == null
                    || currentShort == null || currentLong == null) {
                continue;
            }
            if (previousShort.compareTo(previousLong) <= 0
                    && currentShort.compareTo(currentLong) > 0) {
                latest = CrossSignal.GOLDEN;
            } else if (previousShort.compareTo(previousLong) >= 0
                    && currentShort.compareTo(currentLong) < 0) {
                latest = CrossSignal.DEAD;
            }
        }
        return latest;
    }

    private static List<BigDecimal> exponentialMovingAverageSeries(
            List<BigDecimal> values,
            int period
    ) {
        List<BigDecimal> result = new ArrayList<>(java.util.Collections.nCopies(values.size(), null));
        if (values.size() < period) {
            return result;
        }
        BigDecimal ema = simpleMovingAverage(values, period, period);
        result.set(period - 1, ema);
        BigDecimal multiplier = BigDecimal.valueOf(2)
                .divide(BigDecimal.valueOf(period + 1L), SCALE, RoundingMode.HALF_UP);
        for (int index = period; index < values.size(); index++) {
            ema = values.get(index).subtract(ema).multiply(multiplier).add(ema)
                    .setScale(SCALE, RoundingMode.HALF_UP);
            result.set(index, ema);
        }
        return result;
    }

    private static BigDecimal midpoint(List<DailyCandle> candles, int endExclusive, int period) {
        if (endExclusive < period) {
            return null;
        }
        BigDecimal highest = null;
        BigDecimal lowest = null;
        for (int index = endExclusive - period; index < endExclusive; index++) {
            DailyCandle candle = candles.get(index);
            highest = highest == null ? candle.high() : highest.max(candle.high());
            lowest = lowest == null ? candle.low() : lowest.min(candle.low());
        }
        return highest.add(lowest).divide(BigDecimal.valueOf(2), SCALE, RoundingMode.HALF_UP);
    }

    record Macd(BigDecimal macd, BigDecimal signal, BigDecimal histogram) {
    }

    record Ichimoku(BigDecimal conversion, BigDecimal base, BigDecimal spanA, BigDecimal spanB) {
    }

    enum CrossSignal {
        GOLDEN,
        DEAD,
        NONE
    }
}
