package com.example.stockanalysis.unified.report;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.stockanalysis.shared.api.ApiResponse;
import com.example.stockanalysis.shared.exception.*;
import com.example.stockanalysis.unified.portfolio.PortfolioProperties;
import org.springframework.web.bind.annotation.*;

@Tag(name = "운영")
@RestController
@RequestMapping("/api/v1/unified-analysis/operations/telegram-connection")
public class TelegramConnectionController {
    private final TelegramConnectionService service;
    private final PortfolioProperties settings;
    public TelegramConnectionController(TelegramConnectionService service,PortfolioProperties settings){this.service=service;this.settings=settings;}
    @Operation(summary = "텔레그램 연결 확인", description = "운영 확인 기능이 활성화된 경우 텔레그램 연결 상태를 점검합니다.")
    @PostMapping public ApiResponse<TelegramConnectionService.Check> check(){
        if(!settings.operationsEnabled())throw new BusinessException(ErrorCode.UNIFIED_ANALYSIS_CONFLICT,"운영 확인 기능이 비활성화되어 있습니다.");
        return ApiResponse.success(service.check());
    }
}
