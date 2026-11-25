package com.example.stockanalysis.screener.application;

import com.example.stockanalysis.engine.screener.*;
import com.example.stockanalysis.screener.domain.*;
import com.example.stockanalysis.screener.domain.ScreenerItem.ItemStatus;
import com.example.stockanalysis.screener.domain.ScreenerRun.Status;
import com.example.stockanalysis.shared.exception.BusinessException;
import com.example.stockanalysis.shared.exception.ErrorCode;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ScreenerService {
    private static final Logger log = LoggerFactory.getLogger(ScreenerService.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final ScreenerStore store;
    private final ScreenerDataProvider provider;
    private final DefaultScreenerEngine engine;
    private final Executor executor;
    private final Clock clock;
    private final Set<String> active = ConcurrentHashMap.newKeySet();
    public ScreenerService(ScreenerStore store, ScreenerDataProvider provider, DefaultScreenerEngine engine,
            @Qualifier("screenerExecutor") Executor executor, Clock clock) {
        this.store = store; this.provider = provider; this.engine = engine; this.executor = executor; this.clock = clock;
    }

    public synchronized ScreenerRun submit(ScreenerRunRequest request, String idempotencyKey) {
        String key = idempotencyKey == null ? UUID.randomUUID().toString() : idempotencyKey;
        if (!key.matches("[A-Za-z0-9._:-]{1,100}")) throw new IllegalArgumentException("invalid Idempotency-Key");
        Optional<ScreenerRun> previous = store.findByRequestKey(key);
        if (previous.isPresent()) {
            if (!previous.get().request().equals(request)) throw conflict("같은 Idempotency-Key에 다른 요청을 사용할 수 없습니다.");
            return previous.get();
        }
        LocalDateTime now = LocalDateTime.now(clock.withZone(SEOUL));
        LocalDateTime cutoff = request.asOf() == null ? now : request.asOf();
        if (cutoff.isAfter(now)) throw new IllegalArgumentException("asOf cannot be in the future");
        if (cutoff.toLocalDate().isBefore(now.toLocalDate()) && request.stockCodes().isEmpty())
            throw new IllegalArgumentException("historical collection requires explicit stockCodes; today's universe is not a historical universe");
        ScreenerRun run = new ScreenerRun(UUID.randomUUID().toString(), key, request, cutoff, engine.config(), DefaultScreenerEngine.VERSION, clock.instant(), null,
                Status.READY, 0, 0, 0, 0, null);
        store.create(run);
        enqueue(run);
        return get(run.runId());
    }
    public synchronized ScreenerRun resume(String id) {
        ScreenerRun run = get(id);
        requireVersion(run);
        if (active.contains(id)) throw conflict("이미 실행 중입니다.");
        if (run.status() == Status.COMPLETED) throw conflict("완료된 실행입니다. 새 실행을 요청해 주세요.");
        enqueue(run);
        return get(id);
    }
    /** Persisted once-per-KST-day job; only abandoned READY/RUNNING jobs are automatically resumed. */
    public synchronized ScreenerRun ensureScheduledSwing(LocalDateTime plannedAt) {
        var now = LocalDateTime.now(clock.withZone(SEOUL));
        if (!plannedAt.toLocalDate().equals(now.toLocalDate()) || plannedAt.isAfter(now))
            throw new IllegalArgumentException("scheduled swing must target today's elapsed start time");
        String key = "scheduled-swing:" + plannedAt.toLocalDate();
        var previous = store.findByRequestKey(key);
        if (previous.isPresent()) {
            var run = previous.get();
            if (!active.contains(run.runId()) && (run.status() == Status.READY || run.status() == Status.RUNNING)) {
                log.info("Resuming abandoned scheduled swing run: run={}", run.runId());
                return resume(run.runId());
            }
            // Failed/partial jobs stay visible for manual inspection, never retry every minute.
            return run;
        }
        var request = new ScreenerRunRequest(com.example.stockanalysis.engine.screener.ScreenerTypes.Mode.SWING,
                0, ScreenerRunRequest.Market.ALL, plannedAt, List.of());
        var run = submit(request, key);
        log.info("Scheduled swing screening registered: run={}, cutoff={}", run.runId(), plannedAt);
        return run;
    }
    // Single-server execution model; serialize recovery with submit/resume, including queued jobs.
    public synchronized ScreenerRun recover(String id) {
        ScreenerRun run = get(id);
        if (active.contains(id)) throw conflict("실제로 실행 또는 대기 중인 작업입니다. 중단 상태를 정리할 수 없습니다.");
        if (run.status() != Status.READY && run.status() != Status.RUNNING) return run;
        ScreenerRun recovered = run.progress(Status.INTERRUPTED, run.totalCount(), run.completedCount(),
                run.failedCount(), run.skippedCount(), "STALE_RUN_RECOVERED", clock.instant());
        store.saveRun(recovered);
        log.info("Screener stale run recovered: run={}", id);
        return recovered;
    }
    private void enqueue(ScreenerRun run) {
        active.add(run.runId());
        try { executor.execute(() -> execute(run)); }
        catch (RejectedExecutionException e) {
            active.remove(run.runId());
            store.saveRun(run.progress(Status.FAILED, run.totalCount(), run.completedCount(), run.failedCount(), run.skippedCount(), "QUEUE_FULL", clock.instant()));
            throw conflict("실행 대기열이 가득 찼습니다. 잠시 후 다시 요청해 주세요.");
        }
    }
    private void execute(ScreenerRun original) {
        ScreenerRun run = original;
        long collectionNanos = 0, engineNanos = 0;
        int engineCompleted = 0;
        List<ScreenerItem> items = new ArrayList<>();
        try {
            run = run.progress(Status.RUNNING, run.totalCount(), run.completedCount(), run.failedCount(), run.skippedCount(), null, null);
            store.saveRun(run);
            items.addAll(store.items(run.runId()));
            if (run.totalCount() == 0) {
                var universe = provider.universe(run.request().market());
                Set<String> requested = new HashSet<>(run.request().stockCodes());
                if (!requested.isEmpty() && !universe.stream().map(ScreenerItem.Stock::stockCode).collect(java.util.stream.Collectors.toSet()).containsAll(requested))
                    throw new IllegalArgumentException("UNKNOWN_STOCK_CODE");
                String runId = run.runId();
                String source = requested.isEmpty() ? "MARKET_UNIVERSE" : "EXPLICIT_CODES";
                items = new ArrayList<>(universe.stream().filter(s -> requested.isEmpty() || requested.contains(s.stockCode()))
                        .filter(s -> "ST".equals(s.stockType()))
                        .map(s -> ScreenerItem.pending(runId, s, source)).toList());
                // Non-ST and unknown types never become persisted items. Zero eligible stocks is a valid empty run.
                ScreenerRun initialized = progress(run, items, Status.RUNNING, null);
                store.initialize(initialized, items);
                run = initialized;
            }
            if (items.size() != run.totalCount()) throw new IllegalStateException("INCOMPLETE_UNIVERSE_SNAPSHOT");
            DefaultScreenerEngine runEngine = new DefaultScreenerEngine(run.config());
            for (int i = 0; i < items.size(); i++) {
                if (Thread.currentThread().isInterrupted()) throw new CancellationException();
                ScreenerItem item = items.get(i);
                if (item.status() == ItemStatus.SKIPPED || item.status() == ItemStatus.ANALYZED && item.sourceComplete()) continue;
                ScreenerInput input = null;
                ScreenerItem result;
                String phase = "COLLECTION";
                try {
                    long collectionStarted = System.nanoTime();
                    var data = provider.collect(item.stockCode(), run);
                    long itemCollectionNanos = System.nanoTime() - collectionStarted;
                    collectionNanos += itemCollectionNanos;
                    ScreenerInput candidateInput = data.input();
                    if (!candidateInput.stockCode().equals(item.stockCode()) || !candidateInput.timeframe().equals(run.request().timeframe()) || !candidateInput.asOf().equals(run.cutoff()))
                        throw new IllegalArgumentException("SOURCE_IDENTITY_MISMATCH");
                    input = candidateInput;
                    phase = "ENGINE";
                    long engineStarted = System.nanoTime();
                    var calculated = runEngine.analyze(input);
                    long itemEngineNanos = System.nanoTime() - engineStarted;
                    engineNanos += itemEngineNanos;
                    engineCompleted++;
                    log.debug("Screener engine completed: runId={}, stockCode={}, result=SUCCESS, collectionMs={}, engineMs={}",
                            run.runId(), item.stockCode(), itemCollectionNanos/1_000_000.0, itemEngineNanos/1_000_000.0);
                    result = new ScreenerItem(run.runId(), item.stockCode(), item.stockName(), item.market(), ItemStatus.ANALYZED,
                            item.observationSource(), data.sourceComplete(), calculated, data.warnings(), null);
                } catch (CancellationException e) { throw e;
                } catch (RuntimeException e) {
                    log.warn("Screener stock failed: run={}, code={}, phase={}, type={}", run.runId(), item.stockCode(), phase, e.getClass().getSimpleName());
                    result = new ScreenerItem(run.runId(), item.stockCode(), item.stockName(), item.market(), ItemStatus.FAILED,
                            item.observationSource(), false, null, List.of(), e instanceof IllegalArgumentException ? "INVALID_SOURCE_DATA" : "SOURCE_OR_ANALYSIS_FAILED");
                }
                store.saveItem(result, input);
                items.set(i, result);
                run = progress(run, items, Status.RUNNING, null);
                store.saveRun(run);
            }
            boolean errors = items.stream().anyMatch(i -> i.status() == ItemStatus.FAILED || i.status() == ItemStatus.ANALYZED && !i.sourceComplete());
            var completed = progress(run, items, errors ? Status.COMPLETED_WITH_ERRORS : Status.COMPLETED, null);
            store.saveRun(completed);
            log.info("Screener finished: run={}, status={}, total={}, analyzed={}, failed={}, skipped={}, engineCompletedThisAttempt={}, successfulCollectionMs={}, successfulEngineMs={}",
                    completed.runId(), completed.status(), completed.totalCount(), completed.completedCount(), completed.failedCount(), completed.skippedCount(),
                    engineCompleted, collectionNanos/1_000_000.0, engineNanos/1_000_000.0);
        } catch (RuntimeException e) {
            // A failed atomic initialization must remain retryable, never record an uncommitted universe size.
            if (run.totalCount() == 0) items.clear();
            Status status = e instanceof CancellationException ? Status.INTERRUPTED : Status.FAILED;
            log.warn("Screener run stopped: run={}, type={}", run.runId(), e.getClass().getSimpleName());
            String error = status == Status.INTERRUPTED ? "INTERRUPTED" : safeRunError(e);
            try { store.saveRun(progress(run, items, status, error)); }
            catch (RuntimeException storageError) { log.error("Screener status persistence failed: run={}", run.runId()); }
        } finally { active.remove(original.runId()); }
    }
    private ScreenerRun progress(ScreenerRun run, List<ScreenerItem> items, Status status, String error) {
        int completed = (int) items.stream().filter(i -> i.status() == ItemStatus.ANALYZED).count();
        int failed = (int) items.stream().filter(i -> i.status() == ItemStatus.FAILED).count();
        int skipped = (int) items.stream().filter(i -> i.status() == ItemStatus.SKIPPED).count();
        return run.progress(status, items.isEmpty() ? run.totalCount() : items.size(), completed, failed, skipped, error, status == Status.RUNNING ? null : clock.instant());
    }
    public ScreenerRun get(String id) { return store.find(id).orElseThrow(() -> missing("스크리너 실행을 찾을 수 없습니다.")); }
    public ScreenerItem item(String id, String code) {
        get(id);
        return store.item(id, code).orElseThrow(() -> missing("결과 종목을 찾을 수 없습니다."));
    }
    public ScreenerResult replay(String id, String code) {
        provider.validateStockType(code);
        ScreenerRun run = get(id);
        requireVersion(run);
        var input = store.input(id, code).orElseThrow(() -> missing("저장된 입력이 없습니다."));
        return new DefaultScreenerEngine(run.config()).analyze(input);
    }
    public ScreenerResult evaluate(ScreenerInput input) {
        provider.validateStockType(input.stockCode());
        return engine.analyze(input);
    }
    public ScreenerPage query(ScreenerQuery query) {
        ScreenerRun run = query.runId() == null ? store.latest(query.mode(), query.interval()).orElse(null) : get(query.runId());
        if (run == null) return new ScreenerPage(null, store.storageMode(), 0, query.page(), query.size(), Map.of(), List.of());
        if (run.request().mode() != query.mode() || run.request().interval() != query.interval())
            throw new IllegalArgumentException("run mode or interval does not match query");
        if (run.status() == Status.READY || run.status() == Status.RUNNING)
            return new ScreenerPage(run, store.storageMode(), 0, query.page(), query.size(), Map.of(), List.of());
        var page = store.query(run.runId(), query);
        if (page.universeCount() != run.totalCount())
            throw conflict("목록 조회용 데이터가 누락되었습니다. 09_screener_summary_backfill.sql 이관 및 실행 상태를 확인해 주세요.");
        return new ScreenerPage(run, store.storageMode(), page.totalElements(), query.page(), query.size(), page.stageCounts(),
                page.content(), page.recentCount(), page.watchCount());
    }
    public record ScreenerPage(ScreenerRun run, String storageMode, long totalElements, int page, int size, Map<Integer, Long> stageCounts,
            List<com.example.stockanalysis.screener.domain.ScreenerSummary> content, long recentCount, long watchCount) {
        public ScreenerPage(ScreenerRun run, String storageMode, long totalElements, int page, int size, Map<Integer, Long> stageCounts,
                List<com.example.stockanalysis.screener.domain.ScreenerSummary> content) {
            this(run, storageMode, totalElements, page, size, stageCounts, content, 0, 0);
        }
    }
    private void requireVersion(ScreenerRun run) {
        if (!DefaultScreenerEngine.VERSION.equals(run.engineVersion())) throw conflict("저장된 엔진 버전과 다릅니다. 해당 버전으로 재현해 주세요.");
    }
    private String safeRunError(RuntimeException exception) {
        String code = exception.getMessage();
        return code != null && Set.of("UNKNOWN_STOCK_CODE", "EMPTY_UNIVERSE", "UNIVERSE_PAGE_LIMIT", "INCOMPLETE_UNIVERSE_SNAPSHOT").contains(code) ? code : "RUN_FAILED";
    }
    private static BusinessException missing(String message) { return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, message); }
    private static BusinessException conflict(String message) { return new BusinessException(ErrorCode.SCREENER_CONFLICT, message); }
}
