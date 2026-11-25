package com.example.stockanalysis.analysis.api;

import com.example.stockanalysis.analysis.domain.StockChartPoint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record StockPriceResponse(
        OffsetDateTime timestamp,
        LocalDate businessDate,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        long volume
) {

    static StockPriceResponse from(StockChartPoint price) {
        return new StockPriceResponse(
                price.timestamp(),
                price.timestamp().toLocalDate(),
                price.open(),
                price.high(),
                price.low(),
                price.close(),
                price.volume()
        );
    }
}
