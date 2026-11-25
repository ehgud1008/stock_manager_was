package com.example.stockanalysis.analysis.domain;

import java.math.BigDecimal;

public record AnalysisFactorView(
        String factorCode,
        BigDecimal rawValue,
        BigDecimal score,
        BigDecimal weight,
        BigDecimal contribution,
        String reason
) {
}
