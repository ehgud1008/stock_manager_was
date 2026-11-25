package com.example.stockanalysis.analysis.domain;

import com.example.stockanalysis.shared.exception.BusinessException;
import com.example.stockanalysis.shared.exception.ErrorCode;
import java.util.Locale;

public enum StockPricePeriod {
    MINUTE("ka10080"),
    DAY("ka10081"),
    WEEK("ka10082"),
    MONTH("ka10083");

    private final String apiId;

    StockPricePeriod(String apiId) {
        this.apiId = apiId;
    }

    public String apiId() {
        return apiId;
    }

    public static StockPricePeriod from(String value) {
        try {
            return valueOf(value == null ? "DAY" : value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "가격 주기는 MINUTE, DAY, WEEK, MONTH 중 하나여야 합니다.",
                    exception
            );
        }
    }
}
