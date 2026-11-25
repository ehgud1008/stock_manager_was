package com.example.stockanalysis.analysis.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.stockanalysis.analysis.application.StockAnalysisService;
import com.example.stockanalysis.engine.model.AnalysisMode;
import com.example.stockanalysis.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "종목분석")
@RestController
@RequestMapping("/api/v1/stocks/{stockCode}/analysis")
public class StockAnalysisController {

    private final StockAnalysisService service;

    public StockAnalysisController(StockAnalysisService service) {
        this.service = service;
    }

    @Operation(summary = "종목분석 실행", description = "요청한 모드로 분석을 완료한 결과를 반환합니다. 현재 구현은 동기 처리 후 HTTP 202를 반환합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<StockAnalysisResponse> execute(
            @PathVariable @Pattern(regexp = "\\d{6}") String stockCode,
            @Valid @RequestBody AnalysisExecutionRequest request
    ) {
        return ApiResponse.success(
                "종목분석이 완료되었습니다.",
                StockAnalysisResponse.from(service.execute(stockCode, request.toCommand()))
        );
    }

    @Operation(summary = "최근 종목분석 조회", description = "모드별 최신 저장 결과를 조회합니다. 기본 모드는 SWING입니다.")
    @GetMapping("/latest")
    public ApiResponse<StockAnalysisResponse> latest(
            @PathVariable @Pattern(regexp = "\\d{6}") String stockCode,
            @RequestParam(defaultValue = "SWING") AnalysisMode mode
    ) {
        return ApiResponse.success(StockAnalysisResponse.from(service.getLatest(stockCode, mode)));
    }

    @Operation(summary = "종목분석 이력 조회", description = "모드별 분석 이력을 페이지로 조회합니다.")
    @GetMapping("/history")
    public ApiResponse<AnalysisHistoryResponse> history(
            @PathVariable @Pattern(regexp = "\\d{6}") String stockCode,
            @RequestParam(defaultValue = "SWING") AnalysisMode mode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(
                AnalysisHistoryResponse.from(service.getHistory(stockCode, mode, page, size))
        );
    }
}
