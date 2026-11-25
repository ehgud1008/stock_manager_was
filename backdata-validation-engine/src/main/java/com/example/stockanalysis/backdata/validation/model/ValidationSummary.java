package com.example.stockanalysis.backdata.validation.model;

import java.util.Map;

public record ValidationSummary(
        int totalRecords,
        int validRecords,
        int quarantinedRecords,
        int issueCount,
        Map<ValidationSeverity, Long> issuesBySeverity
) {

    public ValidationSummary {
        issuesBySeverity = Map.copyOf(issuesBySeverity);
    }
}
