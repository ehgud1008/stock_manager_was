package com.example.stockanalysis.stock.domain;

import java.util.regex.Pattern;

public record StockCode(String value) {

    private static final Pattern SIX_DIGIT_CODE = Pattern.compile("\\d{6}");

    public StockCode {
        if (value == null || !SIX_DIGIT_CODE.matcher(value).matches()) {
            throw new IllegalArgumentException("종목코드는 숫자 6자리여야 합니다.");
        }
    }

    public static StockCode of(String value) {
        return new StockCode(value);
    }
}
