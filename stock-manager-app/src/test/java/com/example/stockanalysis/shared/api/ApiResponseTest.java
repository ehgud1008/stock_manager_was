package com.example.stockanalysis.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.stockanalysis.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void createsSuccessResponseWithDefaultMessage() {
        ApiResponse<String> response = ApiResponse.success("payload");

        assertThat(response.success()).isTrue();
        assertThat(response.code()).isEqualTo("SUCCESS");
        assertThat(response.message()).isEqualTo("정상 처리되었습니다.");
        assertThat(response.data()).isEqualTo("payload");
    }

    @Test
    void createsSuccessResponseWithCustomMessage() {
        ApiResponse<Integer> response = ApiResponse.success("완료", 42);

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("완료");
        assertThat(response.data()).isEqualTo(42);
    }

    @Test
    void createsFailureResponseWithoutData() {
        ApiResponse<Void> response = ApiResponse.failure(ErrorCode.INVALID_REQUEST, "잘못된 요청");

        assertThat(response.success()).isFalse();
        assertThat(response.code()).isEqualTo("INVALID_REQUEST");
        assertThat(response.message()).isEqualTo("잘못된 요청");
        assertThat(response.data()).isNull();
    }
}
