package com.example.stockanalysis.backdata.validation.model;

import java.time.LocalDate;

public record ValidationIssue(
        String code,
        ValidationSeverity severity,
        String message,
        int recordIndex,
        String apiId,
        String stockCode,
        LocalDate businessDate,
        String field
) {
}
