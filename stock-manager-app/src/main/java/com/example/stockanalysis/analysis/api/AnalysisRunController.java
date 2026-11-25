package com.example.stockanalysis.analysis.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.stockanalysis.analysis.application.StockAnalysisService;
import com.example.stockanalysis.shared.api.ApiResponse;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "종목분석")
@RestController
@RequestMapping("/api/v1/analysis-runs")
public class AnalysisRunController {

    private final StockAnalysisService service;

    public AnalysisRunController(StockAnalysisService service) {
        this.service = service;
    }

    @Operation(summary = "분석 실행 결과 조회", description = "숫자형 analysisRunId로 저장된 종목분석 결과를 조회합니다.")
    @GetMapping("/{analysisRunId}")
    public ApiResponse<StockAnalysisResponse> getRun(
            @PathVariable @Positive long analysisRunId
    ) {
        return ApiResponse.success(StockAnalysisResponse.from(service.getRun(analysisRunId)));
    }
}
