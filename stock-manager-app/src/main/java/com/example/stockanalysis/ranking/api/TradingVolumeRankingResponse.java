package com.example.stockanalysis.ranking.api;

import com.example.stockanalysis.ranking.domain.TradingVolumeRankingPage;
import java.util.List;

public record TradingVolumeRankingResponse(
        List<TradingVolumeRankedStockResponse> items,
        boolean hasNext,
        String nextPageToken
) {

    public static TradingVolumeRankingResponse from(
            TradingVolumeRankingPage page
    ) {
        return new TradingVolumeRankingResponse(
                page.items().stream()
                        .map(TradingVolumeRankedStockResponse::from)
                        .toList(),
                page.hasNext(),
                page.nextPageToken()
        );
    }
}
