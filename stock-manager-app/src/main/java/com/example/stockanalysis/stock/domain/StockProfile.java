package com.example.stockanalysis.stock.domain;

import java.math.BigDecimal;

public record StockProfile(
        StockCode stockCode,
        String stockName,
        BigDecimal currentPrice,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal marketCapitalization,
        BigDecimal listedShares,
        BigDecimal per,
        BigDecimal eps,
        BigDecimal roe,
        BigDecimal pbr,
        BigDecimal bps
) {
}
