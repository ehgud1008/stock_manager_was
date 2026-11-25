package com.example.stockanalysis.backdata.validation.rule;

import com.example.stockanalysis.backdata.validation.model.BackdataRecord;
import com.example.stockanalysis.backdata.validation.model.ValidationIssue;
import com.example.stockanalysis.backdata.validation.model.ValidationSeverity;
import java.math.BigDecimal;

final class RuleSupport {

    private RuleSupport() {
    }

    static ValidationIssue issue(
            String code,
            ValidationSeverity severity,
            String message,
            int index,
            BackdataRecord record,
            String field
    ) {
        return new ValidationIssue(
                code,
                severity,
                message,
                index,
                record.apiId(),
                record.stockCode(),
                record.businessDate(),
                field
        );
    }

    static BigDecimal decimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim()
                .replace(",", "")
                .replace("%", "");
        return new BigDecimal(normalized);
    }
}
