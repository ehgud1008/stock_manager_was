package com.example.stockanalysis.decision;

import com.example.stockanalysis.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/account-risk")
@Tag(name="계좌 위험 설정")
public class AccountRiskController {
    private final AccountRiskService service;
    public AccountRiskController(AccountRiskService service){this.service=service;}
    @GetMapping @Operation(summary="현재 소유자의 계좌 위험 설정 조회")
    public ApiResponse<AccountRiskModels.View> get(){return ApiResponse.success(service.get());}
    @PutMapping @Operation(summary="계좌 위험 설정 등록 및 수정",description="현금 포함 총자산과 허용 위험을 직접 입력합니다. 수정 시 expectedRevision을 사용하며 과거 판단은 변경하지 않습니다.")
    public ApiResponse<AccountRiskModels.Settings> save(@Valid @RequestBody AccountRiskModels.Edit edit){return ApiResponse.success(service.save(edit));}
}
