package com.example.stockanalysis.ranking.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.stockanalysis.ranking.application.RankingQueryService;
import com.example.stockanalysis.ranking.domain.RankingMarket;
import com.example.stockanalysis.ranking.domain.RankingSort;
import com.example.stockanalysis.ranking.domain.TradingExchange;
import com.example.stockanalysis.ranking.domain.TradingVolumeRankingCriteria;
import com.example.stockanalysis.shared.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "순위")
@RestController
@RequestMapping("/api/v1/rankings")
public class RankingController {

    private final RankingQueryService rankingQueryService;

    public RankingController(RankingQueryService rankingQueryService) {
        this.rankingQueryService = rankingQueryService;
    }

    @Operation(summary = "거래량 순위 조회", description = "시장·정렬·관리종목 포함 여부·거래소를 선택합니다. 다음 페이지는 응답의 nextPageToken을 요청의 pageToken으로 전달합니다.")
    @GetMapping("/trading-volume")
    public ApiResponse<TradingVolumeRankingResponse> getTradingVolumeRanking(
            @RequestParam(defaultValue = "ALL") RankingMarket market,
            @RequestParam(defaultValue = "VOLUME") RankingSort sort,
            @RequestParam(defaultValue = "false") boolean includeManagedStocks,
            @RequestParam(defaultValue = "CONSOLIDATED") TradingExchange exchange,
            @RequestParam(required = false) String pageToken
    ) {
        TradingVolumeRankingCriteria criteria =
                new TradingVolumeRankingCriteria(
                        market,
                        sort,
                        includeManagedStocks,
                        exchange,
                        pageToken
                );
        return ApiResponse.success(
                TradingVolumeRankingResponse.from(
                        rankingQueryService.getTradingVolumeRanking(criteria)
                )
        );
    }
}
