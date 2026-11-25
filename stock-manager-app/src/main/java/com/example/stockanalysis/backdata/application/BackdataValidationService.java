package com.example.stockanalysis.backdata.application;

import com.example.stockanalysis.backdata.api.BackdataTrResponse;
import com.example.stockanalysis.backdata.api.BackdataBatchValidationRunResponse;
import com.example.stockanalysis.backdata.api.BackdataSourceCallResponse;
import com.example.stockanalysis.backdata.api.BackdataValidationStatus;
import com.example.stockanalysis.backdata.api.BackdataValidationRunResponse;
import com.example.stockanalysis.backdata.api.KiwoomBackdataBatchValidationRequest;
import com.example.stockanalysis.backdata.api.KiwoomBackdataQueryRequest;
import com.example.stockanalysis.backdata.api.KiwoomBackdataValidationRequest;
import com.example.stockanalysis.backdata.infrastructure.kiwoom.KiwoomBackdataCallResult;
import com.example.stockanalysis.backdata.infrastructure.kiwoom.KiwoomBackdataCatalog;
import com.example.stockanalysis.backdata.infrastructure.kiwoom.KiwoomBackdataClient;
import com.example.stockanalysis.backdata.infrastructure.kiwoom.KiwoomBackdataRecordMapper;
import com.example.stockanalysis.backdata.infrastructure.kiwoom.KiwoomBackdataTrDefinition;
import com.example.stockanalysis.backdata.validation.api.BackdataValidationEngine;
import com.example.stockanalysis.backdata.validation.model.BackdataRecord;
import com.example.stockanalysis.backdata.validation.model.ValidationCommand;
import com.example.stockanalysis.backdata.validation.model.ValidationIssue;
import com.example.stockanalysis.backdata.validation.model.ValidationResult;
import com.example.stockanalysis.backdata.validation.model.ValidationSummary;
import com.example.stockanalysis.kiwoom.client.KiwoomContinuation;
import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;
import com.example.stockanalysis.shared.exception.BusinessException;
import com.example.stockanalysis.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class BackdataValidationService {

    private static final Logger log = LoggerFactory.getLogger(BackdataValidationService.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final long SLOW_OPERATION_THRESHOLD_MS = 5_000L;

    private final KiwoomBackdataCatalog catalog;
    private final KiwoomBackdataClient client;
    private final KiwoomBackdataRecordMapper recordMapper;
    private final BackdataValidationEngine validationEngine;
    private final Clock clock;

    public BackdataValidationService(
            KiwoomBackdataCatalog catalog,
            KiwoomBackdataClient client,
            KiwoomBackdataRecordMapper recordMapper,
            BackdataValidationEngine validationEngine,
            Clock clock
    ) {
        this.catalog = catalog;
        this.client = client;
        this.recordMapper = recordMapper;
        this.validationEngine = validationEngine;
        this.clock = clock;
    }

    public List<BackdataTrResponse> supportedTrs() {
        List<BackdataTrResponse> supportedTrs = catalog.all().stream()
                .map(BackdataTrResponse::from)
                .toList();
        log.debug("백데이터 지원 전문 목록 조회 완료. trCount={}", supportedTrs.size());
        return supportedTrs;
    }

    public BackdataValidationRunResponse validate(
            KiwoomBackdataValidationRequest request
    ) {
        UUID runId = UUID.randomUUID();
        long startedAt = System.currentTimeMillis();
        String stage = "REQUEST_VALIDATION";
        BackdataValidationRunResponse response = null;
        try (MDC.MDCCloseable ignored = MDC.putCloseable("jobId", runId.toString())) {
            log.info(
                    "백데이터 단건 검증 시작. runId={}, apiId={}, profile={}, asOfDate={}, continuation={}",
                    runId,
                    request.apiId(),
                    request.profile(),
                    request.asOfDate(),
                    "Y".equalsIgnoreCase(request.continuation())
            );

            KiwoomBackdataTrDefinition definition = getDefinition(request.apiId());
            validateContinuation(request);
            Instant observedAt = clock.instant();
            LocalDate asOfDate = request.asOfDate() == null
                    ? observedAt.atZone(SEOUL).toLocalDate()
                    : request.asOfDate();

            stage = "EXTERNAL_API_CALL";
            KiwoomBackdataCallResult callResult = client.execute(
                    definition,
                    request.request(),
                    request.continuation(),
                    request.nextKey()
            );

            stage = "RECORD_MAPPING";
            List<BackdataRecord> records = recordMapper.map(
                    definition,
                    request.request(),
                    callResult.body(),
                    asOfDate,
                    observedAt
            );
            log.debug(
                    "백데이터 응답 매핑 완료. runId={}, apiId={}, recordCount={}",
                    runId,
                    definition.apiId(),
                    records.size()
            );

            stage = "DATA_VALIDATION";
            long engineStarted = System.nanoTime();
            ValidationResult result = validationEngine.validate(
                    new ValidationCommand(request.profile(), clock.instant(), records)
            );
            log.info("Validation engine completed: runId={}, result=SUCCESS, recordCount={}, quarantinedCount={}, issueCount={}, engineMs={}",
                    runId, records.size(), result.quarantinedRecords().size(), result.issues().size(),
                    (System.nanoTime()-engineStarted)/1_000_000.0);
            response = BackdataValidationRunResponse.of(
                    runId,
                    definition.apiId(),
                    definition.name(),
                    definition.schema().datasetType(),
                    request.profile(),
                    observedAt,
                    result,
                    callResult.body(),
                    callResult.continuation()
            );
            return response;
        } finally {
            try (MDC.MDCCloseable ignored = MDC.putCloseable("jobId", runId.toString())) {
                logSingleCompletion(
                        runId,
                        request.apiId(),
                        stage,
                        response,
                        System.currentTimeMillis() - startedAt
                );
            }
        }
    }

    public BackdataBatchValidationRunResponse validateBatch(
            KiwoomBackdataBatchValidationRequest request
    ) {
        UUID runId = UUID.randomUUID();
        long startedAt = System.currentTimeMillis();
        int succeededCalls = 0;
        String failedApiId = null;
        String stage = "QUERY_VALIDATION";
        BackdataBatchValidationRunResponse response = null;
        try (MDC.MDCCloseable ignored = MDC.putCloseable("jobId", runId.toString())) {
            log.info(
                    "백데이터 배치 검증 시작. runId={}, profile={}, targetCount={}",
                    runId,
                    request.profile(),
                    request.queries().size()
            );
            Instant observedAt = clock.instant();
            List<BackdataRecord> allRecords = new ArrayList<>();
            List<SourceCallDraft> sourceCallDrafts = new ArrayList<>();

            for (int queryIndex = 0; queryIndex < request.queries().size(); queryIndex++) {
                KiwoomBackdataQueryRequest query = request.queries().get(queryIndex);
                failedApiId = query.apiId();
                stage = "QUERY_VALIDATION";
                KiwoomBackdataTrDefinition definition = getDefinition(query.apiId());
                validateContinuation(query.continuation(), query.nextKey());
                LocalDate asOfDate = query.asOfDate() == null
                        ? observedAt.atZone(SEOUL).toLocalDate()
                        : query.asOfDate();

                stage = "EXTERNAL_API_CALL";
                KiwoomBackdataCallResult callResult = client.execute(
                        definition,
                        query.request(),
                        query.continuation(),
                        query.nextKey()
                );

                stage = "RECORD_MAPPING";
                List<BackdataRecord> records = recordMapper.map(
                        definition,
                        query.request(),
                        callResult.body(),
                        asOfDate,
                        observedAt
                );
                int recordOffset = allRecords.size();
                allRecords.addAll(records);
                sourceCallDrafts.add(new SourceCallDraft(
                        queryIndex,
                        definition.apiId(),
                        definition.name(),
                        definition.schema().datasetType(),
                        recordOffset,
                        records.size(),
                        callResult.body(),
                        callResult.continuation()
                ));
                succeededCalls++;
                failedApiId = null;
            }

            stage = "DATA_VALIDATION";
            long engineStarted = System.nanoTime();
            ValidationResult result = validationEngine.validate(
                    new ValidationCommand(request.profile(), clock.instant(), allRecords)
            );
            log.info("Validation engine completed: runId={}, result=SUCCESS, recordCount={}, quarantinedCount={}, issueCount={}, engineMs={}",
                    runId, allRecords.size(), result.quarantinedRecords().size(), result.issues().size(),
                    (System.nanoTime()-engineStarted)/1_000_000.0);
            List<BackdataSourceCallResponse> sourceResponses = sourceCallDrafts.stream()
                    .map(draft -> toSourceResponse(draft, result.issues()))
                    .toList();
            response = BackdataBatchValidationRunResponse.of(
                    runId,
                    request.profile(),
                    observedAt,
                    sourceResponses,
                    result
            );
            return response;
        } finally {
            try (MDC.MDCCloseable ignored = MDC.putCloseable("jobId", runId.toString())) {
                logBatchCompletion(
                        runId,
                        request.queries().size(),
                        succeededCalls,
                        failedApiId,
                        stage,
                        response,
                        System.currentTimeMillis() - startedAt
                );
            }
        }
    }

    private void logSingleCompletion(
            UUID runId,
            String apiId,
            String stage,
            BackdataValidationRunResponse response,
            long elapsedMs
    ) {
        String result = response == null ? "FAILURE" : "SUCCESS";
        ValidationSummary summary = response == null ? null : response.summary();
        int totalRecords = summary == null ? 0 : summary.totalRecords();
        int validRecords = summary == null ? 0 : summary.validRecords();
        int quarantinedRecords = summary == null ? 0 : summary.quarantinedRecords();
        int issueCount = summary == null ? 0 : summary.issueCount();
        boolean needsAttention = response == null
                || !response.backtestReady()
                || elapsedMs >= SLOW_OPERATION_THRESHOLD_MS;

        if (needsAttention) {
            log.warn(
                    "백데이터 단건 검증 종료. runId={}, apiId={}, result={}, stage={}, "
                            + "backtestReady={}, totalRecords={}, validRecords={}, quarantinedRecords={}, "
                            + "issueCount={}, issueCodes={}, failedItemIds={}, elapsedMs={}, thresholdMs={}",
                    runId,
                    apiId,
                    result,
                    stage,
                    response != null && response.backtestReady(),
                    totalRecords,
                    validRecords,
                    quarantinedRecords,
                    issueCount,
                    sampleIssueCodes(response),
                    sampleFailedItemIds(response),
                    elapsedMs,
                    SLOW_OPERATION_THRESHOLD_MS
            );
        } else {
            log.info(
                    "백데이터 단건 검증 종료. runId={}, apiId={}, result={}, stage={}, "
                            + "backtestReady={}, totalRecords={}, validRecords={}, quarantinedRecords={}, "
                            + "issueCount={}, elapsedMs={}",
                    runId,
                    apiId,
                    result,
                    stage,
                    true,
                    totalRecords,
                    validRecords,
                    quarantinedRecords,
                    issueCount,
                    elapsedMs
            );
        }
    }

    private void logBatchCompletion(
            UUID runId,
            int totalCalls,
            int succeededCalls,
            String failedApiId,
            String stage,
            BackdataBatchValidationRunResponse response,
            long elapsedMs
    ) {
        String result = response == null ? "FAILURE" : "SUCCESS";
        int failedCalls = response == null && failedApiId != null ? 1 : 0;
        int excludedCalls = Math.max(totalCalls - succeededCalls - failedCalls, 0);
        ValidationSummary summary = response == null ? null : response.summary();
        int totalRecords = summary == null ? 0 : summary.totalRecords();
        int validRecords = summary == null ? 0 : summary.validRecords();
        int quarantinedRecords = summary == null ? 0 : summary.quarantinedRecords();
        int issueCount = summary == null ? 0 : summary.issueCount();
        boolean needsAttention = response == null
                || !response.backtestReady()
                || elapsedMs >= SLOW_OPERATION_THRESHOLD_MS;

        if (needsAttention) {
            log.warn(
                    "백데이터 배치 검증 종료. runId={}, result={}, stage={}, totalCalls={}, "
                            + "succeededCalls={}, failedCalls={}, excludedCalls={}, failedApiId={}, "
                            + "backtestReady={}, totalRecords={}, validRecords={}, quarantinedRecords={}, "
                            + "issueCount={}, issueCodes={}, failedItemIds={}, elapsedMs={}, thresholdMs={}",
                    runId,
                    result,
                    stage,
                    totalCalls,
                    succeededCalls,
                    failedCalls,
                    excludedCalls,
                    failedApiId,
                    response != null && response.backtestReady(),
                    totalRecords,
                    validRecords,
                    quarantinedRecords,
                    issueCount,
                    sampleIssueCodes(response),
                    sampleFailedItemIds(response),
                    elapsedMs,
                    SLOW_OPERATION_THRESHOLD_MS
            );
        } else {
            log.info(
                    "백데이터 배치 검증 종료. runId={}, result={}, stage={}, totalCalls={}, "
                            + "succeededCalls={}, failedCalls={}, excludedCalls={}, backtestReady={}, "
                            + "totalRecords={}, validRecords={}, quarantinedRecords={}, issueCount={}, "
                            + "elapsedMs={}",
                    runId,
                    result,
                    stage,
                    totalCalls,
                    succeededCalls,
                    failedCalls,
                    excludedCalls,
                    true,
                    totalRecords,
                    validRecords,
                    quarantinedRecords,
                    issueCount,
                    elapsedMs
            );
        }
    }

    private List<String> sampleIssueCodes(BackdataValidationRunResponse response) {
        return response == null ? List.of() : sampleIssueCodes(response.issues());
    }

    private List<String> sampleIssueCodes(BackdataBatchValidationRunResponse response) {
        return response == null ? List.of() : sampleIssueCodes(response.issues());
    }

    private List<String> sampleIssueCodes(List<ValidationIssue> issues) {
        return issues.stream()
                .map(ValidationIssue::code)
                .filter(Objects::nonNull)
                .distinct()
                .limit(5)
                .toList();
    }

    private List<String> sampleFailedItemIds(BackdataValidationRunResponse response) {
        return response == null ? List.of() : sampleFailedItemIds(response.issues());
    }

    private List<String> sampleFailedItemIds(BackdataBatchValidationRunResponse response) {
        return response == null ? List.of() : sampleFailedItemIds(response.issues());
    }

    private List<String> sampleFailedItemIds(List<ValidationIssue> issues) {
        return issues.stream()
                .filter(issue -> issue.severity().blocksRecord())
                .map(issue -> Objects.toString(issue.apiId(), "-")
                        + ":" + Objects.toString(issue.stockCode(), "-")
                        + ":" + issue.recordIndex())
                .distinct()
                .limit(5)
                .toList();
    }

    private BackdataSourceCallResponse toSourceResponse(
            SourceCallDraft draft,
            List<ValidationIssue> allIssues
    ) {
        List<ValidationIssue> sourceIssues = allIssues.stream()
                .filter(issue -> belongsToSource(issue, draft))
                .map(issue -> localizeIssue(issue, draft.recordOffset()))
                .toList();
        return new BackdataSourceCallResponse(
                draft.queryIndex(),
                draft.apiId(),
                draft.apiName(),
                draft.datasetType(),
                draft.recordCount(),
                BackdataValidationStatus.from(draft.recordCount(), sourceIssues),
                sourceIssues,
                draft.kiwoomResponse(),
                draft.continuation()
        );
    }

    private boolean belongsToSource(ValidationIssue issue, SourceCallDraft draft) {
        if (draft.recordCount() == 0) {
            return issue.recordIndex() < 0;
        }
        return issue.recordIndex() >= draft.recordOffset()
                && issue.recordIndex() < draft.recordOffset() + draft.recordCount();
    }

    private ValidationIssue localizeIssue(ValidationIssue issue, int recordOffset) {
        int localIndex = issue.recordIndex() < 0
                ? issue.recordIndex()
                : issue.recordIndex() - recordOffset;
        return new ValidationIssue(
                issue.code(),
                issue.severity(),
                issue.message(),
                localIndex,
                issue.apiId(),
                issue.stockCode(),
                issue.businessDate(),
                issue.field()
        );
    }

    private KiwoomBackdataTrDefinition getDefinition(String apiId) {
        try {
            return catalog.getRequired(apiId);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    exception.getMessage(),
                    exception
            );
        }
    }

    private void validateContinuation(KiwoomBackdataValidationRequest request) {
        validateContinuation(request.continuation(), request.nextKey());
    }

    private void validateContinuation(String continuation, String nextKey) {
        if ("Y".equalsIgnoreCase(continuation)
                && (nextKey == null || nextKey.isBlank())) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "연속조회 요청에는 nextKey가 필요합니다."
            );
        }
    }

    private record SourceCallDraft(
            int queryIndex,
            String apiId,
            String apiName,
            com.example.stockanalysis.backdata.validation.model.BackdataDatasetType datasetType,
            int recordOffset,
            int recordCount,
            KiwoomResult kiwoomResponse,
            KiwoomContinuation continuation
    ) {
    }
}
