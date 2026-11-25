package com.example.stockanalysis.analysis.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.stockanalysis.analysis.application.StockAnalysisService;
import com.example.stockanalysis.shared.api.ApiResponse;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "종목 시세")
@RestController
@RequestMapping("/api/v1/stocks/{stockCode}/prices")
public class StockPriceController {

    private final StockAnalysisService service;

    public StockPriceController(StockAnalysisService service) {
        this.service = service;
    }

    @Operation(summary = "종목 차트 가격 조회", description = "period는 DAY, WEEK, MONTH, MINUTE입니다. 분봉 interval은 1, 3, 5, 10, 15, 30, 45, 60을 지원합니다. from/to는 선택 날짜 필터입니다.")
    @GetMapping
    public ApiResponse<List<StockPriceResponse>> prices(
            @PathVariable @Pattern(regexp = "\\d{6}") String stockCode,
            @RequestParam(defaultValue = "DAY") String period,
            @RequestParam(defaultValue = "1") int interval,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return ApiResponse.success(service.getPrices(stockCode, period, interval, from, to).stream()
                .map(StockPriceResponse::from)
                .toList());
    }
}
