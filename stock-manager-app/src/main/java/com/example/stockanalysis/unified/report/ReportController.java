package com.example.stockanalysis.unified.report;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.example.stockanalysis.unified.report.ReportModels.*;
import com.example.stockanalysis.shared.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "장 마감 리포트")
@RestController
@RequestMapping("/api/v1/unified-analysis/runs/{id}/report")
public class ReportController {
    private final DailyReportService service;
    public ReportController(DailyReportService service){this.service=service;}
    @Operation(summary = "실행별 장 마감 리포트 조회")
    @GetMapping public ApiResponse<View> get(@PathVariable String id){return ApiResponse.success(service.get(id));}
    @Operation(summary = "장 마감 리포트 생성 요청", description = "비동기 생성을 요청합니다. 기능 설정에 따라 텔레그램 발송이 수반될 수 있습니다.")
    @PostMapping @ResponseStatus(HttpStatus.ACCEPTED) public ApiResponse<View> create(@PathVariable String id){return ApiResponse.success(service.request(id));}
    @Operation(summary = "리포트 종목별 판단 근거 조회")
    @GetMapping("/stocks/{code}") public ApiResponse<StockDecision> evidence(@PathVariable String id,@PathVariable String code){return ApiResponse.success(service.evidence(id,code));}
}
