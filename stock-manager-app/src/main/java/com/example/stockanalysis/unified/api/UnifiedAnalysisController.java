package com.example.stockanalysis.unified.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.stockanalysis.shared.api.ApiResponse;
import com.example.stockanalysis.unified.application.UnifiedAnalysisResponse;
import com.example.stockanalysis.unified.application.UnifiedAnalysisService;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "종합분석")
@RestController
@RequestMapping("/api/v1/stocks/{stockCode}/unified-analysis")
public class UnifiedAnalysisController {
    private final UnifiedAnalysisService service;

    public UnifiedAnalysisController(UnifiedAnalysisService service) {
        this.service = service;
    }

    @Operation(summary = "개별 종목 종합분석 실행", description = "종목분석과 EMA 스크리너를 동일 일봉으로 실행한 결과를 반환합니다.")
    @PostMapping
    public ApiResponse<UnifiedAnalysisResponse> execute(@PathVariable @Pattern(regexp = "[0-9]{6}") String stockCode) {
        return ApiResponse.success("종합분석이 완료되었습니다.", service.execute(stockCode));
    }
}
