package com.example.stockanalysis.backdata.validation.rule;

import com.example.stockanalysis.backdata.validation.model.BackdataRecord;
import com.example.stockanalysis.backdata.validation.model.ValidationCommand;
import com.example.stockanalysis.backdata.validation.model.ValidationIssue;
import com.example.stockanalysis.backdata.validation.model.ValidationSeverity;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public final class TemporalValidationRule implements ValidationRule {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Duration CLOCK_SKEW = Duration.ofMinutes(5);

    @Override
    public List<ValidationIssue> validate(ValidationCommand command) {
        List<ValidationIssue> issues = new ArrayList<>();
        for (int index = 0; index < command.records().size(); index++) {
            BackdataRecord record = command.records().get(index);
            if (record.observedAt().isAfter(command.validationTime().plus(CLOCK_SKEW))) {
                issues.add(RuleSupport.issue(
                        "OBSERVED_AT_IN_FUTURE",
                        ValidationSeverity.FATAL,
                        "수집 시각이 검증 시각보다 미래입니다.",
                        index,
                        record,
                        "observedAt"
                ));
            }
            if (record.businessDate() == null) {
                continue;
            }
            LocalDate observedDate = record.observedAt().atZone(SEOUL).toLocalDate();
            if (record.businessDate().isAfter(observedDate)) {
                issues.add(RuleSupport.issue(
                        "BUSINESS_DATE_IN_FUTURE",
                        ValidationSeverity.ERROR,
                        "거래일이 실제 수집 시점보다 미래입니다.",
                        index,
                        record,
                        "businessDate"
                ));
            }
        }
        return issues;
    }
}
