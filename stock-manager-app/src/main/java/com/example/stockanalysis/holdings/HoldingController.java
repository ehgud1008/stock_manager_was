package com.example.stockanalysis.holdings;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.stockanalysis.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@Tag(name = "보유종목")
@RestController
@RequestMapping("/api/v1/holdings")
public class HoldingController {
    private final HoldingService service;
    public HoldingController(HoldingService service){this.service=service;}
    @Operation(summary = "보유종목 목록 조회")
    @GetMapping public ApiResponse<List<HoldingModels.Holding>> list(){return ApiResponse.success(service.list());}
    @Operation(summary = "보유종목 등록 및 수정", description = "신규 등록은 expectedRevision=0, 수정은 조회한 revision을 전달합니다. 수량 0은 명시적 미보유입니다. 수량이 양수이면 평균단가가 필요합니다. 잔고 기준일은 미래 날짜일 수 없습니다.")
    @PutMapping("/{code}") public ApiResponse<HoldingModels.Holding> update(@PathVariable String code,@Valid @RequestBody HoldingModels.Edit edit){return ApiResponse.success(service.update(code,edit));}
}
