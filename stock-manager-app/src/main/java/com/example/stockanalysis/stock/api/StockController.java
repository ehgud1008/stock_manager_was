package com.example.stockanalysis.stock.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.stockanalysis.shared.api.ApiResponse;
import com.example.stockanalysis.stock.application.StockQueryService;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "종목 기본정보")
@RestController
@RequestMapping("/api/v1/stocks")
public class StockController {

    private final StockQueryService stockQueryService;

    public StockController(StockQueryService stockQueryService) {
        this.stockQueryService = stockQueryService;
    }

    @Operation(summary = "종목 기본정보 조회", description = "숫자 6자리 종목코드로 기본정보를 조회합니다.")
    @GetMapping("/{stockCode}")
    public ApiResponse<StockProfileResponse> getStockProfile(
            @PathVariable
            @Pattern(regexp = "\\d{6}", message = "종목코드는 숫자 6자리여야 합니다.")
            String stockCode
    ) {
        return ApiResponse.success(
                StockProfileResponse.from(stockQueryService.getStockProfile(stockCode))
        );
    }
}
