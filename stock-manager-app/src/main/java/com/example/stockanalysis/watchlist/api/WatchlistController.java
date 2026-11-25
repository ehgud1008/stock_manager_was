package com.example.stockanalysis.watchlist.api;

import com.example.stockanalysis.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.stockanalysis.watchlist.application.WatchlistQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관심종목")
@RestController
@RequestMapping("/api/v1/watchlists")
public class WatchlistController {

    private final WatchlistQueryService watchlistQueryService;

    public WatchlistController(WatchlistQueryService watchlistQueryService) {
        this.watchlistQueryService = watchlistQueryService;
    }

    @Operation(summary = "관심종목 그룹 조회", description = "관심종목 그룹과 그룹별 종목 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<WatchlistGroupResponse>> getGroups() {
        List<WatchlistGroupResponse> response = watchlistQueryService.getGroups()
                .stream()
                .map(WatchlistGroupResponse::from)
                .toList();
        return ApiResponse.success(response);
    }
}
