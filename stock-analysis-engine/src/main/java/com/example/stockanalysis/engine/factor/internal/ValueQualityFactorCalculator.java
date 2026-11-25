package com.example.stockanalysis.engine.factor.internal;

import com.example.stockanalysis.engine.factor.FactorCalculator;
import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.FactorResult;
import java.math.BigDecimal;

public final class ValueQualityFactorCalculator implements FactorCalculator {

    public static final String CODE = "VALUE_QUALITY";
    public static final String ROE = "roe";
    public static final String PER = "per";
    public static final String PBR = "pbr";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public FactorResult calculate(AnalysisInput input) {
        BigDecimal roe = input.observations().get(ROE);
        BigDecimal per = input.observations().get(PER);
        BigDecimal pbr = input.observations().get(PBR);
        BigDecimal roeScore = roe == null
                ? BigDecimal.valueOf(50)
                : FactorMath.clampScore(BigDecimal.valueOf(50)
                        .add(roe.subtract(BigDecimal.valueOf(8)).multiply(BigDecimal.valueOf(3))));
        BigDecimal perScore = per == null || per.signum() <= 0
                ? BigDecimal.valueOf(50)
                : FactorMath.clampScore(BigDecimal.valueOf(80)
                        .subtract(per.subtract(BigDecimal.valueOf(8)).multiply(BigDecimal.valueOf(2))));
        BigDecimal pbrScore = pbr == null || pbr.signum() <= 0
                ? BigDecimal.valueOf(50)
                : FactorMath.clampScore(BigDecimal.valueOf(80)
                        .subtract(pbr.subtract(BigDecimal.ONE).multiply(BigDecimal.valueOf(15))));
        BigDecimal score = FactorMath.average(java.util.List.of(roeScore, perScore, pbrScore));
        BigDecimal rawValue = FactorMath.displayValue(roe == null ? BigDecimal.ZERO : roe);
        return new FactorResult(
                CODE,
                rawValue,
                score,
                FactorMath.weight(input, "0.05", "0.05"),
                "ROE·PER·PBR를 결합한 가치·수익성 점수입니다."
        );
    }
}
