package com.example.stockanalysis.ranking.domain;

import java.math.BigDecimal;

public record TradingVolumeRankedStock(
        int rank,
        String stockCode,
        String stockName,
        BigDecimal currentPrice,
        String previousDayChangeSign,
        BigDecimal previousDayChange,
        BigDecimal changeRate,
        BigDecimal tradingVolume,
        BigDecimal previousDayRatio,
        BigDecimal turnoverRate,
        BigDecimal tradingAmountMillionWon
) {
}
