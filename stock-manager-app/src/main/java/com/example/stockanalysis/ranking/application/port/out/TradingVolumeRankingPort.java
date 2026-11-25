package com.example.stockanalysis.ranking.application.port.out;

import com.example.stockanalysis.ranking.domain.TradingVolumeRankingCriteria;
import com.example.stockanalysis.ranking.domain.TradingVolumeRankingPage;

public interface TradingVolumeRankingPort {

    TradingVolumeRankingPage findTradingVolumeRanking(
            TradingVolumeRankingCriteria criteria
    );
}
