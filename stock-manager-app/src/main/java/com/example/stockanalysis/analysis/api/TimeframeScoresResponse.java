package com.example.stockanalysis.analysis.api;

import com.example.stockanalysis.analysis.domain.AnalysisFactorView;
import com.example.stockanalysis.engine.factor.internal.SwingTimeframeFactorCalculator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record TimeframeScoresResponse(
        BigDecimal daily,
        BigDecimal weekly,
        BigDecimal monthly
) {

    static TimeframeScoresResponse from(List<AnalysisFactorView> factors) {
        return new TimeframeScoresResponse(
                score(factors, SwingTimeframeFactorCalculator.DAILY_CODE),
                score(factors, SwingTimeframeFactorCalculator.WEEKLY_CODE),
                score(factors, SwingTimeframeFactorCalculator.MONTHLY_CODE)
        );
    }

    private static BigDecimal score(List<AnalysisFactorView> factors, String code) {
        return factors.stream()
                .filter(factor -> code.equals(factor.factorCode()))
                .map(AnalysisFactorView::score)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .map(value -> value.setScale(1, RoundingMode.HALF_UP))
                .orElse(null);
    }
}
