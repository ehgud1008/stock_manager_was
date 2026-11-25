package com.example.stockanalysis.unified.portfolio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.stockanalysis.shared.api.ApiResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "누적 모의계좌")
@RestController
@RequestMapping("/api/v1/unified-analysis/portfolios")
public class PortfolioController {
    private final PortfolioService service;
    public PortfolioController(PortfolioService service){this.service=service;}
    public record Listing(boolean enabled,List<PortfolioModels.Summary> runs,List<PortfolioModels.Available> reports) {}
    @Operation(summary = "모의계좌 목록 및 사용 가능한 보고서 조회")
    @GetMapping public ApiResponse<Listing> list(){return ApiResponse.success(new Listing(service.enabled(),service.recent(),service.available()));}
    @Operation(summary = "보고서 후보 선정 감사 조회")
    @GetMapping("/reports/{id}/selection-audit") public ApiResponse<SelectionDiagnostics.Audit> audit(@PathVariable String id){return ApiResponse.success(service.audit(id));}
    @Operation(summary = "모의계좌 평가 등록", description = "비동기 평가를 등록합니다. Idempotency-Key 헤더가 필요합니다.")
    @PostMapping @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<PortfolioModels.View> create(@RequestBody PortfolioModels.Request request,@RequestHeader("Idempotency-Key") String key){return ApiResponse.success(service.create(request,key));}
    @Operation(summary = "모의계좌 평가 조회")
    @GetMapping("/{id}") public ApiResponse<PortfolioModels.View> get(@PathVariable String id){return ApiResponse.success(service.get(id));}
    @Operation(summary = "모의계좌 특정 개정 결과 조회")
    @GetMapping("/{id}/revisions/{revision}") public ApiResponse<PortfolioModels.Result> revision(@PathVariable String id,@PathVariable int revision){return ApiResponse.success(service.revision(id,revision));}
}
