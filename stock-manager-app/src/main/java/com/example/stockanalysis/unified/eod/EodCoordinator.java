package com.example.stockanalysis.unified.eod;

import com.example.stockanalysis.analysis.application.model.CollectedAnalysisData;
import com.example.stockanalysis.analysis.domain.DataQualityStatus;
import com.example.stockanalysis.engine.model.AnalysisMode;
import com.example.stockanalysis.kiwoom.client.KiwoomApiException;
import com.example.stockanalysis.screener.application.ScreenerDataProvider;
import com.example.stockanalysis.screener.domain.ScreenerRunRequest.Market;
import com.example.stockanalysis.screener.domain.ScreenerItem.Stock;
import com.example.stockanalysis.shared.exception.*;
import com.example.stockanalysis.unified.application.*;
import com.example.stockanalysis.unified.scan.*;
import com.example.stockanalysis.unified.scan.UnifiedScanModels.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class EodCoordinator {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EodCoordinator.class);
    private final EodStore jobs;
    private final UnifiedScanStore scans;
    private final UnifiedAnalysisService analyzer;
    private final ScreenerDataProvider universe;
    private final TradingCalendar calendar;
    private final EodProperties settings;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final ScheduledExecutorService heartbeats;
    private final String owner = UUID.randomUUID().toString();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final String version;

    public EodCoordinator(EodStore jobs, UnifiedScanStore scans, UnifiedAnalysisService analyzer,
            ScreenerDataProvider universe, TradingCalendar calendar, EodProperties settings, ObjectMapper mapper,
            Clock clock, @Qualifier("eodHeartbeatExecutor") ScheduledExecutorService heartbeats) {
        this.jobs=jobs; this.scans=scans; this.analyzer=analyzer; this.universe=universe; this.calendar=calendar;
        this.settings=settings; this.mapper=mapper; this.clock=clock; this.heartbeats=heartbeats;
        version = "eod-v2-split/" + UnifiedScanService.VERSION + "/" + hash(settings.toString()).substring(0,12);
    }
    private LocalDateTime localNow() { return LocalDateTime.ofInstant(clock.instant(), SEOUL); }
    private long epoch(LocalDateTime date) { return date.atZone(SEOUL).toInstant().toEpochMilli(); }
    private Run get(String id) { return scans.find(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)); }

    public Run submit(Request request, String key) {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{1,100}")) throw new IllegalArgumentException("Idempotency-Key가 필요합니다.");
        Market market = request.market() == null ? Market.ALL : request.market();
        return jobs.locked(() -> {
            var old = scans.byKey(key);
            if (old.isPresent()) {
                boolean previousClose = old.get().engineVersion().startsWith(UnifiedScanService.PREVIOUS_CLOSE_PREFIX);
                if (!old.get().market().equals(market.name()) || previousClose != (request.priceMode() == PriceMode.PREVIOUS_CLOSE))
                    throw conflict("같은 요청 키에 다른 시장 또는 가격 기준을 사용할 수 없습니다.");
                return old.get();
            }
            if (request.priceMode() == PriceMode.PREVIOUS_CLOSE) {
                if (jobs.activePreviousClose().isPresent()) throw conflict("이미 전일까지 완료 일봉 분석이 실행 중이거나 대기 중입니다.");
                String id = UUID.randomUUID().toString();
                scans.create(id, key, market.name(), localNow().toLocalDate().minusDays(1),
                        UnifiedScanService.PREVIOUS_CLOSE_VERSION, clock.millis());
                return get(id);
            }
            requireWeekday();
            if (!jobs.activeRuns().isEmpty()) throw conflict("이미 종합분석이 실행 중이거나 대기 중입니다.");
            var day = calendar.latestDue(localNow());
            if (day.status() != TradingCalendar.Status.OPEN) throw conflict("거래일 달력을 확인할 수 없습니다. 달력 적용 범위를 확인하세요.");
            return create(day, market, key, "MANUAL");
        });
    }
    public void ensureScheduled() {
        if(!EodExecutionDays.isWeekday(clock))return;
        var now = localNow();
        jobs.locked(() -> {
            var today = calendar.day(now.toLocalDate());
            if (!now.toLocalTime().isBefore(EodProperties.START) && today.status() == TradingCalendar.Status.CLOSED)
                jobs.schedule(today.date(), "CLOSED", null, clock.millis());
            var day = calendar.latestDue(now);
            if (day.status() == TradingCalendar.Status.UNKNOWN) {
                jobs.schedule(day.date(), "CALENDAR_UNAVAILABLE", null, clock.millis()); return null;
            }
            var existing = jobs.sameDay(day.date(), "ALL");
            if (existing.isPresent()) { jobs.schedule(day.date(), "REGISTERED", existing.get(), clock.millis()); return null; }
            long scheduledAt = epoch(day.date().atTime(EodProperties.START));
            if (clock.millis() >= scheduledAt + settings.catchupWindow().toMillis()) {
                jobs.schedule(day.date(), "MISSED_WINDOW", null, clock.millis()); return null;
            }
            var run = create(day, Market.ALL, "unified-eod-" + day.date() + "-ALL-KRX", "SCHEDULED");
            jobs.schedule(day.date(), "REGISTERED", run.runId(), clock.millis()); return null;
        });
    }
    private Run create(TradingCalendar.Day day, Market market, String key, String trigger) {
        String id = UUID.randomUUID().toString(); long now = clock.millis();
        long cutoff = epoch(day.date().atTime(day.closeTime()));
        long eligible = cutoff + settings.settlementDelay().toMillis();
        scans.create(id, key, market.name(), day.date(), version, now);
        jobs.create(id, trigger, cutoff, eligible, Math.max(now, eligible) + settings.runTimeout().toMillis(),
                day.version(), EodProperties.POLICY, now);
        return get(id);
    }
    public Run resume(String id) {
        return jobs.locked(() -> {
            var run = get(id);
            if (run.engineVersion().startsWith(UnifiedScanService.PREVIOUS_CLOSE_PREFIX)) return resumePreviousClose(run);
            requireWeekday();
            if (run.eod() == null || !version.equals(run.engineVersion())) throw conflict("엔진 또는 실행 정책 버전이 변경되었습니다. 새 분석을 실행하세요.");
            if (run.status().equals("COMPLETED")) throw conflict("이미 완료된 실행입니다.");
            if (jobs.live() || jobs.activeRuns().contains(id)) throw conflict("실행 중이거나 자동 복구 대기 중입니다.");
            if (clock.millis() >= run.eod().deadlineEpoch()) throw conflict("실행 기한이 지났습니다. 새 분석을 실행하세요.");
            // Permanent failures and exhausted retries are not silently reset by repeated resume requests.
            int changed = jobs.requeueRetryableFailures(id, settings.maxAttempts());
            if (changed == 0) throw conflict("재시도 가능한 미처리 항목이 없습니다. 새 분석을 실행하세요.");
            scans.state(id, "READY", null, null); return get(id);
        });
    }
    private Run resumePreviousClose(Run run) {
        if (!UnifiedScanService.PREVIOUS_CLOSE_VERSION.equals(run.engineVersion()))
            throw conflict("엔진 버전이 변경되었습니다. 새 분석을 실행하세요.");
        if (!Set.of("FAILED", "COMPLETED_WITH_ERRORS", "INTERRUPTED").contains(run.status()))
            throw conflict("실패하거나 중단된 실행만 재개할 수 있습니다.");
        if (jobs.activePreviousClose().isPresent()) throw conflict("전일까지 완료 일봉 분석이 이미 실행 중입니다.");
        if (!run.initialized() && !run.baseDate().equals(localNow().toLocalDate().minusDays(1)))
            throw conflict("이전 날짜의 대상 종목이 저장되지 않았습니다. 새 분석을 실행하세요.");
        scans.requeueFailures(run.runId());
        scans.state(run.runId(), "READY", null, null);
        return get(run.runId());
    }
    public Run recover(String id) {
        return jobs.locked(() -> {
            var run=get(id);
            if (jobs.live()) throw conflict("실제로 실행 중인 작업은 중단 정리할 수 없습니다.");
            if (run.eod() != null && jobs.activeRuns().contains(id)) scans.state(id,"INTERRUPTED","RECOVERY_PENDING",null);
            if (run.eod() == null && Set.of("READY","RUNNING").contains(run.status())) {
                String reason = run.engineVersion().startsWith(UnifiedScanService.PREVIOUS_CLOSE_PREFIX) ? "STALE_RUN" : "LEGACY_ENGINE_UNSUPPORTED";
                scans.state(id, "INTERRUPTED", reason, clock.millis());
            }
            return get(id);
        });
    }

    /** One bounded item per invocation. Persisted waits never occupy a worker or sleep a thread. */
    public void tick() {
        boolean weekday = EodExecutionDays.isWeekday(clock);
        if (!busy.compareAndSet(false,true)) return;
        ScheduledFuture<?> heartbeat = null; boolean acquired = false;
        String runId = null;
        String phase = "ACQUIRE_LEASE";
        try {
            if (!weekday && jobs.activePreviousClose().isEmpty()) return;
            acquired = jobs.acquire(owner, settings.leaseDuration());
            if (!acquired) return;
            heartbeat = heartbeats.scheduleAtFixedRate(() -> {
                try { jobs.heartbeat(owner, settings.leaseDuration()); }
                catch (RuntimeException e) { log.warn("EOD heartbeat unavailable: type={}", e.getClass().getSimpleName()); }
            }, 10, 10, TimeUnit.SECONDS);
            phase = "LIST_ACTIVE_RUNS";
            var eodRuns = weekday ? jobs.activeRuns() : List.<String>of();
            runId = eodRuns.isEmpty() ? jobs.activePreviousClose().orElse(null) : eodRuns.get(0);
            if (runId == null) return;
            phase = "LOAD_RUN";
            var run = get(runId);
            phase = "PROCESS_RUN";
            if (run.eod() == null) processPreviousClose(run);
            else process(run);
        } catch (RuntimeException e) {
            log.warn("EOD worker will recover from persisted state: type={}, runId={}, phase={}, trace={}",
                    e.getClass().getSimpleName(), runId, phase, safeTrace(e));
        } finally {
            if (heartbeat != null) heartbeat.cancel(false);
            try { if (acquired) jobs.release(owner); }
            catch (RuntimeException e) { log.warn("EOD lease release deferred to expiry"); }
            busy.set(false);
        }
    }
    private void processPreviousClose(Run run) {
        String id = run.runId();
        if (!UnifiedScanService.PREVIOUS_CLOSE_VERSION.equals(run.engineVersion())) {
            jobs.owned(owner, () -> scans.state(id, "FAILED", "ENGINE_VERSION_CHANGED", clock.millis()));
            return;
        }
        if (!run.initialized()) {
            List<Stock> stocks;
            try {
                analyzer.holdingBatch(id);
                stocks = universe.universe(Market.valueOf(run.market())).stream()
                        .filter(stock -> "ST".equals(stock.stockType())).toList();
                if (stocks.stream().map(stock -> stock.stockCode()).distinct().count() != stocks.size())
                    throw new IllegalArgumentException("DUPLICATE_UNIVERSE");
            } catch (RuntimeException error) {
                log.warn("Previous close universe failed: run={}, type={}", id, error.getClass().getSimpleName());
                jobs.owned(owner, () -> scans.state(id, "FAILED", "UNIVERSE_LOAD_FAILED", clock.millis()));
                return;
            }
            jobs.owned(owner, () -> {
                scans.initialize(id, stocks, false);
                scans.state(id, "RUNNING", null, null);
            });
            return;
        }

        var next = scans.nextPreviousCloseItem(id);
        if (next.isEmpty()) {
            String status = run.failed() == 0 ? "COMPLETED" : "COMPLETED_WITH_ERRORS";
            jobs.owned(owner, () -> scans.state(id, status, null, clock.millis()));
            return;
        }
        var item = next.get();
        String phase = "COLLECT_AND_ANALYZE";
        try {
            var holding = analyzer.holdingBatch(id);
            var raw = analyzer.executeAt(item.stockCode(), run.baseDate().plusDays(1).atStartOfDay());
            var detail = holding == null ? raw : analyzer.personalize(raw, holding);
            phase = "SUMMARIZE";
            var summary = UnifiedScanService.summarize(item, detail);
            phase = "SAVE_RESULT";
            jobs.owned(owner, () -> {
                scans.complete(id, summary, detail);
                if ("READY".equals(run.status())) scans.state(id, "RUNNING", null, null);
            });
        } catch (CancellationException | EodStore.LeaseLost error) {
            throw error;
        } catch (RuntimeException error) {
            log.warn("Previous close item failed: run={}, stock={}, phase={}, trace={}", id, item.stockCode(), phase, safeTrace(error));
            jobs.owned(owner, () -> scans.fail(id, item.stockCode(), "COLLECTION_OR_ANALYSIS_FAILED"));
        }
    }

    private void requireWeekday(){
        if(!EodExecutionDays.isWeekday(clock))throw conflict("주말에는 전체 종합분석 실행과 재개를 중지합니다. 평일에 다시 시도하세요.");
    }
    private void process(Run run) {
        long now=clock.millis(); String id=run.runId();
        if (!version.equals(run.engineVersion())) { jobs.owned(owner, () -> scans.state(id,"FAILED","ENGINE_VERSION_CHANGED",now)); return; }
        if (now >= run.eod().deadlineEpoch()) {
            log.warn("EOD run deadline exceeded: runId={}, status=DATA_UNAVAILABLE, reason=RUN_DEADLINE_EXCEEDED, pending={}, retrying={}",
                    id, run.eod().pending(), run.eod().retrying());
            jobs.owned(owner, () -> { jobs.expireItems(id); finish(id,now,"RUN_DEADLINE_EXCEEDED"); }); return;
        }
        if (now < run.eod().eligibleEpoch()) {
            jobs.owned(owner, () -> scans.state(id,"WAITING_DATA","SETTLEMENT_DELAY",null)); return;
        }
        analyzer.holdingBatch(id);
        jobs.owned(owner, () -> {
            if (Set.of("RUNNING","INTERRUPTED").contains(run.status()))
                jobs.recordRecovery(id);
            scans.state(id,"RUNNING",null,null); jobs.start(id,now);
        });
        if (!run.initialized()) { initialize(run,now); return; }
        var item=jobs.nextItem(id,now);
        if (item.isEmpty()) { jobs.owned(owner, () -> finish(id,now,null)); return; }
        collect(run,item.get(),now);
    }
    private void initialize(Run run,long now) {
        String id=run.runId();
        if (jobs.nextInit(id)>now) { jobs.owned(owner, () -> scans.state(id,"RETRY_WAIT","UNIVERSE_RETRY",null)); return; }
        if (jobs.initAttempts(id)>=settings.maxAttempts()) { jobs.owned(owner, () -> scans.state(id,"FAILED","UNIVERSE_ATTEMPTS_EXHAUSTED",now)); return; }
        jobs.owned(owner, () -> jobs.initAttempt(id,now+settings.retryDelay().toMillis()));
        try {
            var stocks=universe.universe(Market.valueOf(run.market())).stream().filter(s -> "ST".equals(s.stockType())).toList();
            if (stocks.isEmpty() || stocks.stream().map(s -> s.stockCode()).distinct().count()!=stocks.size())
                throw new IllegalArgumentException("INVALID_UNIVERSE");
            jobs.owned(owner, () -> { scans.initialize(id,stocks); jobs.initializeItems(id); scans.state(id,"READY",null,null); });
        } catch (EodStore.LeaseLost | CancellationException e) { throw e; }
        catch (RuntimeException e) {
            log.warn("EOD universe failed: runId={}, trace={}", id, safeTrace(e));
            boolean retry=retryable(e) && jobs.initAttempts(id)<settings.maxAttempts();
            jobs.owned(owner, () -> scans.state(id,retry?"RETRY_WAIT":"FAILED",retry?"UNIVERSE_RETRY":"UNIVERSE_FAILED",retry?null:now));
        }
    }
    private void collect(Run run,EodStore.Attempt item,long now) {
        String id=run.runId();
        long started=System.nanoTime(), dailyMs=0, enrichmentMs=0, analysisMs=0, saveMs=0;
        String phase="READINESS";
        long deadline=item.readinessDeadline()==null ? Math.min(run.eod().deadlineEpoch(),now+settings.readinessTimeout().toMillis()) : item.readinessDeadline();
        if (now>=deadline || item.attempts()>=settings.maxAttempts()) {
            log.warn("EOD item ended: runId={}, stockCode={}, phase=READINESS, status={}, reason={}, attempts={}, deadlineEpoch={}",
                    id, item.code(), now>=deadline?"DATA_UNAVAILABLE":"FAILED",
                    now>=deadline?"DATA_READY_TIMEOUT":"RETRY_EXHAUSTED", item.attempts(), deadline);
            jobs.owned(owner, () -> {
                jobs.failure(id,item.code(),now>=deadline?"DATA_READY_TIMEOUT":"RETRY_EXHAUSTED",false,0,now>=deadline);
                finish(id,now,null);
            }); return;
        }
        jobs.owned(owner, () -> jobs.attempt(id,item,now,deadline));
        var previousMdc = org.slf4j.MDC.getCopyOfContextMap();
        try {
            org.slf4j.MDC.put("jobId", id);
            org.slf4j.MDC.put("stockCode", item.code());
            long stage=System.nanoTime();
            var collected=analyzer.collectEod(item.code(),run.baseDate());
            dailyMs=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-stage);
            long received=clock.millis();
            var input=collected.input();
            if (!item.code().equals(input.stock().stockCode()) || input.mode()!=AnalysisMode.SWING || !run.baseDate().equals(input.baseDate()))
                throw new IllegalArgumentException("INPUT_IDENTITY_MISMATCH");
            var latest=input.dailyCandles().stream().max(Comparator.comparing(com.example.stockanalysis.engine.model.DailyCandle::businessDate)).orElseThrow();
            if (!latest.businessDate().equals(run.baseDate())) {
                waitData(run,item,null,received,deadline,"PRICE_DATE_NOT_READY"); return;
            }
            if (collected.dataQualityStatus()==DataQualityStatus.INVALID) throw new IllegalArgumentException("INVALID_REQUIRED_DATA");
            // Check every supplied adjusted candle, so corrections/splits cannot be mistaken for stable input.
            var ordered=input.dailyCandles().stream().sorted(Comparator.comparing(com.example.stockanalysis.engine.model.DailyCandle::businessDate)).toList();
            var dailySource=collected.sources().stream().filter(s -> "ka10081".equals(s.apiId()))
                    .flatMap(s -> s.records().stream())
                    .map(r -> json(Arrays.asList(r.apiId(),r.stockCode(),r.businessDate(),new TreeMap<>(r.values())))).sorted().toList();
            String fingerprint=hash(json(Map.of("candles",ordered,"dailyRecords",dailySource)));
            if (!fingerprint.equals(item.hash()) || item.firstSample()==null || received-item.firstSample()<settings.stabilityInterval().toMillis()) {
                waitData(run,item,fingerprint,received,deadline,"VERIFYING_DAILY_STABILITY"); return;
            }
            if (received>=deadline) { waitData(run,item,null,received,deadline,"DATA_READY_TIMEOUT"); return; }
            phase="ENRICHMENT"; stage=System.nanoTime();
            var enriched=analyzer.completeEod(collected);
            // Enrichment must never replace the daily sample which passed stability verification.
            if (!item.code().equals(enriched.input().stock().stockCode()) || enriched.input().mode()!=AnalysisMode.SWING
                    || !run.baseDate().equals(enriched.input().baseDate()) || enriched.dataQualityStatus()==DataQualityStatus.INVALID
                    || !collected.input().dailyCandles().equals(enriched.input().dailyCandles())
                    || !collected.sources().stream().filter(s -> "ka10081".equals(s.apiId())).toList()
                            .equals(enriched.sources().stream().filter(s -> "ka10081".equals(s.apiId())).toList()))
                throw new IllegalArgumentException("VERIFIED_DAILY_SAMPLE_CHANGED");
            collected=enriched;
            received=clock.millis();
            enrichmentMs=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-stage);
            if (received>=deadline) { waitData(run,item,null,received,deadline,"DATA_READY_TIMEOUT"); return; }
            phase="ANALYSIS"; stage=System.nanoTime();
            var capture=analyzer.analyzeEod(item.code(),run.baseDate(),LocalDateTime.ofInstant(Instant.ofEpochMilli(received),SEOUL),
                    collected,LocalDateTime.ofInstant(Instant.ofEpochMilli(run.eod().priceCutoffEpoch()),SEOUL));
            var holdingSnapshot=analyzer.holdingBatch(id);
            var result=holdingSnapshot==null?capture.analysis():analyzer.personalize(capture.analysis(),holdingSnapshot);
            if (!result.result().priceDate().equals(run.baseDate())) throw new IllegalArgumentException("RESULT_DATE_MISMATCH");
            long analyzed=clock.millis();
            analysisMs=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-stage);
            if (analyzed>=deadline) { waitData(run,item,null,analyzed,deadline,"DATA_READY_TIMEOUT"); return; }
            var warnings=new ArrayList<>(result.collectionWarnings());
            warnings.add("일봉은 마감 대기 후 두 번의 동일 가격 이력으로 확인했습니다. API 확정 플래그가 없으므로 이후 정정될 수 있습니다.");
            var detail=new UnifiedAnalysisResponse(result.result(),result.collectionQuality(),warnings,result.volume(),result.tradingAmountMillionWon(),result.priceHistory(),
                    new UnifiedAnalysisResponse.EodMetadata(run.eod().priceBasis(),run.baseDate(),Instant.ofEpochMilli(run.eod().priceCutoffEpoch()),
                            Instant.ofEpochMilli(now),Instant.ofEpochMilli(received),Instant.ofEpochMilli(analyzed),run.eod().readinessPolicy(),run.eod().calendarVersion()), result.tradeJudgment(), result.holdingAnalysis());
            // Normalized inputs and request/receipt provenance, without provider credentials or polymorphic raw DTOs.
            var sources=collected.sources().stream().map(s -> Map.of("apiId",s.apiId(),"request",s.request(),"records",s.records(),
                    "requestedAt",s.requestedAt(),"receivedAt",s.receivedAt())).toList();
            String snapshot=json(Map.of("input",collected.input(),"sources",sources,"warnings",collected.warnings(),"quality",collected.dataQualityStatus(),
                    "firstStableSampleEpoch",item.firstSample(),"dailyFingerprint",fingerprint,"engineVersion",run.engineVersion(),"metadata",detail.eod()));
            var summary=UnifiedScanService.summarize(scans.getItem(id,item.code()),detail);
            phase="SAVE"; stage=System.nanoTime();
            long collectionCompleted=received;
            jobs.owned(owner, () -> {
                scans.complete(id,summary,detail); jobs.snapshot(id,item.code(),snapshot,collectionCompleted,analyzed); jobs.touch(id,analyzed); finish(id,analyzed,null);
            });
            saveMs=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-stage); phase="COMPLETED";
        } catch (EodStore.LeaseLost | CancellationException e) { throw e; }
        catch (RuntimeException e) {
            boolean retry=retryable(e) && item.attempts()+1<settings.maxAttempts();
            long next=clock.millis()+settings.retryDelay().toMillis()*(1L<<Math.min(item.attempts(),6));
            String error=e instanceof IllegalArgumentException ? "INVALID_REQUIRED_DATA" : retry?"COLLECTION_RETRY":"COLLECTION_OR_ANALYSIS_FAILED";
            log.warn("EOD collection or analysis failed: runId={}, stockCode={}, phase={}, status={}, reason={}, attempts={}, retryable={}, nextTryEpoch={}, trace={}",
                    id, item.code(), phase, retry?"RETRY_WAIT":"FAILED", error, item.attempts()+1, retry, retry?next:null, safeTrace(e));
            jobs.owned(owner, () -> { jobs.failure(id,item.code(),error,retry,next,false); finish(id,clock.millis(),null); });
        } finally {
            if (previousMdc == null) org.slf4j.MDC.clear();
            else org.slf4j.MDC.setContextMap(previousMdc);
            log.info("EOD item timing: runId={}, stockCode={}, phase={}, dailyMs={}, enrichmentMs={}, analysisMs={}, saveMs={}, totalMs={}",
                    id,item.code(),phase,dailyMs,enrichmentMs,analysisMs,saveMs,TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started));
        }
    }
    private void waitData(Run run,EodStore.Attempt item,String fingerprint,long now,long deadline,String error) {
        jobs.owned(owner, () -> {
            if (now>=deadline) jobs.failure(run.runId(),item.code(),"DATA_READY_TIMEOUT",false,0,true);
            else jobs.waitForData(run.runId(),item,fingerprint,now,Math.min(deadline,now+settings.stabilityInterval().toMillis()),error);
            finish(run.runId(),now,null);
        });
        if (now>=deadline) {
            log.warn("EOD item ended: runId={}, stockCode={}, status=DATA_UNAVAILABLE, reason=DATA_READY_TIMEOUT, trigger={}, deadlineEpoch={}",
                    run.runId(), item.code(), error, deadline);
        } else {
            log.info("EOD item waiting: runId={}, stockCode={}, status=RETRY_WAIT, reason={}, nextTryEpoch={}, deadlineEpoch={}",
                    run.runId(), item.code(), error, Math.min(deadline,now+settings.stabilityInterval().toMillis()), deadline);
        }
    }
    private void finish(String id,long now,String error) {
        var result=get(id); jobs.touch(id,now);
        if (result.eod().pending()+result.eod().retrying()>0) { scans.state(id,"RETRY_WAIT",null,null); return; }
        boolean errors=result.failed()+result.eod().dataUnavailable()>0;
        String status=!result.initialized() || result.completed()==0 && (errors || result.total()==0) ? "FAILED" : errors?"COMPLETED_WITH_ERRORS":"COMPLETED";
        scans.state(id,status,error!=null?error:status.equals("FAILED")?"NO_VALID_RESULTS":null,now);
    }
    static boolean retryable(Throwable error) {
        if (error instanceof KiwoomApiException e) {
            Integer status=e.getExternalStatusCode();
            String code=e.getFailureCode();
            return status!=null && (status==429 || status>=500)
                    || code!=null && Set.of("EMPTY_RESPONSE","RETURN_CODE_1700","RETURN_CODE_-200","RETURN_CODE_-205").contains(code);
        }
        if (error instanceof org.springframework.web.client.HttpStatusCodeException e)
            return e.getStatusCode().value()==429 || e.getStatusCode().is5xxServerError();
        return error instanceof org.springframework.web.client.ResourceAccessException
                || error instanceof org.springframework.dao.TransientDataAccessException;
    }
    private String json(Object value) {
        try { return mapper.writeValueAsString(value); } catch (Exception e) { throw new IllegalArgumentException("SNAPSHOT_SERIALIZATION_FAILED",e); }
    }
    // Keep diagnostic locations, but never log provider messages, request bodies or credentials.
    static String safeTrace(Throwable error) {
        var trace=new StringJoiner(" | caused by ");
        var seen=Collections.newSetFromMap(new IdentityHashMap<Throwable,Boolean>());
        for(int depth=0;error!=null && depth<4 && seen.add(error);depth++,error=error.getCause()) {
            var frames=new StringJoiner(" <- ");
            Arrays.stream(error.getStackTrace()).limit(20).forEach(frame->frames.add(frame.toString()));
            String code = error instanceof BusinessException business ? " code="+business.getErrorCode().name()
                    : error instanceof java.sql.SQLException sql ? " sqlErrorCode="+sql.getErrorCode() : "";
            trace.add(error.getClass().getName()+code+": "+frames);
        }
        return trace.toString();
    }
    private static String hash(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static BusinessException conflict(String message) { return new BusinessException(ErrorCode.UNIFIED_ANALYSIS_CONFLICT,message); }
}
