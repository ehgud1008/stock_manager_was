package com.example.stockanalysis.decision;

import com.example.stockanalysis.shared.api.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name="AI 통합 매매 판단 및 성과")
@RequestMapping("/api/v1/trade-decisions")
public class DecisionRecheckController {
    private final DecisionRecheckService service;
    public DecisionRecheckController(DecisionRecheckService service){this.service=service;}
    @Operation(summary="저장 판단과 조회 가격·수동 등록 보유정보·계좌 설정을 별도로 재확인",
            description="AI 호출·주문·잔고 변경·과거 결과 갱신 없이 참고용 비교를 반환합니다. 시세 시각·호가 규칙·가격 기준 일치와 증권사 잔고는 확인 불가입니다.")
    @GetMapping("/{id}/recheck")
    public ResponseEntity<ApiResponse<JsonNode>> get(@PathVariable String id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.get(id)));
    }
}
