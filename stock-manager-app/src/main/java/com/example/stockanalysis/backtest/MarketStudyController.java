package com.example.stockanalysis.backtest;

import static com.example.stockanalysis.backtest.MarketStudy.*;
import com.example.stockanalysis.shared.api.ApiResponse;
import java.io.IOException;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/swing-backtests/market-runs")
class MarketStudyController {
    private final MarketStudyService service;
    MarketStudyController(MarketStudyService service) { this.service = service; }
    @PutMapping("/{id}")
    ApiResponse<Job> create(@PathVariable UUID id,@RequestBody Spec spec) throws IOException {
        return ApiResponse.success(service.create(id,spec));
    }
    @GetMapping
    ApiResponse<List<Job>> list() throws IOException { return ApiResponse.success(service.jobs()); }
    @GetMapping("/{id}")
    ApiResponse<Job> job(@PathVariable UUID id) throws IOException { return ApiResponse.success(service.job(id)); }
    @GetMapping("/{id}/result")
    ApiResponse<Result> result(@PathVariable UUID id) throws IOException { return ApiResponse.success(service.result(id)); }
    @PostMapping("/{id}/cancel")
    ApiResponse<Job> cancel(@PathVariable UUID id) throws IOException { return ApiResponse.success(service.control(id,false)); }
    @PostMapping("/{id}/retry")
    ApiResponse<Job> retry(@PathVariable UUID id) throws IOException { return ApiResponse.success(service.control(id,true)); }
}
