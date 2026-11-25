package com.example.stockanalysis.backdata.validation.model;

import java.util.List;

public record ValidationResult(
        ValidationSummary summary,
        List<BackdataRecord> validRecords,
        List<BackdataRecord> quarantinedRecords,
        List<ValidationIssue> issues
) {

    public ValidationResult {
        validRecords = List.copyOf(validRecords);
        quarantinedRecords = List.copyOf(quarantinedRecords);
        issues = List.copyOf(issues);
    }

    public boolean isBacktestReady() {
        return summary.totalRecords() > 0
                && quarantinedRecords.isEmpty()
                && issues.stream().noneMatch(issue -> issue.severity().blocksRecord());
    }
}
