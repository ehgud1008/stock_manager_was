package com.example.stockanalysis.decision;

import com.example.stockanalysis.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import java.util.Map;

@RestController
@Tag(name="AI 통합 매매 판단 및 성과")
@RequestMapping("/api/v1")
public class IntegratedDecisionController {
    private final IntegratedDecisionService integrated;private final DecisionService decisions;private final DecisionOutcomeService outcomes;
    public IntegratedDecisionController(IntegratedDecisionService integrated,DecisionService decisions,DecisionOutcomeService outcomes){this.integrated=integrated;this.decisions=decisions;this.outcomes=outcomes;}
    @Operation(summary="고정된 분석·완료 리서치·현재 보유정보로 통합 판단 생성")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="기존 완료 작업 반환",useReturnTypeSchema=true),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="202",description="비동기 통합 판단 접수",useReturnTypeSchema=true,
            headers=@Header(name="Location",description="기존 작업 조회 경로",schema=@Schema(type="string")))
    })
    @PostMapping("/analysis-snapshots/{id}/integrated-decisions")
    public ResponseEntity<ApiResponse<DecisionModels.View>> create(@PathVariable String id,@RequestBody(required=false) IntegratedDecisionService.Request request,
            @RequestHeader(value="Idempotency-Key",required=false) String key) {
        var job=integrated.create(id,request,key,null);var view=decisions.get(job.id(),"DECISION");
        return ResponseEntity.status(java.util.Set.of("SUCCEEDED","FAILED").contains(job.status())?200:202)
                .location(URI.create("/api/v1/trade-decisions/"+job.id())).body(ApiResponse.success(view));
    }
    @Operation(summary="통합 판단의 고정 계획과 후속 모의 성과 조회")
    @GetMapping("/trade-decisions/{id}/outcome")
    public ApiResponse<Map<String,Object>> outcome(@PathVariable String id){return ApiResponse.success(outcomes.get(id));}
    @Operation(summary="동일 조건의 엔진 기준과 AI 최종 판단 성과 비교")
    @GetMapping("/trade-decisions/{id}/comparison")
    public ApiResponse<Map<String,Object>> comparison(@PathVariable String id){return ApiResponse.success(outcomes.comparison(id));}
    @Operation(summary="저장된 판단을 변경하지 않고 현재 거래일·신호 기준 유효 상태 조회")
    @GetMapping("/trade-decisions/{id}/validity")
    public ApiResponse<com.fasterxml.jackson.databind.JsonNode> validity(@PathVariable String id){return ApiResponse.success(integrated.currentValidity(id));}
}
