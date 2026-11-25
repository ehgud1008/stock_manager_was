package com.example.stockanalysis.engine.model;

import java.math.BigDecimal;
import java.util.Objects;

public record StockSnapshot(
        String stockCode,
        String stockName,
        String marketCode,
        BigDecimal currentPrice
) {

    public StockSnapshot {
        if (stockCode == null || !stockCode.matches("\\d{6}")) {
            throw new IllegalArgumentException("stockCode must contain exactly six digits");
        }
        stockName = requireText(stockName, "stockName");
        marketCode = requireText(marketCode, "marketCode");
        Objects.requireNonNull(currentPrice, "currentPrice");
        if (currentPrice.signum() <= 0) {
            throw new IllegalArgumentException("currentPrice must be positive");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
