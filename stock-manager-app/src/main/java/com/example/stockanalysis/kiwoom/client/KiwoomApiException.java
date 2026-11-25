package com.example.stockanalysis.kiwoom.client;

import com.example.stockanalysis.shared.exception.BusinessException;
import com.example.stockanalysis.shared.exception.ErrorCode;

public class KiwoomApiException extends BusinessException {

    private final Integer externalStatusCode;
    private final String failureCode;
    private boolean logged;

    public KiwoomApiException(String message) {
        this(message, null, "KIWOOM_API_ERROR", null);
    }

    public KiwoomApiException(String message, Throwable cause) {
        this(message, null, "KIWOOM_API_ERROR", cause);
    }

    public KiwoomApiException(String message, Integer externalStatusCode, String failureCode) {
        this(message, externalStatusCode, failureCode, null);
    }

    public KiwoomApiException(
            String message,
            Integer externalStatusCode,
            String failureCode,
            Throwable cause
    ) {
        super(ErrorCode.EXTERNAL_API_ERROR, message, cause);
        this.externalStatusCode = externalStatusCode;
        this.failureCode = failureCode;
    }

    public Integer getExternalStatusCode() {
        return externalStatusCode;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public synchronized boolean markLogged() {
        if (logged) {
            return false;
        }
        logged = true;
        return true;
    }
}
