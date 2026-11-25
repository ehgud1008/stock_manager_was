package com.example.stockanalysis.stock.api;

import com.example.stockanalysis.stock.domain.StockProfile;
import java.math.BigDecimal;

public record StockProfileResponse(
        String stockCode,
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

    public static StockProfileResponse from(StockProfile profile) {
        return new StockProfileResponse(
                profile.stockCode().value(),
                profile.stockName(),
                profile.currentPrice(),
                profile.openPrice(),
                profile.highPrice(),
                profile.lowPrice(),
                profile.marketCapitalization(),
                profile.listedShares(),
                profile.per(),
                profile.eps(),
                profile.roe(),
                profile.pbr(),
                profile.bps()
        );
    }
}
