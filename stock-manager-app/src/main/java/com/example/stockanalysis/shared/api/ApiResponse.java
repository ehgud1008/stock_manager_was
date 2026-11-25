package com.example.stockanalysis.shared.api;

import com.example.stockanalysis.shared.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

public record ApiResponse<T>(
        @Schema(description = "요청 처리 성공 여부", example = "true") boolean success,
        @Schema(description = "성공 시 SUCCESS, 실패 시 업무 오류 코드", example = "SUCCESS") String code,
        @Schema(description = "처리 결과 설명", example = "정상 처리되었습니다.") String message,
        @Schema(description = "API별 결과 데이터. 실패 시 null") T data
) {

    private static final String SUCCESS_MESSAGE = "정상 처리되었습니다.";

    public static <T> ApiResponse<T> success(T data) {
        return success(SUCCESS_MESSAGE, data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, ErrorCode.SUCCESS.name(), message, data);
    }

    public static <T> ApiResponse<T> failure(ErrorCode code, String message) {
        return new ApiResponse<>(false, code.name(), message, null);
    }
}
