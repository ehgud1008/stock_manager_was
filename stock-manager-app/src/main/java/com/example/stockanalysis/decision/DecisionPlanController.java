package com.example.stockanalysis.decision;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.example.stockanalysis.decision.DecisionModels.*;
import com.example.stockanalysis.shared.api.ApiResponse;
import com.example.stockanalysis.shared.exception.*;
import com.example.stockanalysis.unified.application.UnifiedAnalysisResponse;
import com.example.stockanalysis.unified.scan.UnifiedScanStore;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** Read-only calculations: never queues a snapshot, collects prices, or calls AI. */
@Tag(name = "저장 분석 및 매매 판단")
@RestController
@RequestMapping("/api/v1")
public class DecisionPlanController {
    private final DecisionSettings settings;private final DecisionService service;private final UnifiedScanStore scans;
    private final DecisionPolicy policy;private final DecisionJson json;
    public DecisionPlanController(DecisionSettings settings,DecisionService service,UnifiedScanStore scans,DecisionPolicy policy,DecisionJson json) {
        this.settings=settings;this.service=service;this.scans=scans;this.policy=policy;this.json=json;
    }
    @Operation(summary = "저장 스냅샷의 가격 계획 계산", description = "완료된 스냅샷으로 가격 계획만 계산합니다. 분석 수집이나 AI 호출은 수행하지 않습니다.")
    @PostMapping("/analysis-snapshots/{id}/decision-plan")
    public ApiResponse<JsonNode> snapshot(@PathVariable String id,@Valid @RequestBody ReviewRequest context) {
        var snapshot=service.get(id,"SNAPSHOT");
        if(!"SUCCEEDED".equals(snapshot.status())) throw new BusinessException(ErrorCode.DECISION_CONFLICT,"저장 분석 완료 후 가격 계획을 확인하세요.");
        return plan(json.convert(snapshot.result().path("analysis"),UnifiedAnalysisResponse.class),context);
    }
    @Operation(summary = "종합분석 결과의 가격 계획 계산", description = "sourceRunId에 저장된 종목 결과로 가격 계획만 계산합니다. 분석 수집이나 AI 호출은 수행하지 않습니다.")
    @PostMapping("/stocks/{code}/decision-plan")
    public ApiResponse<JsonNode> source(@PathVariable String code,@RequestParam String sourceRunId,@Valid @RequestBody ReviewRequest context) {
        if(!settings.enabled()) throw new BusinessException(ErrorCode.DECISION_DISABLED);
        if(!code.matches("[0-9]{6}") || !sourceRunId.matches("[A-Za-z0-9-]{1,36}")) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        var data=scans.detail(sourceRunId,code).orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if(!code.equals(data.result().stock().stockCode())) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        return plan(data,context);
    }
    private ApiResponse<JsonNode> plan(UnifiedAnalysisResponse data,ReviewRequest context) {
        DecisionService.validateContext(context,data);return ApiResponse.success(policy.prepare(data,context));
    }
}
