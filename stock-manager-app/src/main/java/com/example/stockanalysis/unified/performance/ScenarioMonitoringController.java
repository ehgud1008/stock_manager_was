package com.example.stockanalysis.unified.performance;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.stockanalysis.shared.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

@Tag(name = "시나리오 모니터링")
@RestController
@RequestMapping("/api/v1/unified-analysis/runs/{id}/monitoring")
public class ScenarioMonitoringController {
    private final ScenarioMonitoringService service;
    public ScenarioMonitoringController(ScenarioMonitoringService service){this.service=service;}
    @Operation(summary = "실행별 시나리오 모니터링 조회")
    @GetMapping public ApiResponse<ScenarioMonitoringModels.View> get(@PathVariable String id) {
        return ApiResponse.success(service.get(id));
    }
}
