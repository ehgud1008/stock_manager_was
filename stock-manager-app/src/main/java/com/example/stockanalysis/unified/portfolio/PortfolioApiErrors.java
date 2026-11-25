package com.example.stockanalysis.unified.portfolio;

import com.example.stockanalysis.shared.api.ApiResponse;
import com.example.stockanalysis.shared.exception.ErrorCode;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.*;

/** Input and reproducibility checks are actionable client errors, not server failures. */
@Order(-1)
@RestControllerAdvice(assignableTypes={PortfolioController.class,OperationsController.class})
public class PortfolioApiErrors {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> invalid(IllegalArgumentException e){
        return ResponseEntity.badRequest().body(ApiResponse.failure(ErrorCode.INVALID_REQUEST,e.getMessage()));
    }
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> missingKey(MissingRequestHeaderException e){
        return ResponseEntity.badRequest().body(ApiResponse.failure(ErrorCode.INVALID_REQUEST,"Idempotency-Key가 필요합니다."));
    }
}
