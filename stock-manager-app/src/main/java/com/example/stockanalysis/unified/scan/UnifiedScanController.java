package com.example.stockanalysis.unified.scan;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.example.stockanalysis.unified.scan.UnifiedScanModels.*;
import com.example.stockanalysis.shared.api.ApiResponse;
import com.example.stockanalysis.analysis.api.StockAnalysisResponse;
import com.example.stockanalysis.analysis.application.StockAnalysisService;
import com.example.stockanalysis.shared.exception.ErrorCode;
import com.example.stockanalysis.unified.application.UnifiedAnalysisResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "전체 종합분석")
@RestController
@RequestMapping("/api/v1/unified-analysis/runs")
public class UnifiedScanController {
    private final UnifiedScanService service;
    private final StockAnalysisService stockAnalysis;

    public UnifiedScanController(UnifiedScanService service, StockAnalysisService stockAnalysis) {
        this.service = service;
        this.stockAnalysis = stockAnalysis;
    }
    @ExceptionHandler({IllegalArgumentException.class, org.springframework.web.bind.MissingRequestHeaderException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> invalid(Exception e) { return ApiResponse.failure(ErrorCode.INVALID_REQUEST, e.getMessage()); }
    @Operation(summary = "전체 종합분석 등록", description = "시장별 비동기 종합분석을 등록합니다. Idempotency-Key 헤더가 필요합니다.")
    @PostMapping @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Run> start(@RequestBody Request request, @RequestHeader("Idempotency-Key") String key) { return ApiResponse.success(service.submit(request, key)); }
    @Operation(summary = "최근 전체 종합분석 목록 조회")
    @GetMapping public ApiResponse<List<Run>> recent(@RequestParam(defaultValue = "false") boolean archived) {
        return ApiResponse.success(archived ? service.recentArchived() : service.recent());
    }
    @Operation(summary = "통합 전 실행의 저장된 종목 상세 조회")
    @GetMapping("/archive/{id}/items/{code}")
    public ApiResponse<StockAnalysisResponse> archivedDetail(@PathVariable String id, @PathVariable String code) {
        return ApiResponse.success(StockAnalysisResponse.from(stockAnalysis.getRun(service.archivedAnalysisId(id, code))));
    }
    @Operation(summary = "전체 종합분석 진행 상태 조회")
    @GetMapping("/{id}") public ApiResponse<Run> get(@PathVariable String id) { return ApiResponse.success(service.get(id)); }
    @Operation(summary = "전체 종합분석 재개")
    @PostMapping("/{id}/resume") @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Run> resume(@PathVariable String id) { return ApiResponse.success(service.resume(id)); }
    @Operation(summary = "전체 종합분석 복구")
    @PostMapping("/{id}/recover") public ApiResponse<Run> recover(@PathVariable String id) { return ApiResponse.success(service.recover(id)); }
    @Operation(summary = "종합분석 실행의 종목 상세 조회")
    @GetMapping("/{id}/items/{code}") public ApiResponse<UnifiedAnalysisResponse> detail(@PathVariable String id, @PathVariable String code) { return ApiResponse.success(service.detail(id, code)); }
    @Operation(summary = "종목의 저장된 날짜별 점수 이력")
    @GetMapping("/{id}/items/{code}/score-history")
    public ApiResponse<List<UnifiedScanStore.ScoreHistory>> scoreHistory(@PathVariable String id, @PathVariable String code) {
        return ApiResponse.success(service.scoreHistory(id, code));
    }
    @Operation(summary = "전체 종합분석 결과 검색", description = "단계·전환·신호·점수·거래량으로 필터링합니다. size는 1~100, stage는 1~6, 점수는 0~100입니다. 거래대금 minTradingAmountEok의 단위는 억원입니다.")
    @GetMapping("/{id}/items") public ApiResponse<Page> items(@PathVariable String id,
            @RequestParam(required = false) String search, @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer stage, @RequestParam(required = false) String transition,
            @RequestParam(required = false) Boolean allSlopesUp, @RequestParam(required = false) Integer maxBarsSinceTransition,
            @RequestParam(required = false) String signal, @RequestParam(required = false) String quality,
            @RequestParam(required = false) BigDecimal minScore, @RequestParam(required = false) BigDecimal maxScore,
            @RequestParam(required = false) BigDecimal minRewardRisk, @RequestParam(defaultValue = "totalScore") String sort,
            @RequestParam(defaultValue = "false") boolean ascending, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String entryStatus,
            @RequestParam(required = false) String market, @RequestParam(required = false) Long minVolume,
            @RequestParam(required = false) BigDecimal minTradingAmountEok, @RequestParam(required = false) String setup,
            @RequestParam(defaultValue = "false") boolean archived) {
        var query = new Query(search, status, stage, transition, allSlopesUp, maxBarsSinceTransition,
                signal, quality, minScore, maxScore, minRewardRisk, sort, ascending, page, size, entryStatus, market, minVolume, minTradingAmountEok, setup);
        return ApiResponse.success(archived ? service.queryArchived(id, query) : service.query(id, query));
    }
}
