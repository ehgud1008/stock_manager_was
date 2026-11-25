package com.example.stockanalysis.engine.factor.internal;

import com.example.stockanalysis.engine.factor.FactorCalculator;
import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.engine.model.FactorResult;
import java.math.BigDecimal;
import java.util.List;

public final class MomentumFactorCalculator implements FactorCalculator {

    public static final String CODE = "MOMENTUM";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public FactorResult calculate(AnalysisInput input) {
        List<DailyCandle> candles = FactorMath.orderedCandles(input);
        List<BigDecimal> closes = candles.stream().map(DailyCandle::close).toList();
        int baseIndex = Math.max(candles.size() - 21, 0);
        int shortBaseIndex = Math.max(candles.size() - 6, 0);
        BigDecimal base = candles.get(baseIndex).close();
        BigDecimal latest = candles.get(candles.size() - 1).close();
        BigDecimal returnRate = FactorMath.percentChange(latest, base);
        BigDecimal shortReturn = FactorMath.percentChange(
                latest,
                candles.get(shortBaseIndex).close()
        );
        BigDecimal returnScore = FactorMath.clampScore(
                BigDecimal.valueOf(50).add(returnRate.multiply(new BigDecimal("2.5")))
        );
        BigDecimal acceleration = shortReturn.subtract(
                returnRate.divide(BigDecimal.valueOf(4), 8, java.math.RoundingMode.HALF_UP)
        );
        BigDecimal accelerationScore = FactorMath.clampScore(
                BigDecimal.valueOf(50).add(acceleration.multiply(BigDecimal.valueOf(4)))
        );
        BigDecimal rsi = TechnicalIndicatorMath.rsi(closes, 14);
        BigDecimal rsiScore = rsiScore(rsi);
        BigDecimal divergenceScore = divergenceScore(closes, rsi);
        BigDecimal score = FactorMath.clampScore(
                returnScore.multiply(new BigDecimal("0.40"))
                        .add(accelerationScore.multiply(new BigDecimal("0.20")))
                        .add(rsiScore.multiply(new BigDecimal("0.25")))
                        .add(divergenceScore.multiply(new BigDecimal("0.15")))
        );
        BigDecimal displayReturnRate = FactorMath.displayValue(returnRate);
        return new FactorResult(
                CODE,
                displayReturnRate,
                score,
                FactorMath.weight(input, "0.15", "0.20"),
                "최근 20거래일 수익률은 " + displayReturnRate.toPlainString()
                        + "%입니다. 5일 수익률은 "
                        + FactorMath.displayValue(shortReturn).toPlainString() + "%, RSI는 "
                        + (rsi == null ? "표본 부족" : FactorMath.displayValue(rsi).toPlainString())
                        + "입니다."
        );
    }

    private BigDecimal rsiScore(BigDecimal rsi) {
        if (rsi == null) return BigDecimal.valueOf(50);
        if (rsi.compareTo(BigDecimal.valueOf(40)) < 0) return BigDecimal.valueOf(30);
        if (rsi.compareTo(BigDecimal.valueOf(50)) < 0) return BigDecimal.valueOf(55);
        if (rsi.compareTo(BigDecimal.valueOf(65)) <= 0) return BigDecimal.valueOf(85);
        if (rsi.compareTo(BigDecimal.valueOf(75)) <= 0) return BigDecimal.valueOf(65);
        return BigDecimal.valueOf(30);
    }

    private BigDecimal divergenceScore(List<BigDecimal> closes, BigDecimal currentRsi) {
        if (closes.size() < 25 || currentRsi == null) return BigDecimal.valueOf(50);
        int comparisonIndex = closes.size() - 6;
        BigDecimal pastRsi = TechnicalIndicatorMath.rsi(
                closes.subList(0, comparisonIndex),
                14
        );
        if (pastRsi == null) return BigDecimal.valueOf(50);
        boolean priceUp = closes.get(closes.size() - 1).compareTo(closes.get(comparisonIndex - 1)) > 0;
        boolean rsiUp = currentRsi.compareTo(pastRsi) > 0;
        if (priceUp && !rsiUp) return BigDecimal.valueOf(30);
        if (!priceUp && rsiUp) return BigDecimal.valueOf(70);
        return priceUp ? BigDecimal.valueOf(65) : BigDecimal.valueOf(35);
    }
}
