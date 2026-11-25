package com.example.stockanalysis.decision;

import com.example.stockanalysis.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name="AI 통합 매매 판단 및 성과")
@RequestMapping("/api/v1/trade-decisions/basket")
public class DecisionBasketController {
    private final DecisionBasketService service;

    public DecisionBasketController(DecisionBasketService service) {
        this.service = service;
    }

    @GetMapping("/candidates")
    @Operation(summary="다종목 합산 검토에 선택할 수 있는 저장 매수 판단 조회")
    public ResponseEntity<ApiResponse<DecisionBasketService.Candidates>> candidates(@RequestParam(defaultValue="0") int page) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.candidates(page)));
    }

    @PostMapping("/preview")
    @Operation(summary="선택한 통합 판단의 매수 금액과 손절 기준 계획 위험 합산",
            description="서버 저장 수량과 비용을 사용한 검토 계산입니다. 주문과 현금 예약 기능을 제공하지 않습니다.")
    public ResponseEntity<ApiResponse<DecisionBasketService.Preview>> preview(@Valid @RequestBody DecisionBasketService.Request request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.preview(request)));
    }
}
