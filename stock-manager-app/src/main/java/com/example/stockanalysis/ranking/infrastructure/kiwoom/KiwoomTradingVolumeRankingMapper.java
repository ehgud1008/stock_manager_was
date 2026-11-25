package com.example.stockanalysis.ranking.infrastructure.kiwoom;

import com.example.stockanalysis.kiwoom.client.KiwoomNumberParser;
import com.example.stockanalysis.kiwoom.client.KiwoomResponse;
import com.example.stockanalysis.kiwoom.contract.tr.ka10030.Ka10030Request;
import com.example.stockanalysis.kiwoom.contract.tr.ka10030.Ka10030Response;
import com.example.stockanalysis.kiwoom.contract.tr.ka10030.Ka10030Response.TdyTrdeQtyUpperItem;
import com.example.stockanalysis.ranking.domain.RankingMarket;
import com.example.stockanalysis.ranking.domain.RankingSort;
import com.example.stockanalysis.ranking.domain.TradingExchange;
import com.example.stockanalysis.ranking.domain.TradingVolumeRankedStock;
import com.example.stockanalysis.ranking.domain.TradingVolumeRankingCriteria;
import com.example.stockanalysis.ranking.domain.TradingVolumeRankingPage;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class KiwoomTradingVolumeRankingMapper {

    private final KiwoomNumberParser numberParser;

    public KiwoomTradingVolumeRankingMapper(KiwoomNumberParser numberParser) {
        this.numberParser = numberParser;
    }

    public Ka10030Request toRequest(TradingVolumeRankingCriteria criteria) {
        return Ka10030Request.builder()
                .mrktTp(marketCode(criteria.market()))
                .sortTp(sortCode(criteria.sort()))
                .mangStkIncls(criteria.includeManagedStocks() ? "0" : "1")
                .crdTp("0")
                .trdeQtyTp("0")
                .pricTp("0")
                .trdePricaTp("0")
                .mrktOpenTp("0")
                .stexTp(exchangeCode(criteria.exchange()))
                .build();
    }

    public TradingVolumeRankingPage toDomain(
            KiwoomResponse<Ka10030Response> response
    ) {
        List<TdyTrdeQtyUpperItem> rawItems =
                response.body().getTdyTrdeQtyUpper();
        if (rawItems == null) {
            rawItems = List.of();
        }

        List<TradingVolumeRankedStock> items = new ArrayList<>(rawItems.size());
        for (int index = 0; index < rawItems.size(); index++) {
            items.add(toDomain(index + 1, rawItems.get(index)));
        }

        return new TradingVolumeRankingPage(
                List.copyOf(items),
                response.continuation().hasNext(),
                response.continuation().nextKey()
        );
    }

    private TradingVolumeRankedStock toDomain(
            int rank,
            TdyTrdeQtyUpperItem item
    ) {
        return new TradingVolumeRankedStock(
                rank,
                item.getStkCd(),
                item.getStkNm(),
                numberParser.absoluteDecimal(item.getCurPrc(), "현재가"),
                item.getPredPreSig(),
                numberParser.decimal(item.getPredPre(), "전일대비"),
                numberParser.decimal(item.getFluRt(), "등락률"),
                numberParser.decimal(item.getTrdeQty(), "거래량"),
                numberParser.decimal(item.getPredRt(), "전일비"),
                numberParser.decimal(item.getTrdeTernRt(), "거래회전율"),
                numberParser.decimal(item.getTrdeAmt(), "거래금액")
        );
    }

    private String marketCode(RankingMarket market) {
        return switch (market) {
            case ALL -> "000";
            case KOSPI -> "001";
            case KOSDAQ -> "101";
        };
    }

    private String sortCode(RankingSort sort) {
        return switch (sort) {
            case VOLUME -> "1";
            case TURNOVER -> "2";
            case TRADING_AMOUNT -> "3";
        };
    }

    private String exchangeCode(TradingExchange exchange) {
        return switch (exchange) {
            case KRX -> "1";
            case NXT -> "2";
            case CONSOLIDATED -> "3";
        };
    }
}
