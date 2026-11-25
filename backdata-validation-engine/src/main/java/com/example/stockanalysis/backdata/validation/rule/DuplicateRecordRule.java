package com.example.stockanalysis.backdata.validation.rule;

import com.example.stockanalysis.backdata.validation.model.BackdataRecord;
import com.example.stockanalysis.backdata.validation.model.ValidationCommand;
import com.example.stockanalysis.backdata.validation.model.ValidationIssue;
import com.example.stockanalysis.backdata.validation.model.ValidationSeverity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DuplicateRecordRule implements ValidationRule {

    @Override
    public List<ValidationIssue> validate(ValidationCommand command) {
        Set<RecordKey> seen = new HashSet<>();
        List<ValidationIssue> issues = new ArrayList<>();
        for (int index = 0; index < command.records().size(); index++) {
            BackdataRecord record = command.records().get(index);
            RecordKey key = new RecordKey(
                    record.apiId(),
                    record.stockCode(),
                    record.referenceCode(),
                    record.variant(),
                    record.businessDate()
            );
            if (!seen.add(key)) {
                issues.add(RuleSupport.issue(
                        "DUPLICATE_RECORD",
                        ValidationSeverity.ERROR,
                        "동일한 자연키의 데이터가 중복되었습니다.",
                        index,
                        record,
                        null
                ));
            }
        }
        return issues;
    }

    private record RecordKey(
            String apiId,
            String stockCode,
            String referenceCode,
            String variant,
            java.time.LocalDate businessDate
    ) {
    }
}
