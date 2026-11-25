package com.example.stockanalysis.kiwoom.tr.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Locale;

public record KiwoomTrRequestHeader(
        @Schema(description = "호출할 키움 TR ID", example = "ka10001")
        @JsonProperty("api-id") @NotBlank String apiId,
        @Schema(description = "키움 접근 토큰. 실제 호출에 사용할 인증 문자열을 입력")
        @JsonProperty("authorization") @NotBlank String authorization,
        @Schema(description = "연속조회 여부. 생략 시 N", allowableValues = {"Y", "N"}, example = "N")
        @JsonProperty("cont-yn")
        @Pattern(regexp = "[YN]", message = "cont-yn은 Y 또는 N이어야 합니다.")
        String contYn,
        @Schema(description = "연속조회 키. cont-yn=Y일 때 필수", example = "")
        @JsonProperty("next-key") String nextKey
) {

    public KiwoomTrRequestHeader {
        apiId = apiId == null ? null : apiId.trim().toLowerCase(Locale.ROOT);
        authorization = authorization == null ? null : authorization.trim();
        contYn = contYn == null || contYn.isBlank()
                ? "N"
                : contYn.trim().toUpperCase(Locale.ROOT);
        nextKey = nextKey == null ? "" : nextKey.trim();
    }

    @Override
    public String toString() {
        return "KiwoomTrRequestHeader[apiId=" + apiId
                + ", authorization=***, contYn=" + contYn
                + ", nextKey=" + nextKey + "]";
    }
}
