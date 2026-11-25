package com.example.stockanalysis.kiwoom.client;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class KiwoomNumberParser {

    public BigDecimal decimal(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim().replace(",", ""));
        } catch (NumberFormatException exception) {
            throw new KiwoomApiException(
                    "키움증권 응답의 " + fieldName + " 값이 올바르지 않습니다.",
                    exception
            );
        }
    }

    public BigDecimal absoluteDecimal(String value, String fieldName) {
        BigDecimal decimal = decimal(value, fieldName);
        return decimal == null ? null : decimal.abs();
    }
}
