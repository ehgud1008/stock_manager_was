package com.example.stockanalysis.backdata.validation.rule;

import com.example.stockanalysis.backdata.validation.model.BackdataRecord;
import com.example.stockanalysis.backdata.validation.model.BackdataSchema;
import com.example.stockanalysis.backdata.validation.model.BackdataSchemaRegistry;
import com.example.stockanalysis.backdata.validation.model.ValidationCommand;
import com.example.stockanalysis.backdata.validation.model.ValidationIssue;
import com.example.stockanalysis.backdata.validation.model.ValidationSeverity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class NumericValidationRule implements ValidationRule {

    private final BackdataSchemaRegistry schemaRegistry;

    public NumericValidationRule(BackdataSchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    @Override
    public List<ValidationIssue> validate(ValidationCommand command) {
        List<ValidationIssue> issues = new ArrayList<>();
        for (int index = 0; index < command.records().size(); index++) {
            BackdataRecord record = command.records().get(index);
            BackdataSchema schema = schemaRegistry.find(record.apiId()).orElse(null);
            if (schema == null) {
                continue;
            }
            for (String field : schema.numericFields()) {
                String value = record.value(field);
                if (value == null || value.isBlank()) {
                    continue;
                }
                try {
                    BigDecimal decimal = RuleSupport.decimal(value);
                    if (schema.nonNegativeFields().contains(field)
                            && decimal != null
                            && decimal.signum() < 0) {
                        issues.add(RuleSupport.issue(
                                "NEGATIVE_VALUE",
                                ValidationSeverity.ERROR,
                                "음수가 허용되지 않는 값입니다: " + field,
                                index,
                                record,
                                field
                        ));
                    }
                } catch (NumberFormatException exception) {
                    issues.add(RuleSupport.issue(
                            "INVALID_NUMBER",
                            ValidationSeverity.ERROR,
                            "숫자 형식이 올바르지 않습니다: " + field,
                            index,
                            record,
                            field
                    ));
                }
            }
        }
        return issues;
    }
}
