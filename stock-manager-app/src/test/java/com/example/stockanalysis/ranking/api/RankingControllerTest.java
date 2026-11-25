package com.example.stockanalysis.ranking.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.stockanalysis.ranking.application.RankingQueryService;
import com.example.stockanalysis.ranking.domain.RankingMarket;
import com.example.stockanalysis.ranking.domain.RankingSort;
import com.example.stockanalysis.ranking.domain.TradingExchange;
import com.example.stockanalysis.ranking.domain.TradingVolumeRankedStock;
import com.example.stockanalysis.ranking.domain.TradingVolumeRankingCriteria;
import com.example.stockanalysis.ranking.domain.TradingVolumeRankingPage;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RankingController.class)
class RankingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RankingQueryService rankingQueryService;

    @Test
    void convertsBusinessQueryParametersWithoutStockCode() throws Exception {
        TradingVolumeRankingCriteria criteria = new TradingVolumeRankingCriteria(
                RankingMarket.KOSPI,
                RankingSort.TRADING_AMOUNT,
                false,
                TradingExchange.KRX,
                "next-page"
        );
        when(rankingQueryService.getTradingVolumeRanking(criteria))
                .thenReturn(new TradingVolumeRankingPage(
                        List.of(rankedStock()),
                        true,
                        "following-page"
                ));

        mockMvc.perform(get("/api/v1/rankings/trading-volume")
                        .queryParam("market", "KOSPI")
                        .queryParam("sort", "TRADING_AMOUNT")
                        .queryParam("exchange", "KRX")
                        .queryParam("pageToken", "next-page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].rank").value(1))
                .andExpect(jsonPath("$.data.items[0].stockCode").value("005930"))
                .andExpect(jsonPath("$.data.items[0].tradingVolume").value(34954641))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.nextPageToken").value("following-page"));

        verify(rankingQueryService).getTradingVolumeRanking(criteria);
    }

    @Test
    void rejectsUnknownMarketAsValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/rankings/trading-volume")
                        .queryParam("market", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private TradingVolumeRankedStock rankedStock() {
        return new TradingVolumeRankedStock(
                1,
                "005930",
                "삼성전자",
                new BigDecimal("152000"),
                "5",
                new BigDecimal("-100"),
                new BigDecimal("-0.07"),
                new BigDecimal("34954641"),
                new BigDecimal("155.13"),
                new BigDecimal("48.21"),
                new BigDecimal("5308092")
        );
    }
}
