package com.example.stockanalysis.kiwoom.tr.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.stockanalysis.kiwoom.tr.application.KiwoomTrExecutionService;
import com.example.stockanalysis.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "키움 TR")
@RestController
@RequestMapping("/api/v1/kiwoom/tr")
public class KiwoomTrController {

    private final KiwoomTrExecutionService service;

    public KiwoomTrController(KiwoomTrExecutionService service) {
        this.service = service;
    }

    @Operation(summary = "키움 TR 실행", description = "JSON 본문의 header에 api-id, authorization, cont-yn, next-key를 전달합니다. authorization은 이 API의 본문 필드입니다. body 구조는 TR별로 다릅니다. 연속조회 cont-yn=Y이면 next-key가 필요합니다.")
    @PostMapping
    public ApiResponse<KiwoomTrExecutionResponse> execute(
            @Valid @RequestBody KiwoomTrExecutionRequest request
    ) {
        return ApiResponse.success(
                "키움 전문 통신이 완료되었습니다.",
                service.execute(request)
        );
    }
}
