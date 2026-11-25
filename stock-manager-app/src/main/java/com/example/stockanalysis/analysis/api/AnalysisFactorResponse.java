package com.example.stockanalysis.analysis.api;

import com.example.stockanalysis.analysis.domain.AnalysisFactorView;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record AnalysisFactorResponse(
        String factorCode,
        BigDecimal rawValue,
        BigDecimal score,
        BigDecimal weight,
        BigDecimal contribution,
        String reason
) {

    static AnalysisFactorResponse from(AnalysisFactorView factor) {
        BigDecimal displayRawValue = factor.rawValue() == null
                ? null
                : factor.rawValue().setScale(1, RoundingMode.HALF_UP);
        return new AnalysisFactorResponse(
                factor.factorCode(),
                displayRawValue,
                factor.score(),
                factor.weight(),
                factor.contribution(),
                displayReason(factor.reason(), displayRawValue)
        );
    }

    private static String displayReason(String reason, BigDecimal displayRawValue) {
        if (reason == null || displayRawValue == null) {
            return reason;
        }
        return java.util.regex.Pattern.compile("[-+]?\\d+\\.\\d{2,}(?=%|배)")
                .matcher(reason).replaceAll(match -> new BigDecimal(match.group())
                        .setScale(1, RoundingMode.HALF_UP).toPlainString());
    }
}
