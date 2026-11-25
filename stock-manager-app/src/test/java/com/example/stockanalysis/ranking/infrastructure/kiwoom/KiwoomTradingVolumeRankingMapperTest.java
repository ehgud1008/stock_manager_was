package com.example.stockanalysis.ranking.infrastructure.kiwoom;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.stockanalysis.kiwoom.client.KiwoomContinuation;
import com.example.stockanalysis.kiwoom.client.KiwoomNumberParser;
import com.example.stockanalysis.kiwoom.client.KiwoomResponse;
import com.example.stockanalysis.kiwoom.contract.tr.ka10030.Ka10030Request;
import com.example.stockanalysis.kiwoom.contract.tr.ka10030.Ka10030Response;
import com.example.stockanalysis.kiwoom.contract.tr.ka10030.Ka10030Response.TdyTrdeQtyUpperItem;
import com.example.stockanalysis.ranking.domain.RankingMarket;
import com.example.stockanalysis.ranking.domain.RankingSort;
import com.example.stockanalysis.ranking.domain.TradingExchange;
import com.example.stockanalysis.ranking.domain.TradingVolumeRankingCriteria;
import com.example.stockanalysis.ranking.domain.TradingVolumeRankingPage;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class KiwoomTradingVolumeRankingMapperTest {

    private final KiwoomTradingVolumeRankingMapper mapper =
            new KiwoomTradingVolumeRankingMapper(new KiwoomNumberParser());

    @Test
    void mapsBusinessCriteriaToKiwoomCodes() {
        Ka10030Request request = mapper.toRequest(new TradingVolumeRankingCriteria(
                RankingMarket.KOSDAQ,
                RankingSort.TURNOVER,
                false,
                TradingExchange.CONSOLIDATED,
                null
        ));

        assertThat(request.getMrktTp()).isEqualTo("101");
        assertThat(request.getSortTp()).isEqualTo("2");
        assertThat(request.getMangStkIncls()).isEqualTo("1");
        assertThat(request.getStexTp()).isEqualTo("3");
    }

    @Test
    void mapsOrderedResultAndContinuationToPage() {
        Ka10030Response raw = Ka10030Response.builder()
                .tdyTrdeQtyUpper(List.of(rawItem()))
                .returnCode(0)
                .returnMsg("정상")
                .build();

        TradingVolumeRankingPage page = mapper.toDomain(new KiwoomResponse<>(
                raw,
                new KiwoomContinuation(true, "next-key")
        ));

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).rank()).isEqualTo(1);
        assertThat(page.items().get(0).currentPrice())
                .isEqualByComparingTo(new BigDecimal("152000"));
        assertThat(page.items().get(0).changeRate())
                .isEqualByComparingTo(new BigDecimal("-0.07"));
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextPageToken()).isEqualTo("next-key");
    }

    private TdyTrdeQtyUpperItem rawItem() {
        return TdyTrdeQtyUpperItem.builder()
                .stkCd("005930")
                .stkNm("삼성전자")
                .curPrc("-152000")
                .predPreSig("5")
                .predPre("-100")
                .fluRt("-0.07")
                .trdeQty("34954641")
                .predRt("+155.13")
                .trdeTernRt("+48.21")
                .trdeAmt("5308092")
                .opmrTrdeQty("0")
                .opmrPredRt("0.00")
                .opmrTrdeRt("+0.00")
                .opmrTrdeAmt("0")
                .afMkrtTrdeQty("0")
                .afMkrtPredRt("0.00")
                .afMkrtTrdeRt("+0.00")
                .afMkrtTrdeAmt("0")
                .bfMkrtTrdeQty("0")
                .bfMkrtPredRt("0.00")
                .bfMkrtTrdeRt("+0.00")
                .bfMkrtTrdeAmt("0")
                .build();
    }
}
