package com.example.stockanalysis.ranking.domain;

public record TradingVolumeRankingCriteria(
        RankingMarket market,
        RankingSort sort,
        boolean includeManagedStocks,
        TradingExchange exchange,
        String pageToken
) {
}
