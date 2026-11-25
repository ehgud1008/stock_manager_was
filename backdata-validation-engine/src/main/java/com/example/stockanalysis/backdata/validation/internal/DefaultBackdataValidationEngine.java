package com.example.stockanalysis.backdata.validation.internal;

import com.example.stockanalysis.backdata.validation.api.BackdataValidationEngine;
import com.example.stockanalysis.backdata.validation.model.BackdataRecord;
import com.example.stockanalysis.backdata.validation.model.BackdataSchemaRegistry;
import com.example.stockanalysis.backdata.validation.model.ValidationCommand;
import com.example.stockanalysis.backdata.validation.model.ValidationIssue;
import com.example.stockanalysis.backdata.validation.model.ValidationResult;
import com.example.stockanalysis.backdata.validation.model.ValidationSeverity;
import com.example.stockanalysis.backdata.validation.model.ValidationSummary;
import com.example.stockanalysis.backdata.validation.rule.CandleValidationRule;
import com.example.stockanalysis.backdata.validation.rule.CrossSourceCandleRule;
import com.example.stockanalysis.backdata.validation.rule.DuplicateRecordRule;
import com.example.stockanalysis.backdata.validation.rule.EmptyDatasetRule;
import com.example.stockanalysis.backdata.validation.rule.NumericValidationRule;
import com.example.stockanalysis.backdata.validation.rule.SchemaValidationRule;
import com.example.stockanalysis.backdata.validation.rule.ShortSellingValidationRule;
import com.example.stockanalysis.backdata.validation.rule.TemporalValidationRule;
import com.example.stockanalysis.backdata.validation.rule.ValidationRule;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DefaultBackdataValidationEngine implements BackdataValidationEngine {

    private static final Logger log =
            LoggerFactory.getLogger(DefaultBackdataValidationEngine.class);
    private static final long SLOW_RULE_THRESHOLD_MS = 1_000L;
    private static final long SLOW_VALIDATION_THRESHOLD_MS = 5_000L;

    private final List<ValidationRule> rules;

    public DefaultBackdataValidationEngine(BackdataSchemaRegistry schemaRegistry) {
        this(List.of(
                new EmptyDatasetRule(),
                new SchemaValidationRule(schemaRegistry),
                new NumericValidationRule(schemaRegistry),
                new TemporalValidationRule(),
                new DuplicateRecordRule(),
                new CandleValidationRule(),
                new ShortSellingValidationRule(),
                new CrossSourceCandleRule()
        ));
    }

    public DefaultBackdataValidationEngine(List<ValidationRule> rules) {
        this.rules = List.copyOf(rules);
    }

    @Override
    public ValidationResult validate(ValidationCommand command) {
        long startedAt = System.currentTimeMillis();
        int appliedRuleCount = 0;
        int skippedRuleCount = 0;
        String currentRule = "COMMAND_VALIDATION";
        log.info(
                "백테스트 데이터 검증 엔진 시작. profile={}, recordCount={}, configuredRuleCount={}",
                command.profile(),
                command.records().size(),
                rules.size()
        );

        List<ValidationIssue> issues = new ArrayList<>();
        try {
            for (ValidationRule rule : rules) {
                currentRule = rule.getClass().getSimpleName();
                if (!rule.supports(command)) {
                    skippedRuleCount++;
                    log.debug(
                            "백테스트 데이터 검증 규칙 제외. profile={}, rule={}, reason=UNSUPPORTED_PROFILE",
                            command.profile(),
                            currentRule
                    );
                    continue;
                }

                appliedRuleCount++;
                long ruleStartedAt = System.currentTimeMillis();
                List<ValidationIssue> ruleIssues = rule.validate(command);
                long ruleElapsedMs = System.currentTimeMillis() - ruleStartedAt;
                issues.addAll(ruleIssues);
                if (ruleElapsedMs >= SLOW_RULE_THRESHOLD_MS) {
                    log.warn(
                            "백테스트 데이터 검증 규칙 종료. profile={}, rule={}, recordCount={}, "
                                    + "issueCount={}, elapsedMs={}, thresholdMs={}",
                            command.profile(),
                            currentRule,
                            command.records().size(),
                            ruleIssues.size(),
                            ruleElapsedMs,
                            SLOW_RULE_THRESHOLD_MS
                    );
                } else {
                    log.debug(
                            "백테스트 데이터 검증 규칙 종료. profile={}, rule={}, recordCount={}, "
                                    + "issueCount={}, elapsedMs={}",
                            command.profile(),
                            currentRule,
                            command.records().size(),
                            ruleIssues.size(),
                            ruleElapsedMs
                    );
                }
            }
        } catch (RuntimeException exception) {
            log.warn(
                    "백테스트 데이터 검증 엔진 중단. profile={}, rule={}, recordCount={}, "
                            + "causeType={}, elapsedMs={}, errorLoggedByCaller=true",
                    command.profile(),
                    currentRule,
                    command.records().size(),
                    exception.getClass().getSimpleName(),
                    System.currentTimeMillis() - startedAt
            );
            throw exception;
        }

        Set<Integer> blockedIndexes = new HashSet<>();
        for (ValidationIssue issue : issues) {
            if (issue.severity().blocksRecord()) {
                blockedIndexes.add(issue.recordIndex());
            }
        }

        List<BackdataRecord> valid = new ArrayList<>();
        List<BackdataRecord> quarantined = new ArrayList<>();
        for (int index = 0; index < command.records().size(); index++) {
            if (blockedIndexes.contains(index)) {
                quarantined.add(command.records().get(index));
            } else {
                valid.add(command.records().get(index));
            }
        }

        Map<ValidationSeverity, Long> severityCounts =
                new EnumMap<>(ValidationSeverity.class);
        for (ValidationSeverity severity : ValidationSeverity.values()) {
            severityCounts.put(
                    severity,
                    issues.stream().filter(issue -> issue.severity() == severity).count()
            );
        }
        ValidationSummary summary = new ValidationSummary(
                command.records().size(),
                valid.size(),
                quarantined.size(),
                issues.size(),
                severityCounts
        );
        ValidationResult result = new ValidationResult(summary, valid, quarantined, issues);
        long elapsedMs = System.currentTimeMillis() - startedAt;
        boolean needsAttention = !result.isBacktestReady()
                || elapsedMs >= SLOW_VALIDATION_THRESHOLD_MS;
        if (needsAttention) {
            log.warn(
                    "백테스트 데이터 검증 엔진 종료. profile={}, result={}, totalRecords={}, "
                            + "validRecords={}, quarantinedRecords={}, issueCount={}, issuesBySeverity={}, "
                            + "appliedRuleCount={}, skippedRuleCount={}, elapsedMs={}, thresholdMs={}",
                    command.profile(),
                    result.isBacktestReady() ? "BACKTEST_READY" : "NOT_READY",
                    summary.totalRecords(),
                    summary.validRecords(),
                    summary.quarantinedRecords(),
                    summary.issueCount(),
                    summary.issuesBySeverity(),
                    appliedRuleCount,
                    skippedRuleCount,
                    elapsedMs,
                    SLOW_VALIDATION_THRESHOLD_MS
            );
        } else {
            log.info(
                    "백테스트 데이터 검증 엔진 종료. profile={}, result=BACKTEST_READY, "
                            + "totalRecords={}, validRecords={}, quarantinedRecords={}, issueCount={}, "
                            + "issuesBySeverity={}, appliedRuleCount={}, skippedRuleCount={}, elapsedMs={}",
                    command.profile(),
                    summary.totalRecords(),
                    summary.validRecords(),
                    summary.quarantinedRecords(),
                    summary.issueCount(),
                    summary.issuesBySeverity(),
                    appliedRuleCount,
                    skippedRuleCount,
                    elapsedMs
            );
        }
        return result;
    }
}
