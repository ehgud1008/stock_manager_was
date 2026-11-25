package com.example.stockanalysis.backdata.validation.rule;

import com.example.stockanalysis.backdata.validation.model.BackdataDatasetType;
import com.example.stockanalysis.backdata.validation.model.BackdataRecord;
import com.example.stockanalysis.backdata.validation.model.ValidationCommand;
import com.example.stockanalysis.backdata.validation.model.ValidationIssue;
import com.example.stockanalysis.backdata.validation.model.ValidationSeverity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ShortSellingValidationRule implements ValidationRule {

    @Override
    public List<ValidationIssue> validate(ValidationCommand command) {
        List<ValidationIssue> issues = new ArrayList<>();
        for (int index = 0; index < command.records().size(); index++) {
            BackdataRecord record = command.records().get(index);
            if (record.datasetType() != BackdataDatasetType.SHORT_SELLING) {
                continue;
            }
            try {
                BigDecimal volume = RuleSupport.decimal(record.value("volume"));
                BigDecimal shortVolume = RuleSupport.decimal(record.value("shortVolume"));
                BigDecimal ratio = RuleSupport.decimal(record.value("shortRatio"));
                if (volume != null && shortVolume != null && shortVolume.compareTo(volume) > 0) {
                    issues.add(RuleSupport.issue(
                            "SHORT_VOLUME_EXCEEDS_VOLUME",
                            ValidationSeverity.ERROR,
                            "공매도 수량이 전체 거래량보다 큽니다.",
                            index,
                            record,
                            "shortVolume"
                    ));
                }
                if (ratio != null
                        && (ratio.signum() < 0 || ratio.compareTo(new BigDecimal("100")) > 0)) {
                    issues.add(RuleSupport.issue(
                            "SHORT_RATIO_OUT_OF_RANGE",
                            ValidationSeverity.ERROR,
                            "공매도 비중은 0~100 범위여야 합니다.",
                            index,
                            record,
                            "shortRatio"
                    ));
                }
            } catch (NumberFormatException ignored) {
                // 숫자 형식 오류는 NumericValidationRule이 한 번만 보고한다.
            }
        }
        return issues;
    }
}
