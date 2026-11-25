package com.example.stockanalysis.backdata.validation.rule;

import com.example.stockanalysis.backdata.validation.model.BackdataDatasetType;
import com.example.stockanalysis.backdata.validation.model.BackdataRecord;
import com.example.stockanalysis.backdata.validation.model.ValidationCommand;
import com.example.stockanalysis.backdata.validation.model.ValidationIssue;
import com.example.stockanalysis.backdata.validation.model.ValidationSeverity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CrossSourceCandleRule implements ValidationRule {

    private static final Set<BackdataDatasetType> STOCK_PRICE_TYPES = Set.of(
            BackdataDatasetType.DAILY_CANDLE,
            BackdataDatasetType.DAILY_PRICE,
            BackdataDatasetType.DAILY_TRADE_DETAIL
    );
    private static final Set<String> COMPARABLE_FIELDS = Set.of(
            "close",
            "volume",
            "tradingAmount"
    );

    @Override
    public boolean supports(ValidationCommand command) {
        return command.profile().includesSeriesRules();
    }

    @Override
    public List<ValidationIssue> validate(ValidationCommand command) {
        Map<PriceKey, List<IndexedRecord>> grouped = new HashMap<>();
        for (int index = 0; index < command.records().size(); index++) {
            BackdataRecord record = command.records().get(index);
            if (!STOCK_PRICE_TYPES.contains(record.datasetType())
                    || record.stockCode() == null
                    || record.businessDate() == null) {
                continue;
            }
            grouped.computeIfAbsent(
                    new PriceKey(record.stockCode(), record.businessDate()),
                    ignored -> new ArrayList<>()
            ).add(new IndexedRecord(index, record));
        }

        List<ValidationIssue> issues = new ArrayList<>();
        for (List<IndexedRecord> records : grouped.values()) {
            if (records.size() < 2) {
                continue;
            }
            IndexedRecord baseline = records.get(0);
            for (int currentIndex = 1; currentIndex < records.size(); currentIndex++) {
                IndexedRecord current = records.get(currentIndex);
                if (baseline.record().apiId().equals(current.record().apiId())) {
                    continue;
                }
                compare(baseline, current, issues);
            }
        }
        return issues;
    }

    private void compare(
            IndexedRecord baseline,
            IndexedRecord current,
            List<ValidationIssue> issues
    ) {
        for (String field : COMPARABLE_FIELDS) {
            try {
                BigDecimal expected = RuleSupport.decimal(baseline.record().value(field));
                BigDecimal actual = RuleSupport.decimal(current.record().value(field));
                if (expected == null || actual == null || expected.compareTo(actual) == 0) {
                    continue;
                }
                BigDecimal denominator = expected.abs().max(BigDecimal.ONE);
                BigDecimal differenceRatio = expected.subtract(actual)
                        .abs()
                        .divide(denominator, 6, RoundingMode.HALF_UP);
                ValidationSeverity severity = differenceRatio.compareTo(new BigDecimal("0.01")) > 0
                        ? ValidationSeverity.ERROR
                        : ValidationSeverity.WARNING;
                issues.add(RuleSupport.issue(
                        "CROSS_SOURCE_VALUE_MISMATCH",
                        severity,
                        baseline.record().apiId() + " 값과 일치하지 않습니다: " + field,
                        current.index(),
                        current.record(),
                        field
                ));
            } catch (NumberFormatException ignored) {
                // 숫자 형식 오류는 NumericValidationRule이 한 번만 보고한다.
            }
        }
    }

    private record PriceKey(String stockCode, LocalDate businessDate) {
    }

    private record IndexedRecord(int index, BackdataRecord record) {
    }
}
