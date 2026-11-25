package com.example.stockanalysis.kiwoom.tr.api;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record KiwoomTrExecutionRequest(
        @Schema(description = "키움 TR 요청 헤더를 담는 JSON 객체") @Valid @NotNull KiwoomTrRequestHeader header,
        @Schema(description = "TR별 요청 전문. 필드명은 키움 원본 전문을 사용", example = "{\"stk_cd\":\"005930\"}")
        @NotNull Map<String, Object> body
) {

    public KiwoomTrExecutionRequest {
        body = body == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(body));
    }
}
