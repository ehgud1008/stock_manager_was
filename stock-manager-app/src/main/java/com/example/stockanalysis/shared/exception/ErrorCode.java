package com.example.stockanalysis.shared.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    SUCCESS(HttpStatus.OK, "정상 처리되었습니다."),
    DECISION_CONFLICT(HttpStatus.CONFLICT, "판단 요청이 기존 요청과 충돌합니다."),
    DECISION_DISABLED(HttpStatus.SERVICE_UNAVAILABLE, "저장 분석 기능이 아직 활성화되지 않았습니다."),
    DECISION_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "분석 요청 또는 AI 사용 한도를 초과했습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청값이 올바르지 않습니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청값 검증에 실패했습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    SCREENER_CONFLICT(HttpStatus.CONFLICT, "스크리너 실행 요청이 충돌했습니다."),
    UNIFIED_ANALYSIS_CONFLICT(HttpStatus.CONFLICT, "종합분석 실행 요청이 충돌했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "외부 API 처리 중 오류가 발생했습니다."),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "데이터 처리 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
