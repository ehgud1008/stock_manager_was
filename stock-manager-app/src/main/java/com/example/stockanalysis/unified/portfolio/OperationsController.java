package com.example.stockanalysis.unified.portfolio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.stockanalysis.shared.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

@Tag(name = "운영")
@RestController
@RequestMapping("/api/v1/unified-analysis/operations")
public class OperationsController {
    private final OperationsService service;
    public OperationsController(OperationsService service){this.service=service;}
    @Operation(summary = "운영 상태 조회")
    @GetMapping public ApiResponse<OperationsService.Status> status(){return ApiResponse.success(service.status());}
    @Operation(summary = "리포트 발송 미리보기 조회")
    @GetMapping("/reports/{id}/preview") public ApiResponse<OperationsService.Preview> preview(@PathVariable String id){return ApiResponse.success(service.preview(id));}
    @Operation(summary = "텔레그램 테스트 발송", description = "실제로 텔레그램 메시지를 발송합니다. Idempotency-Key 헤더와 테스트 발송 기능 활성화가 필요합니다.")
    @PostMapping("/telegram-tests") public ApiResponse<OperationsService.TestResult> test(@RequestHeader("Idempotency-Key") String key,@RequestBody OperationsService.TestRequest request){return ApiResponse.success(service.test(key,request));}
}
