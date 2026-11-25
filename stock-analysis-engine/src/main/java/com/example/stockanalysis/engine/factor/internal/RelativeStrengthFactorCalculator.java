package com.example.stockanalysis.engine.factor.internal;

import com.example.stockanalysis.engine.factor.FactorCalculator;
import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.FactorResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 시장과 업종의 동일 기간 수익률을 뺀 상대수익률을 점수화한다.
 * 비교 데이터가 아직 수집되지 않은 경우 방향을 왜곡하지 않도록 중립값을 반환한다.
 */
public final class RelativeStrengthFactorCalculator implements FactorCalculator {

    public static final String CODE = "RELATIVE_STRENGTH";
    public static final String MARKET_RETURN_20 = "marketReturn20";
    public static final String INDUSTRY_RETURN_20 = "industryReturn20";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public FactorResult calculate(AnalysisInput input) {
        List<BigDecimal> closes = FactorMath.orderedCandles(input).stream()
                .map(candle -> candle.close())
                .toList();
        int baseIndex = Math.max(0, closes.size() - 21);
        BigDecimal stockReturn = FactorMath.percentChange(
                closes.get(closes.size() - 1),
                closes.get(baseIndex)
        );
        BigDecimal marketReturn = input.observations().get(MARKET_RETURN_20);
        BigDecimal industryReturn = input.observations().get(INDUSTRY_RETURN_20);
        List<BigDecimal> relativeReturns = new ArrayList<>();
        if (marketReturn != null) relativeReturns.add(stockReturn.subtract(marketReturn));
        if (industryReturn != null) relativeReturns.add(stockReturn.subtract(industryReturn));

        if (relativeReturns.isEmpty()) {
            return new FactorResult(
                    CODE,
                    BigDecimal.ZERO.setScale(1),
                    BigDecimal.valueOf(50).setScale(4),
                    FactorMath.weight(input, "0.10", "0.10"),
                    "시장·업종 비교 수익률이 없어 상대강도는 중립 점수를 적용했습니다."
            );
        }

        BigDecimal relativeStrength = FactorMath.average(relativeReturns);
        BigDecimal score = FactorMath.clampScore(
                BigDecimal.valueOf(50).add(relativeStrength.multiply(BigDecimal.valueOf(4)))
        );
        BigDecimal display = FactorMath.displayValue(relativeStrength);
        String basis = marketReturn != null && industryReturn != null
                ? "시장·업종 평균" : marketReturn != null ? "시장" : "업종";
        return new FactorResult(
                CODE,
                display,
                score,
                FactorMath.weight(input, "0.10", "0.10"),
                "종목의 20일 수익률은 " + basis + " 대비 "
                        + display.toPlainString() + "%입니다."
        );
    }
}
