package com.example.stockanalysis.analysis.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record StockChartPoint(
        OffsetDateTime timestamp,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        long volume
) {
}
