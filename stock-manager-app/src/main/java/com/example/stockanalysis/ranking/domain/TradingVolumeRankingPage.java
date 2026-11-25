package com.example.stockanalysis.ranking.domain;

import java.util.List;

public record TradingVolumeRankingPage(
        List<TradingVolumeRankedStock> items,
        boolean hasNext,
        String nextPageToken
) {
}
