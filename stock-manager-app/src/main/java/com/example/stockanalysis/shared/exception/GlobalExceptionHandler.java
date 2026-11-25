package com.example.stockanalysis.shared.exception;

import com.example.stockanalysis.shared.api.ApiResponse;
import com.example.stockanalysis.shared.logging.SensitiveLogSanitizer;
import com.example.stockanalysis.kiwoom.client.KiwoomApiException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        if (exception instanceof KiwoomApiException apiException) {
            if (apiException.markLogged()) {
                log.error(
                        "외부 시스템 응답 처리 실패. targetSystem=KIWOOM, errorCode={}, "
                                + "externalStatus={}, failureCode={}",
                        errorCode,
                        apiException.getExternalStatusCode(),
                        apiException.getFailureCode(),
                        SensitiveLogSanitizer.sanitize(apiException)
                );
            } else {
                log.warn(
                        "요청 처리 중단. errorCode={}, httpStatus={}, causeType={}, "
                                + "errorLoggedBy=EXTERNAL_CLIENT",
                        errorCode,
                        errorCode.getHttpStatus().value(),
                        exception.getClass().getSimpleName()
                );
            }
        } else if (errorCode.getHttpStatus().is4xxClientError()) {
            log.warn(
                    "요청 처리 거부. errorCode={}, httpStatus={}, causeType={}",
                    errorCode,
                    errorCode.getHttpStatus().value(),
                    exception.getClass().getSimpleName()
            );
        } else {
            log.error(
                    "요청 처리 실패. errorCode={}, httpStatus={}",
                    errorCode,
                    errorCode.getHttpStatus().value(),
                    SensitiveLogSanitizer.sanitize(exception)
            );
        }
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(errorCode, exception.getMessage()));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleValidationException(Exception exception) {
        log.warn(
                "요청 검증 실패. errorCode={}, causeType={}",
                ErrorCode.VALIDATION_ERROR,
                exception.getClass().getSimpleName()
        );
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(errorCode, errorCode.getDefaultMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        log.warn(
                "요청 본문 해석 실패. errorCode={}, causeType={}",
                ErrorCode.INVALID_REQUEST,
                exception.getClass().getSimpleName()
        );
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(errorCode, errorCode.getDefaultMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingResource(NoResourceFoundException exception) {
        ErrorCode errorCode = ErrorCode.RESOURCE_NOT_FOUND;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(errorCode, errorCode.getDefaultMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error(
                "예상하지 못한 요청 처리 실패. errorCode={}",
                ErrorCode.INTERNAL_SERVER_ERROR,
                SensitiveLogSanitizer.sanitize(exception)
        );
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(errorCode, errorCode.getDefaultMessage()));
    }
}
