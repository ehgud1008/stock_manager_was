package com.example.stockanalysis.engine.factor.internal;

import com.example.stockanalysis.engine.factor.FactorCalculator;
import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.AnalysisTimeframe;
import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.engine.model.FactorResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class SwingTimeframeFactorCalculator implements FactorCalculator {

    public static final String DAILY_CODE = "DAILY_TECHNICAL";
    public static final String WEEKLY_CODE = "WEEKLY_TECHNICAL";
    public static final String MONTHLY_CODE = "MONTHLY_TECHNICAL";

    private final AnalysisTimeframe timeframe;
    private final String code;
    private final String label;
    private final int shortPeriod;
    private final int middlePeriod;
    private final int longPeriod;
    private final int momentumPeriod;
    private final BigDecimal momentumMultiplier;
    private final String swingWeight;
    private final String shortTermWeight;

    private SwingTimeframeFactorCalculator(
            AnalysisTimeframe timeframe,
            String code,
            String label,
            int shortPeriod,
            int middlePeriod,
            int longPeriod,
            int momentumPeriod,
            String momentumMultiplier,
            String swingWeight,
            String shortTermWeight
    ) {
        this.timeframe = timeframe;
        this.code = code;
        this.label = label;
        this.shortPeriod = shortPeriod;
        this.middlePeriod = middlePeriod;
        this.longPeriod = longPeriod;
        this.momentumPeriod = momentumPeriod;
        this.momentumMultiplier = new BigDecimal(momentumMultiplier);
        this.swingWeight = swingWeight;
        this.shortTermWeight = shortTermWeight;
    }

    public static SwingTimeframeFactorCalculator daily() {
        return new SwingTimeframeFactorCalculator(
                AnalysisTimeframe.DAILY,
                DAILY_CODE,
                "일봉",
                20,
                60,
                180,
                20,
                "2.5",
                "0.10",
                "0.10"
        );
    }

    public static SwingTimeframeFactorCalculator weekly() {
        return new SwingTimeframeFactorCalculator(
                AnalysisTimeframe.WEEKLY,
                WEEKLY_CODE,
                "주봉",
                5,
                20,
                40,
                12,
                "2.0",
                "0.025",
                "0.025"
        );
    }

    public static SwingTimeframeFactorCalculator monthly() {
        return new SwingTimeframeFactorCalculator(
                AnalysisTimeframe.MONTHLY,
                MONTHLY_CODE,
                "월봉",
                6,
                12,
                24,
                6,
                "1.0",
                "0.025",
                "0.025"
        );
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public FactorResult calculate(AnalysisInput input) {
        List<DailyCandle> candles = FactorMath.orderedCandles(input.candles(timeframe));
        BigDecimal weight = FactorMath.weight(input, swingWeight, shortTermWeight);
        if (candles.size() < longPeriod) {
            return new FactorResult(
                    code,
                    BigDecimal.ZERO.setScale(1),
                    BigDecimal.valueOf(50).setScale(4),
                    weight,
                    label + " 표본이 " + candles.size() + "개로 부족하여 중립 점수를 적용했습니다."
            );
        }

        List<BigDecimal> closes = candles.stream().map(DailyCandle::close).toList();
        int size = closes.size();
        BigDecimal latest = closes.get(size - 1);
        BigDecimal shortAverage = TechnicalIndicatorMath.simpleMovingAverage(
                closes, size, shortPeriod
        );
        BigDecimal middleAverage = TechnicalIndicatorMath.simpleMovingAverage(
                closes, size, middlePeriod
        );
        BigDecimal longAverage = TechnicalIndicatorMath.simpleMovingAverage(
                closes, size, longPeriod
        );
        BigDecimal longAveragePast = TechnicalIndicatorMath.simpleMovingAverage(
                closes,
                size - Math.min(5, Math.max(1, size - longPeriod)),
                longPeriod
        );
        BigDecimal longSlope = longAveragePast == null
                ? BigDecimal.ZERO
                : FactorMath.percentChange(longAverage, longAveragePast);
        BigDecimal trendScore = trendScore(
                latest,
                shortAverage,
                middleAverage,
                longAverage,
                longSlope
        );
        BigDecimal structureScore = priceStructureScore(candles);
        int baseIndex = Math.max(size - 1 - momentumPeriod, 0);
        BigDecimal returnRate = FactorMath.percentChange(latest, closes.get(baseIndex));
        BigDecimal momentumScore = FactorMath.clampScore(
                BigDecimal.valueOf(50).add(returnRate.multiply(momentumMultiplier))
        );

        BigDecimal rsi = TechnicalIndicatorMath.rsi(closes, 14);
        BigDecimal rsiScore = rsiScore(rsi);
        TechnicalIndicatorMath.Macd macd = TechnicalIndicatorMath.macd(closes);
        BigDecimal macdScore = macdScore(macd);
        TechnicalIndicatorMath.Ichimoku ichimoku = TechnicalIndicatorMath.ichimoku(candles);
        BigDecimal ichimokuScore = ichimokuScore(latest, ichimoku);
        BigDecimal volumeRatio = volumeRatio(candles);
        BigDecimal volumeScore = FactorMath.clampScore(
                BigDecimal.valueOf(50).add(
                        volumeRatio.subtract(BigDecimal.ONE).multiply(BigDecimal.valueOf(40))
                )
        );

        BigDecimal score = FactorMath.clampScore(
                structureScore.multiply(new BigDecimal("0.30"))
                        .add(trendScore.multiply(new BigDecimal("0.15")))
                        .add(momentumScore.multiply(new BigDecimal("0.15")))
                        .add(rsiScore.multiply(new BigDecimal("0.10")))
                        .add(macdScore.multiply(new BigDecimal("0.10")))
                        .add(ichimokuScore.multiply(new BigDecimal("0.10")))
                        .add(volumeScore.multiply(new BigDecimal("0.10")))
        );
        BigDecimal displayReturn = FactorMath.displayValue(returnRate);
        return new FactorResult(
                code,
                displayReturn,
                score,
                weight,
                reason(
                        latest,
                        displayReturn,
                        shortAverage,
                        middleAverage,
                        longAverage,
                        rsi,
                        macd,
                        ichimoku,
                        volumeRatio,
                        priceStructureLabel(candles)
                )
        );
    }

    private BigDecimal trendScore(
            BigDecimal latest,
            BigDecimal shortAverage,
            BigDecimal middleAverage,
            BigDecimal longAverage,
            BigDecimal longSlope
    ) {
        BigDecimal score;
        if (latest.compareTo(shortAverage) > 0
                && shortAverage.compareTo(middleAverage) > 0
                && middleAverage.compareTo(longAverage) > 0) {
            score = BigDecimal.valueOf(90);
        } else if (latest.compareTo(middleAverage) > 0
                && middleAverage.compareTo(longAverage) > 0) {
            score = BigDecimal.valueOf(75);
        } else if (latest.compareTo(longAverage) > 0) {
            score = BigDecimal.valueOf(60);
        } else if (shortAverage.compareTo(middleAverage) > 0) {
            score = BigDecimal.valueOf(45);
        } else {
            score = BigDecimal.valueOf(25);
        }
        if (longSlope.signum() > 0) score = score.add(BigDecimal.TEN);
        else if (longSlope.signum() < 0) score = score.subtract(BigDecimal.TEN);
        return FactorMath.clampScore(score);
    }

    private BigDecimal priceStructureScore(List<DailyCandle> candles) {
        int window = Math.min(20, Math.max(3, (candles.size() - 1) / 2));
        int recentFrom = candles.size() - 1 - window;
        int previousFrom = recentFrom - window;
        if (previousFrom < 0) return BigDecimal.valueOf(50);
        List<DailyCandle> previous = candles.subList(previousFrom, recentFrom);
        List<DailyCandle> recent = candles.subList(recentFrom, candles.size() - 1);
        DailyCandle latest = candles.get(candles.size() - 1);
        BigDecimal previousHigh = highest(previous);
        BigDecimal previousLow = lowest(previous);
        BigDecimal recentHigh = highest(recent);
        BigDecimal recentLow = lowest(recent);
        boolean higherHigh = recentHigh.compareTo(previousHigh) > 0;
        boolean higherLow = recentLow.compareTo(previousLow) > 0;
        BigDecimal score = higherHigh && higherLow ? BigDecimal.valueOf(85)
                : !higherHigh && !higherLow ? BigDecimal.valueOf(15)
                : higherHigh ? BigDecimal.valueOf(45) : BigDecimal.valueOf(55);
        if (latest.close().compareTo(recentHigh) > 0) score = score.add(BigDecimal.valueOf(15));
        if (latest.close().compareTo(recentLow) < 0) score = score.subtract(BigDecimal.valueOf(15));
        if (latest.high().compareTo(recentHigh) > 0
                && latest.close().compareTo(recentHigh) <= 0) {
            score = score.subtract(BigDecimal.TEN);
        }
        return FactorMath.clampScore(score);
    }

    private String priceStructureLabel(List<DailyCandle> candles) {
        int window = Math.min(20, Math.max(3, (candles.size() - 1) / 2));
        int recentFrom = candles.size() - 1 - window;
        int previousFrom = recentFrom - window;
        if (previousFrom < 0) return "가격 구조 표본 부족";
        List<DailyCandle> previous = candles.subList(previousFrom, recentFrom);
        List<DailyCandle> recent = candles.subList(recentFrom, candles.size() - 1);
        boolean higherHigh = highest(recent).compareTo(highest(previous)) > 0;
        boolean higherLow = lowest(recent).compareTo(lowest(previous)) > 0;
        if (higherHigh && higherLow) return "고점·저점 상승";
        if (!higherHigh && !higherLow) return "고점·저점 하락";
        return higherHigh ? "고점 상승·저점 하락" : "고점 하락·저점 상승";
    }

    private BigDecimal highest(List<DailyCandle> candles) {
        return candles.stream().map(DailyCandle::high).max(BigDecimal::compareTo).orElseThrow();
    }

    private BigDecimal lowest(List<DailyCandle> candles) {
        return candles.stream().map(DailyCandle::low).min(BigDecimal::compareTo).orElseThrow();
    }

    private BigDecimal rsiScore(BigDecimal rsi) {
        if (rsi == null) return BigDecimal.valueOf(50);
        if (rsi.compareTo(BigDecimal.valueOf(30)) < 0) return BigDecimal.valueOf(35);
        if (rsi.compareTo(BigDecimal.valueOf(40)) < 0) return BigDecimal.valueOf(45);
        if (rsi.compareTo(BigDecimal.valueOf(50)) < 0) return BigDecimal.valueOf(60);
        if (rsi.compareTo(BigDecimal.valueOf(65)) <= 0) return BigDecimal.valueOf(85);
        if (rsi.compareTo(BigDecimal.valueOf(75)) <= 0) return BigDecimal.valueOf(70);
        return BigDecimal.valueOf(40);
    }

    private BigDecimal macdScore(TechnicalIndicatorMath.Macd macd) {
        if (macd == null) return BigDecimal.valueOf(50);
        boolean signalUp = macd.macd().compareTo(macd.signal()) > 0;
        boolean aboveZero = macd.macd().signum() > 0;
        if (signalUp && aboveZero) return BigDecimal.valueOf(90);
        if (signalUp) return BigDecimal.valueOf(70);
        if (aboveZero) return BigDecimal.valueOf(50);
        return BigDecimal.valueOf(30);
    }

    private BigDecimal ichimokuScore(
            BigDecimal latest,
            TechnicalIndicatorMath.Ichimoku ichimoku
    ) {
        if (ichimoku == null) return BigDecimal.valueOf(50);
        BigDecimal cloudTop = ichimoku.spanA().max(ichimoku.spanB());
        BigDecimal cloudBottom = ichimoku.spanA().min(ichimoku.spanB());
        boolean conversionAboveBase = ichimoku.conversion().compareTo(ichimoku.base()) > 0;
        if (latest.compareTo(cloudTop) > 0) {
            return BigDecimal.valueOf(conversionAboveBase ? 90 : 75);
        }
        if (latest.compareTo(cloudBottom) >= 0) return BigDecimal.valueOf(50);
        return BigDecimal.valueOf(conversionAboveBase ? 30 : 15);
    }

    private BigDecimal volumeRatio(List<DailyCandle> candles) {
        int sampleSize = Math.min(20, candles.size());
        List<BigDecimal> volumes = candles.subList(candles.size() - sampleSize, candles.size())
                .stream()
                .map(candle -> BigDecimal.valueOf(candle.volume()))
                .toList();
        BigDecimal average = FactorMath.average(volumes);
        if (average.signum() == 0) return BigDecimal.ONE;
        return volumes.get(volumes.size() - 1)
                .divide(average, 8, RoundingMode.HALF_UP);
    }

    private String reason(
            BigDecimal latest,
            BigDecimal returnRate,
            BigDecimal shortAverage,
            BigDecimal middleAverage,
            BigDecimal longAverage,
            BigDecimal rsi,
            TechnicalIndicatorMath.Macd macd,
            TechnicalIndicatorMath.Ichimoku ichimoku,
            BigDecimal volumeRatio,
            String priceStructure
    ) {
        String macdText = macd == null
                ? "MACD 표본 부족"
                : macd.macd().compareTo(macd.signal()) > 0 ? "MACD 상승" : "MACD 하락";
        String cloudText;
        if (ichimoku == null) {
            cloudText = "일목 표본 부족";
        } else {
            BigDecimal top = ichimoku.spanA().max(ichimoku.spanB());
            BigDecimal bottom = ichimoku.spanA().min(ichimoku.spanB());
            if (latest.compareTo(top) > 0) cloudText = "일목 구름 위";
            else if (latest.compareTo(bottom) >= 0) cloudText = "일목 구름 안";
            else cloudText = "일목 구름 아래";
        }
        return label + " " + momentumPeriod + "기간 수익률은 " + returnRate.toPlainString()
                + "%입니다. MA" + shortPeriod + "/" + middlePeriod + "/" + longPeriod
                + "=" + price(shortAverage) + "/" + price(middleAverage) + "/" + price(longAverage)
                + ", RSI=" + (rsi == null ? "표본 부족" : FactorMath.displayValue(rsi).toPlainString())
                + ", " + macdText + ", " + cloudText + ", 거래량 "
                + FactorMath.displayValue(volumeRatio).toPlainString() + "배, "
                + priceStructure + "입니다.";
    }

    private String price(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP).toPlainString();
    }
}
