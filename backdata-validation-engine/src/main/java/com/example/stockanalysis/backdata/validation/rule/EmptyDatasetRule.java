package com.example.stockanalysis.backdata.validation.rule;

import com.example.stockanalysis.backdata.validation.model.ValidationCommand;
import com.example.stockanalysis.backdata.validation.model.ValidationIssue;
import com.example.stockanalysis.backdata.validation.model.ValidationSeverity;
import java.util.List;

public final class EmptyDatasetRule implements ValidationRule {

    @Override
    public List<ValidationIssue> validate(ValidationCommand command) {
        if (!command.records().isEmpty()) {
            return List.of();
        }
        return List.of(new ValidationIssue(
                "EMPTY_DATASET",
                ValidationSeverity.FATAL,
                "검증할 데이터가 없습니다.",
                -1,
                null,
                null,
                null,
                null
        ));
    }
}
