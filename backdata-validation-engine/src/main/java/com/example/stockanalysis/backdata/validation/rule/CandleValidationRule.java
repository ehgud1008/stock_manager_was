package com.example.stockanalysis.backdata.validation.rule;

import com.example.stockanalysis.backdata.validation.model.BackdataDatasetType;
import com.example.stockanalysis.backdata.validation.model.BackdataRecord;
import com.example.stockanalysis.backdata.validation.model.ValidationCommand;
import com.example.stockanalysis.backdata.validation.model.ValidationIssue;
import com.example.stockanalysis.backdata.validation.model.ValidationSeverity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class CandleValidationRule implements ValidationRule {

    private static final Set<BackdataDatasetType> CANDLE_TYPES = EnumSet.of(
            BackdataDatasetType.DAILY_CANDLE,
            BackdataDatasetType.DAILY_PRICE,
            BackdataDatasetType.INDUSTRY_DAILY_CANDLE
    );

    @Override
    public List<ValidationIssue> validate(ValidationCommand command) {
        List<ValidationIssue> issues = new ArrayList<>();
        for (int index = 0; index < command.records().size(); index++) {
            BackdataRecord record = command.records().get(index);
            if (!CANDLE_TYPES.contains(record.datasetType())) {
                continue;
            }
            try {
                BigDecimal open = RuleSupport.decimal(record.value("open"));
                BigDecimal high = RuleSupport.decimal(record.value("high"));
                BigDecimal low = RuleSupport.decimal(record.value("low"));
                BigDecimal close = RuleSupport.decimal(record.value("close"));
                if (open == null || high == null || low == null || close == null) {
                    continue;
                }
                if (open.signum() <= 0 || high.signum() <= 0
                        || low.signum() <= 0 || close.signum() <= 0) {
                    issues.add(RuleSupport.issue(
                            "NON_POSITIVE_PRICE",
                            ValidationSeverity.ERROR,
                            "OHLC 가격은 0보다 커야 합니다.",
                            index,
                            record,
                            "ohlc"
                    ));
                    continue;
                }
                BigDecimal upperBody = open.max(close);
                BigDecimal lowerBody = open.min(close);
                if (high.compareTo(upperBody) < 0) {
                    issues.add(RuleSupport.issue(
                            "HIGH_PRICE_RELATION_INVALID",
                            ValidationSeverity.ERROR,
                            "고가는 시가와 종가보다 작을 수 없습니다.",
                            index,
                            record,
                            "high"
                    ));
                }
                if (low.compareTo(lowerBody) > 0) {
                    issues.add(RuleSupport.issue(
                            "LOW_PRICE_RELATION_INVALID",
                            ValidationSeverity.ERROR,
                            "저가는 시가와 종가보다 클 수 없습니다.",
                            index,
                            record,
                            "low"
                    ));
                }
                if (high.compareTo(low) < 0) {
                    issues.add(RuleSupport.issue(
                            "PRICE_RANGE_INVALID",
                            ValidationSeverity.ERROR,
                            "고가는 저가보다 작을 수 없습니다.",
                            index,
                            record,
                            "high"
                    ));
                }
            } catch (NumberFormatException ignored) {
                // 숫자 형식 오류는 NumericValidationRule이 한 번만 보고한다.
            }
        }
        return issues;
    }
}
