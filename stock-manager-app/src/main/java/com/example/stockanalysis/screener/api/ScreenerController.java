package com.example.stockanalysis.screener.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.stockanalysis.engine.screener.*;
import com.example.stockanalysis.screener.application.*;
import com.example.stockanalysis.screener.domain.*;
import com.example.stockanalysis.shared.api.ApiResponse;
import com.example.stockanalysis.shared.exception.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "EMA 스크리너")
@RestController
@RequestMapping("/api/v1/screener")
public class ScreenerController {
    private final ScreenerService service;
    public ScreenerController(ScreenerService service) { this.service = service; }
    @Operation(summary = "스크리너 실행 등록", description = "비동기 실행을 등록합니다. Idempotency-Key는 선택 헤더입니다.")
    @PostMapping("/runs") @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<ScreenerRun> run(@Valid @RequestBody ScreenerRunRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) { return ApiResponse.success(service.submit(request, key)); }
    @Operation(summary = "스크리너 실행 상태 조회")
    @GetMapping("/runs/{id}")
    public ApiResponse<ScreenerRun> run(@PathVariable String id) { return ApiResponse.success(service.get(id)); }
    @Operation(summary = "스크리너 실행 재개")
    @PostMapping("/runs/{id}/resume") @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<ScreenerRun> resume(@PathVariable String id) { return ApiResponse.success(service.resume(id)); }
    @Operation(summary = "스크리너 실행 복구")
    @PostMapping("/runs/{id}/recover")
    public ApiResponse<ScreenerRun> recover(@PathVariable String id) { return ApiResponse.success(service.recover(id)); }
    @Operation(summary = "스크리너 결과 검색", description = "GET 쿼리로 검색합니다. 기본 mode=SWING, market=ALL, page=0, size=50이며 size는 1~100입니다.")
    @GetMapping
    public ApiResponse<ScreenerService.ScreenerPage> results(@org.springdoc.core.annotations.ParameterObject @ModelAttribute ScreenerQuery query) { return ApiResponse.success(service.query(query)); }
    // Read-only search with a JSON body avoids URL limits for watchlists. GET remains available.
    @Operation(summary = "스크리너 결과 본문 검색", description = "GET 검색과 같은 조회이며 JSON 본문으로 최대 1000개 관심종목 코드를 전달할 수 있습니다. 데이터를 변경하지 않습니다.")
    @PostMapping("/search")
    public ApiResponse<ScreenerService.ScreenerPage> search(@RequestBody ScreenerQuery query) { return ApiResponse.success(service.query(query)); }
    @Operation(summary = "스크리너 종목 결과 조회")
    @GetMapping("/runs/{id}/stocks/{code}")
    public ApiResponse<ScreenerItem> item(@PathVariable String id, @PathVariable String code) { return ApiResponse.success(service.item(id, code)); }
    @Operation(summary = "저장 입력으로 스크리너 재현", description = "저장된 종목 입력을 사용해 스크리너 계산 결과를 재현합니다.")
    @PostMapping("/runs/{id}/stocks/{code}/replay")
    public ApiResponse<ScreenerResult> replay(@PathVariable String id, @PathVariable String code) { return ApiResponse.success(service.replay(id, code)); }
    @Operation(summary = "입력 데이터로 스크리너 계산", description = "전달한 입력을 평가합니다.")
    @PostMapping("/evaluate")
    public ApiResponse<ScreenerResult> evaluate(@RequestBody ScreenerInput input) { return ApiResponse.success(service.evaluate(input)); }
    @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> invalid(IllegalArgumentException exception) { return ApiResponse.failure(ErrorCode.INVALID_REQUEST, exception.getMessage()); }
}
