package com.example.stockanalysis.backdata.validation.rule;

import com.example.stockanalysis.backdata.validation.model.BackdataRecord;
import com.example.stockanalysis.backdata.validation.model.BackdataSchema;
import com.example.stockanalysis.backdata.validation.model.BackdataSchemaRegistry;
import com.example.stockanalysis.backdata.validation.model.ValidationCommand;
import com.example.stockanalysis.backdata.validation.model.ValidationIssue;
import com.example.stockanalysis.backdata.validation.model.ValidationSeverity;
import java.util.ArrayList;
import java.util.List;

public final class SchemaValidationRule implements ValidationRule {

    private final BackdataSchemaRegistry schemaRegistry;

    public SchemaValidationRule(BackdataSchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    @Override
    public List<ValidationIssue> validate(ValidationCommand command) {
        List<ValidationIssue> issues = new ArrayList<>();
        for (int index = 0; index < command.records().size(); index++) {
            BackdataRecord record = command.records().get(index);
            BackdataSchema schema = schemaRegistry.find(record.apiId()).orElse(null);
            if (schema == null) {
                issues.add(RuleSupport.issue(
                        "UNSUPPORTED_API",
                        ValidationSeverity.FATAL,
                        "등록되지 않은 백데이터 전문입니다.",
                        index,
                        record,
                        "apiId"
                ));
                continue;
            }
            if (schema.datasetType() != record.datasetType()) {
                issues.add(RuleSupport.issue(
                        "DATASET_TYPE_MISMATCH",
                        ValidationSeverity.FATAL,
                        "전문과 데이터셋 유형이 일치하지 않습니다.",
                        index,
                        record,
                        "datasetType"
                ));
            }
            if (schema.stockCodeRequired() && isBlank(record.stockCode())) {
                issues.add(RuleSupport.issue(
                        "STOCK_CODE_REQUIRED",
                        ValidationSeverity.ERROR,
                        "종목코드가 필요합니다.",
                        index,
                        record,
                        "stockCode"
                ));
            }
            if (schema.referenceCodeRequired() && isBlank(record.referenceCode())) {
                issues.add(RuleSupport.issue(
                        "REFERENCE_CODE_REQUIRED",
                        ValidationSeverity.ERROR,
                        "업종·테마와 같은 참조코드가 필요합니다.",
                        index,
                        record,
                        "referenceCode"
                ));
            }
            if (schema.businessDateRequired() && record.businessDate() == null) {
                issues.add(RuleSupport.issue(
                        "BUSINESS_DATE_REQUIRED",
                        ValidationSeverity.ERROR,
                        "기준 거래일이 필요합니다.",
                        index,
                        record,
                        "businessDate"
                ));
            }
            for (String field : schema.requiredFields()) {
                if (isBlank(record.value(field))) {
                    issues.add(RuleSupport.issue(
                            "REQUIRED_VALUE_MISSING",
                            ValidationSeverity.ERROR,
                            "필수 값이 없습니다: " + field,
                            index,
                            record,
                            field
                    ));
                }
            }
        }
        return issues;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
