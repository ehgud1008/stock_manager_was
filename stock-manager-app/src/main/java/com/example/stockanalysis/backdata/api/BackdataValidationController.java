package com.example.stockanalysis.backdata.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.stockanalysis.backdata.application.BackdataValidationService;
import com.example.stockanalysis.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "백데이터 검증")
@RestController
@RequestMapping("/api/v1/backdata")
public class BackdataValidationController {

    private final BackdataValidationService service;

    public BackdataValidationController(BackdataValidationService service) {
        this.service = service;
    }

    @Operation(summary = "검증 가능한 키움 TR 목록 조회")
    @GetMapping("/trs")
    public ApiResponse<List<BackdataTrResponse>> supportedTrs() {
        return ApiResponse.success(service.supportedTrs());
    }

    @Operation(summary = "키움 백데이터 단일 검증", description = "TR 호출 결과를 검증하고 검증 실행 결과를 생성합니다.")
    @PostMapping("/validation-runs/kiwoom")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BackdataValidationRunResponse> validate(
            @Valid @RequestBody KiwoomBackdataValidationRequest request
    ) {
        return ApiResponse.success(service.validate(request));
    }

    @Operation(summary = "키움 백데이터 일괄 검증", description = "여러 TR의 검증 실행 결과를 생성합니다.")
    @PostMapping("/validation-runs/kiwoom/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BackdataBatchValidationRunResponse> validateBatch(
            @Valid @RequestBody KiwoomBackdataBatchValidationRequest request
    ) {
        return ApiResponse.success(service.validateBatch(request));
    }
}
