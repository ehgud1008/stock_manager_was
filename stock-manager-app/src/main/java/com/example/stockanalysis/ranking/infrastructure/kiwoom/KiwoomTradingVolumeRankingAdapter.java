package com.example.stockanalysis.ranking.infrastructure.kiwoom;

import com.example.stockanalysis.kiwoom.client.KiwoomResponse;
import com.example.stockanalysis.kiwoom.client.ranking.KiwoomRankingClient;
import com.example.stockanalysis.kiwoom.contract.tr.ka10030.Ka10030Request;
import com.example.stockanalysis.kiwoom.contract.tr.ka10030.Ka10030Response;
import com.example.stockanalysis.ranking.application.port.out.TradingVolumeRankingPort;
import com.example.stockanalysis.ranking.domain.TradingVolumeRankingCriteria;
import com.example.stockanalysis.ranking.domain.TradingVolumeRankingPage;
import org.springframework.stereotype.Component;

@Component
public class KiwoomTradingVolumeRankingAdapter
        implements TradingVolumeRankingPort {

    private final KiwoomRankingClient client;
    private final KiwoomTradingVolumeRankingMapper mapper;

    public KiwoomTradingVolumeRankingAdapter(
            KiwoomRankingClient client,
            KiwoomTradingVolumeRankingMapper mapper
    ) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public TradingVolumeRankingPage findTradingVolumeRanking(
            TradingVolumeRankingCriteria criteria
    ) {
        Ka10030Request request = mapper.toRequest(criteria);
        KiwoomResponse<Ka10030Response> response =
                client.getTradingVolumeRanking(request, criteria.pageToken());
        return mapper.toDomain(response);
    }
}
