package com.example.stockanalysis.backdata.api;

import com.example.stockanalysis.backdata.validation.model.ValidationIssue;
import com.example.stockanalysis.backdata.validation.model.ValidationResult;
import java.util.List;

public enum BackdataValidationStatus {
    VALID,
    WARNING,
    INVALID;

    public static BackdataValidationStatus from(ValidationResult result) {
        if (!result.isBacktestReady()) {
            return INVALID;
        }
        return result.issues().isEmpty() ? VALID : WARNING;
    }

    public static BackdataValidationStatus from(
            int recordCount,
            List<ValidationIssue> issues
    ) {
        if (recordCount == 0 || issues.stream().anyMatch(
                issue -> issue.severity().blocksRecord()
        )) {
            return INVALID;
        }
        return issues.isEmpty() ? VALID : WARNING;
    }
}
