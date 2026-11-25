package com.example.stockanalysis.ranking.api;

import com.example.stockanalysis.ranking.domain.TradingVolumeRankedStock;
import java.math.BigDecimal;

public record TradingVolumeRankedStockResponse(
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

    public static TradingVolumeRankedStockResponse from(
            TradingVolumeRankedStock stock
    ) {
        return new TradingVolumeRankedStockResponse(
                stock.rank(),
                stock.stockCode(),
                stock.stockName(),
                stock.currentPrice(),
                stock.previousDayChangeSign(),
                stock.previousDayChange(),
                stock.changeRate(),
                stock.tradingVolume(),
                stock.previousDayRatio(),
                stock.turnoverRate(),
                stock.tradingAmountMillionWon()
        );
    }
}
