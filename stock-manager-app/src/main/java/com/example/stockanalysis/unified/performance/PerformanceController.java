package com.example.stockanalysis.unified.performance;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.stockanalysis.shared.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "선정 이후 성과")
@RestController
@RequestMapping("/api/v1/unified-analysis/runs/{id}/performance")
public class PerformanceController {
    private final PerformanceService service;
    public PerformanceController(PerformanceService service){this.service=service;}
    @Operation(summary = "실행별 성과 검증 조회")
    @GetMapping public ApiResponse<PerformanceModels.View> get(@PathVariable String id){return ApiResponse.success(service.get(id));}
    @Operation(summary = "성과 검증 실행 요청", description = "선정 이후 가격 관측과 모의매매 성과 평가를 비동기로 요청합니다.")
    @PostMapping @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<PerformanceModels.View> request(@PathVariable String id){return ApiResponse.success(service.request(id));}
}
