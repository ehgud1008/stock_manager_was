package com.example.stockanalysis.storage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.stockanalysis.shared.api.ApiResponse;
import com.example.stockanalysis.shared.exception.*;
import com.example.stockanalysis.unified.portfolio.PortfolioProperties;
import org.springframework.web.bind.annotation.*;

@Tag(name = "운영")
@RestController
@RequestMapping("/api/v1/unified-analysis/operations/storage")
public class StorageController {
    private final StorageService service;private final PortfolioProperties operations;
    public StorageController(StorageService service,PortfolioProperties operations){this.service=service;this.operations=operations;}
    @Operation(summary = "스냅샷 저장소 상태 조회", description = "운영 확인 기능이 비활성화되어 있으면 HTTP 404를 반환합니다.")
    @GetMapping public ApiResponse<StorageService.Status> status(){if(!operations.operationsEnabled())throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);return ApiResponse.success(service.status());}
}
