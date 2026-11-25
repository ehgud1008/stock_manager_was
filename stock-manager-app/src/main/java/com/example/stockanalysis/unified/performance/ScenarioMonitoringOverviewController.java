package com.example.stockanalysis.unified.performance;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.stockanalysis.shared.api.ApiResponse;
import com.example.stockanalysis.shared.exception.ErrorCode;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "시나리오 모니터링")
@RestController
@RequestMapping("/api/v1/unified-analysis/monitoring")
public class ScenarioMonitoringOverviewController {
    private final ScenarioMonitoringOverviewService service;
    public ScenarioMonitoringOverviewController(ScenarioMonitoringOverviewService service){this.service=service;}
    @Operation(summary = "시나리오 모니터링 목록 조회", description = "기간·검색어·처분 상태로 필터링합니다. deleted=true이면 숨긴 항목을 조회합니다.")
    @GetMapping public ApiResponse<ScenarioMonitoringModels.Overview> list(
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue="") String search,@RequestParam(defaultValue="ALL") String disposition,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,
            @RequestParam(defaultValue="false") boolean deleted){
        return ApiResponse.success(service.list(from,to,search,disposition,page,size,deleted));
    }
    @Operation(summary = "시나리오 모니터링 항목 숨기기", description = "해당 실행의 종목을 목록에서 숨깁니다.")
    @DeleteMapping("/{runId}/stocks/{code}") public ApiResponse<Void> delete(@PathVariable String runId,@PathVariable String code){
        service.visibility(runId,code,true);return ApiResponse.success(null);
    }
    @Operation(summary = "시나리오 모니터링 항목 복원", description = "숨긴 항목을 다시 표시합니다.")
    @PostMapping("/{runId}/stocks/{code}/restore") public ApiResponse<Void> restore(@PathVariable String runId,@PathVariable String code){
        service.visibility(runId,code,false);return ApiResponse.success(null);
    }
    @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> invalid(IllegalArgumentException e){return ApiResponse.failure(ErrorCode.INVALID_REQUEST,e.getMessage());}
}
