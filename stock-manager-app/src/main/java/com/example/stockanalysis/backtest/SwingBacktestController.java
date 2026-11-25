package com.example.stockanalysis.backtest;

import static com.example.stockanalysis.backtest.SwingBacktestModels.*;

import com.example.stockanalysis.shared.api.ApiResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/swing-backtests")
class SwingBacktestController {
    private final SwingBacktestService service;

    SwingBacktestController(SwingBacktestService service) {
        this.service = service;
    }

    @PostMapping("/historical")
    ApiResponse<Result> historical(@RequestBody Request request) {
        return ApiResponse.success(service.historical(request));
    }

    @GetMapping("/paper-runs")
    ApiResponse<PaperStatus> paperRuns() {
        return ApiResponse.success(service.paperStatus());
    }

    @PutMapping("/paper-runs/{id}")
    ApiResponse<Result> start(@PathVariable UUID id, @RequestBody Request request) {
        return ApiResponse.success(service.start(id, request));
    }

    @GetMapping("/paper-runs/{id}")
    ApiResponse<Result> get(@PathVariable UUID id) {
        return ApiResponse.success(service.get(id));
    }
}
