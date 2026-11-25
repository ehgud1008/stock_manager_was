package com.example.stockanalysis.backdata.validation.rule;

import com.example.stockanalysis.backdata.validation.model.ValidationCommand;
import com.example.stockanalysis.backdata.validation.model.ValidationIssue;
import java.util.List;

public interface ValidationRule {

    List<ValidationIssue> validate(ValidationCommand command);

    default boolean supports(ValidationCommand command) {
        return true;
    }
}
