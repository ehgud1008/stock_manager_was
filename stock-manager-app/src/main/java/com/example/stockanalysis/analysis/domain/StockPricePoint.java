package com.example.stockanalysis.analysis.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockPricePoint(
        LocalDate businessDate,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        long volume
) {
}
