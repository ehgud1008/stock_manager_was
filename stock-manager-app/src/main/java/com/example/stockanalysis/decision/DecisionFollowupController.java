package com.example.stockanalysis.decision;

import com.example.stockanalysis.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name="AI 통합 매매 판단 및 성과")
@RequestMapping("/api/v1/trade-decisions")
public class DecisionFollowupController {
    private final DecisionPerformanceStatistics statistics;
    private final TakeProfitPlanService takeProfit;

    public DecisionFollowupController(DecisionPerformanceStatistics statistics, TakeProfitPlanService takeProfit) {
        this.statistics = statistics;
        this.takeProfit = takeProfit;
    }

    @Operation(summary="동일 기회를 한 번만 포함한 통합 판단 모의 성과 조회")
    @GetMapping("/statistics")
    public ApiResponse<DecisionPerformanceStatistics.Report> statistics(
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required=false) String stock,
            @RequestParam(required=false) String policy,
            @RequestParam(required=false) String model,
            @RequestParam(defaultValue="STOCK") DecisionPerformanceStatistics.Group groupBy) {
        return ApiResponse.success(statistics.get(from,to,stock,policy,model,groupBy));
    }

    @Operation(summary="사용자 비율로 목표별 정수 익절 수량 검토")
    @PostMapping("/{id}/take-profit-preview")
    public ResponseEntity<ApiResponse<TakeProfitPlanService.Preview>> takeProfit(@PathVariable String id,
            @Valid @RequestBody TakeProfitPlanService.Request request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(takeProfit.preview(id,request)));
    }
}
