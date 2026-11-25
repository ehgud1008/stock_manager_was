package com.example.stockanalysis.engine.factor.internal;

import com.example.stockanalysis.engine.factor.FactorCalculator;
import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.FactorResult;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class SupplyDemandFactorCalculator implements FactorCalculator {

    public static final String CODE = "SUPPLY_DEMAND";
    public static final String FOREIGN_NET_BUY = "foreignNetBuySum";
    public static final String INSTITUTION_NET_BUY = "institutionNetBuySum";
    public static final String AVERAGE_VOLUME = "averageVolume20";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public FactorResult calculate(AnalysisInput input) {
        BigDecimal foreign = input.observations().getOrDefault(FOREIGN_NET_BUY, BigDecimal.ZERO);
        BigDecimal institution = input.observations().getOrDefault(INSTITUTION_NET_BUY, BigDecimal.ZERO);
        BigDecimal averageVolume = input.observations().getOrDefault(AVERAGE_VOLUME, BigDecimal.ZERO);
        BigDecimal netBuyRate = averageVolume.signum() == 0
                ? BigDecimal.ZERO
                : foreign.add(institution)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(averageVolume.abs(), 8, RoundingMode.HALF_UP);
        BigDecimal score = FactorMath.clampScore(
                BigDecimal.valueOf(50).add(netBuyRate.multiply(BigDecimal.valueOf(5)))
        );
        BigDecimal displayNetBuyRate = FactorMath.displayValue(netBuyRate);
        return new FactorResult(
                CODE,
                displayNetBuyRate,
                score,
                FactorMath.weight(input, "0.15", "0.10"),
                "외국인·기관 합산 순매수는 20일 평균 거래량의 "
                        + displayNetBuyRate.toPlainString() + "%입니다."
        );
    }
}
