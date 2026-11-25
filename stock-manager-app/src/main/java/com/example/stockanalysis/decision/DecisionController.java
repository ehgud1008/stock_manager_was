package com.example.stockanalysis.decision;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;

import static com.example.stockanalysis.decision.DecisionModels.*;
import com.example.stockanalysis.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "저장 분석 및 매매 판단")
@RestController
@RequestMapping("/api/v1")
public class DecisionController {
    private final DecisionService service;
    public DecisionController(DecisionService service) { this.service=service; }
    @Operation(summary = "매매 판단 기능 지원 상태 조회")
    @GetMapping("/trade-decisions/capabilities") public ApiResponse<Map<String,Object>> capabilities() { return ApiResponse.success(service.capabilities()); }
    @Operation(summary = "분석 스냅샷 생성", description = "COLLECT 또는 UNIFIED_RUN_ITEM 원본으로 스냅샷을 요청합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청에 해당하는 작업이 종료됨 (SUCCEEDED 또는 FAILED)", useReturnTypeSchema = true, headers = @Header(name = "Location", description = "작업 조회 경로", schema = @Schema(type = "string"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "작업 접수 또는 진행 중. Location 헤더의 조회 경로로 상태를 확인합니다.", useReturnTypeSchema = true, headers = @Header(name = "Location", description = "작업 조회 경로", schema = @Schema(type = "string")))
    })
    @PostMapping("/stocks/{code}/analysis-snapshots") public ResponseEntity<ApiResponse<View>> snapshot(@PathVariable String code,@Valid @RequestBody SnapshotRequest request,@RequestHeader(value="Idempotency-Key",required=false) String key) { return accepted(service.snapshot(code,request,key)); }
    @Operation(summary = "분석 스냅샷 조회")
    @GetMapping("/analysis-snapshots/{id}") public ApiResponse<View> snapshot(@PathVariable String id) { return ApiResponse.success(service.get(id,"SNAPSHOT")); }
    @Operation(summary = "종합분석에서 가져온 스냅샷 조회", description = "종목코드와 sourceRunId에 해당하는 저장 스냅샷을 조회합니다.")
    @GetMapping("/stocks/{code}/analysis-snapshots") public ApiResponse<View> importedSnapshot(@PathVariable String code,@RequestParam String sourceRunId) { return ApiResponse.success(service.importedSnapshot(code,sourceRunId)); }
    @Operation(summary = "스냅샷 기반 매매 판단 요청")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청에 해당하는 작업이 종료됨 (SUCCEEDED 또는 FAILED)", useReturnTypeSchema = true, headers = @Header(name = "Location", description = "작업 조회 경로", schema = @Schema(type = "string"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "작업 접수 또는 진행 중. Location 헤더의 조회 경로로 상태를 확인합니다.", useReturnTypeSchema = true, headers = @Header(name = "Location", description = "작업 조회 경로", schema = @Schema(type = "string")))
    })
    @PostMapping("/analysis-snapshots/{id}/decisions") public ResponseEntity<ApiResponse<View>> decision(@PathVariable String id,@Valid @RequestBody ReviewRequest request,@RequestHeader(value="Idempotency-Key",required=false) String key) { return accepted(service.decision(id,request,key)); }
    @Operation(summary = "스냅샷 기반 뉴스 인사이트 요청", description = "본문은 생략할 수 있습니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청에 해당하는 작업이 종료됨 (SUCCEEDED 또는 FAILED)", useReturnTypeSchema = true, headers = @Header(name = "Location", description = "작업 조회 경로", schema = @Schema(type = "string"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "작업 접수 또는 진행 중. Location 헤더의 조회 경로로 상태를 확인합니다.", useReturnTypeSchema = true, headers = @Header(name = "Location", description = "작업 조회 경로", schema = @Schema(type = "string")))
    })
    @PostMapping("/analysis-snapshots/{id}/news-insights") public ResponseEntity<ApiResponse<View>> insights(@PathVariable String id,@RequestBody(required=false) DecisionService.InsightRequest request,@RequestHeader(value="Idempotency-Key",required=false) String key) { return accepted(service.insights(id,request,key)); }
    @Operation(summary = "매매 판단 조회")
    @GetMapping("/trade-decisions/{id}") public ApiResponse<View> decision(@PathVariable String id) { return ApiResponse.success(service.get(id,"DECISION")); }
    @Operation(summary = "종목별 매매 판단 이력 조회")
    @GetMapping("/stocks/{code}/trade-decisions") public ApiResponse<History> history(@PathVariable String code,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) String snapshotId) { return ApiResponse.success(service.history(code,page,size,snapshotId)); }
    @Operation(summary = "매매 판단 재시도")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청에 해당하는 작업이 종료됨 (SUCCEEDED 또는 FAILED)", useReturnTypeSchema = true, headers = @Header(name = "Location", description = "작업 조회 경로", schema = @Schema(type = "string"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "작업 접수 또는 진행 중. Location 헤더의 조회 경로로 상태를 확인합니다.", useReturnTypeSchema = true, headers = @Header(name = "Location", description = "작업 조회 경로", schema = @Schema(type = "string")))
    })
    @PostMapping("/trade-decisions/{id}/retry") public ResponseEntity<ApiResponse<View>> retry(@PathVariable String id,@RequestHeader(value="Idempotency-Key",required=false) String key) { service.get(id,"DECISION"); return accepted(service.retry(id,key)); }
    @Operation(summary = "분석 스냅샷 재시도")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청에 해당하는 작업이 종료됨 (SUCCEEDED 또는 FAILED)", useReturnTypeSchema = true, headers = @Header(name = "Location", description = "작업 조회 경로", schema = @Schema(type = "string"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "작업 접수 또는 진행 중. Location 헤더의 조회 경로로 상태를 확인합니다.", useReturnTypeSchema = true, headers = @Header(name = "Location", description = "작업 조회 경로", schema = @Schema(type = "string")))
    })
    @PostMapping("/analysis-snapshots/{id}/retry") public ResponseEntity<ApiResponse<View>> retrySnapshot(@PathVariable String id,@RequestHeader(value="Idempotency-Key",required=false) String key) { service.get(id,"SNAPSHOT"); return accepted(service.retry(id,key)); }
    @Operation(summary = "매매 판단 개정 요청")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청에 해당하는 작업이 종료됨 (SUCCEEDED 또는 FAILED)", useReturnTypeSchema = true, headers = @Header(name = "Location", description = "작업 조회 경로", schema = @Schema(type = "string"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "작업 접수 또는 진행 중. Location 헤더의 조회 경로로 상태를 확인합니다.", useReturnTypeSchema = true, headers = @Header(name = "Location", description = "작업 조회 경로", schema = @Schema(type = "string")))
    })
    @PostMapping("/trade-decisions/{id}/revisions") public ResponseEntity<ApiResponse<View>> revision(@PathVariable String id,@RequestHeader(value="Idempotency-Key",required=false) String key) { return accepted(service.revision(id,key)); }
    private ResponseEntity<ApiResponse<View>> accepted(View view) {
        boolean done=java.util.Set.of("SUCCEEDED","FAILED").contains(view.status());
        String path="SNAPSHOT".equals(view.kind())?"analysis-snapshots":"trade-decisions";
        return ResponseEntity.status(done?200:202).location(URI.create("/api/v1/"+path+"/"+view.id())).body(ApiResponse.success(view));
    }
}
