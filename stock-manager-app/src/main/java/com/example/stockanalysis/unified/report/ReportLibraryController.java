package com.example.stockanalysis.unified.report;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.stockanalysis.shared.api.ApiResponse;
import com.example.stockanalysis.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "장 마감 리포트")
@RestController
@RequestMapping("/api/v1/unified-analysis/reports")
public class ReportLibraryController {
    private final DailyReportService service;
    public ReportLibraryController(DailyReportService service){this.service=service;}
    @Operation(summary = "장 마감 리포트 보관함 조회", description = "기본 page=0, size=30으로 리포트 보관함을 조회합니다.")
    @GetMapping public ApiResponse<ReportModels.Library> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="30") int size){
        return ApiResponse.success(service.library(page,size));
    }
    @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> invalid(IllegalArgumentException e){return ApiResponse.failure(ErrorCode.INVALID_REQUEST,e.getMessage());}
}
