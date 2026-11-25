package com.example.stockanalysis.screener;

import com.example.stockanalysis.engine.screener.*;
import com.example.stockanalysis.engine.screener.ScreenerTypes.*;
import com.example.stockanalysis.screener.application.*;
import com.example.stockanalysis.screener.domain.*;
import com.example.stockanalysis.screener.domain.ScreenerItem.Stock;
import com.example.stockanalysis.screener.domain.ScreenerRun.Status;
import com.example.stockanalysis.screener.infrastructure.InMemoryScreenerStore;
import com.example.stockanalysis.shared.exception.BusinessException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ScreenerServiceTest {
    @Test void scheduledJobIsDailyPersistedAndSwingOnlyIncludingRestart() {
        var batchClock = Clock.fixed(Instant.parse("2026-09-14T21:00:00Z"), ZoneOffset.UTC);
        var batchStore = new InMemoryScreenerStore();
        var batchProvider = mock(ScreenerDataProvider.class);
        when(batchProvider.universe(any())).thenReturn(List.of());
        var batch = new ScreenerService(batchStore,batchProvider,engine,Runnable::run,batchClock);
        var planned = LocalDateTime.parse("2026-09-15T04:00:00");
        var first = batch.ensureScheduledSwing(planned);
        assertThat(first.request().mode()).isEqualTo(Mode.SWING);
        assertThat(first.request().interval()).isZero();
        assertThat(first.request().market()).isEqualTo(ScreenerRunRequest.Market.ALL);
        assertThat(first.request().stockCodes()).isEmpty();
        assertThat(first.cutoff()).isEqualTo(planned);
        assertThat(first.requestKey()).isEqualTo("scheduled-swing:2026-09-15");
        var restarted = new ScreenerService(batchStore,batchProvider,engine,Runnable::run,batchClock);
        assertThat(restarted.ensureScheduledSwing(planned.plusHours(1)).runId()).isEqualTo(first.runId());
        verify(batchProvider,times(1)).universe(any());
    }
    @Test void scheduledJobDoesNotDuplicateQueuedWorkButRecoversAbandonedQueue() {
        var batchClock = Clock.fixed(Instant.parse("2026-09-14T21:00:00Z"), ZoneOffset.UTC);
        var batchStore = new InMemoryScreenerStore();
        var tasks = new ArrayList<Runnable>();
        var batch = new ScreenerService(batchStore,provider,engine,tasks::add,batchClock);
        var planned = LocalDateTime.parse("2026-09-15T04:00:00");
        var first = batch.ensureScheduledSwing(planned);
        batch.ensureScheduledSwing(planned);
        assertThat(tasks).hasSize(1);
        var recoveredTasks = new ArrayList<Runnable>();
        var restarted = new ScreenerService(batchStore,provider,engine,recoveredTasks::add,batchClock);
        assertThat(restarted.ensureScheduledSwing(planned).runId()).isEqualTo(first.runId());
        assertThat(recoveredTasks).hasSize(1);
        restarted.ensureScheduledSwing(planned);
        assertThat(recoveredTasks).hasSize(1);
    }
    @Test void failedScheduledBatchIsNotRetriedEveryMinute() {
        var batchClock = Clock.fixed(Instant.parse("2026-09-14T21:00:00Z"), ZoneOffset.UTC);
        var batchStore = new InMemoryScreenerStore();
        var batch = new ScreenerService(batchStore,provider,engine,task -> { throw new RejectedExecutionException(); },batchClock);
        var planned = LocalDateTime.parse("2026-09-15T04:00:00");
        assertThatThrownBy(() -> batch.ensureScheduledSwing(planned)).isInstanceOf(BusinessException.class);
        assertThat(batch.ensureScheduledSwing(planned).status()).isEqualTo(Status.FAILED);
        assertThatThrownBy(() -> batch.ensureScheduledSwing(planned.plusDays(1))).isInstanceOf(IllegalArgumentException.class);
    }
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-13T01:00:00Z"), ZoneOffset.UTC);
    private final ScreenerDataProvider provider = mock(ScreenerDataProvider.class);
    private final InMemoryScreenerStore store = new InMemoryScreenerStore();
    private final DefaultScreenerEngine engine = new DefaultScreenerEngine(ScreenerConfig.defaults());
    private final ScreenerService service = new ScreenerService(store, provider, engine, Runnable::run, clock);
    private ScreenerRunRequest request() { return new ScreenerRunRequest(Mode.SWING, 0, ScreenerRunRequest.Market.ALL, null, List.of()); }
    private Stock stock(String code, String state) { return new Stock(code, code, "KOSPI", state, null, "ST"); }
    private ScreenerDataProvider.Collected data(String code, ScreenerRun run) {
        return new ScreenerDataProvider.Collected(new ScreenerInput(code, run.request().timeframe(), run.cutoff(), List.of(), List.of()), true, List.of());
    }
    @Test void failureDoesNotStopOtherStocksAndResumeReusesSuccessfulSnapshots() {
        when(provider.universe(any())).thenReturn(List.of(stock("000001", ""), stock("000002", ""), stock("000003", "거래정지")));
        when(provider.collect(eq("000001"), any())).thenAnswer(inv -> data(inv.getArgument(0), inv.getArgument(1)));
        when(provider.collect(eq("000002"), any())).thenThrow(new IllegalArgumentException("malformed"));
        var run = service.submit(request(), "job1");
        assertThat(run.status()).isEqualTo(Status.COMPLETED_WITH_ERRORS);
        assertThat(run.completedCount()).isEqualTo(1);
        assertThat(run.failedCount()).isEqualTo(1);
        assertThat(run.skippedCount()).isEqualTo(1);
        var replay = service.replay(run.runId(), "000001");
        assertThat(replay).isEqualTo(service.item(run.runId(), "000001").result());
        assertThat(service.submit(request(), "job1").runId()).isEqualTo(run.runId());
        when(provider.collect(eq("000002"), any())).thenAnswer(inv -> data(inv.getArgument(0), inv.getArgument(1)));
        var resumed = service.resume(run.runId());
        assertThat(resumed.status()).isEqualTo(Status.COMPLETED);
        assertThat(resumed.cutoff()).isEqualTo(run.cutoff());
        assertThat(resumed.completedCount()).isEqualTo(2);
        verify(provider, times(1)).universe(any());
        verify(provider, times(1)).collect(eq("000001"), any());
        verify(provider, never()).collect(eq("000003"), any());
    }
    @Test void sameIdempotencyKeyCannotChangePayloadAndFutureOrHistoricalUniverseAreRejected() {
        when(provider.universe(any())).thenReturn(List.of(stock("000001", "")));
        when(provider.collect(any(), any())).thenAnswer(inv -> data(inv.getArgument(0), inv.getArgument(1)));
        service.submit(request(), "job1");
        assertThatThrownBy(() -> service.submit(new ScreenerRunRequest(Mode.SHORT_TERM, 5, null, null, null), "job1")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.submit(new ScreenerRunRequest(null, null, null, LocalDateTime.of(2027,1,1,0,0), null), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.submit(new ScreenerRunRequest(null, null, null, LocalDateTime.of(2025,1,1,0,0), null), null)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void unknownExplicitCodeFailsBeforeAnyChartCalls() {
        when(provider.universe(any())).thenReturn(List.of(stock("000001", "")));
        var run = service.submit(new ScreenerRunRequest(null, null, null, null, List.of("000002")), null);
        assertThat(run.status()).isEqualTo(Status.FAILED);
        assertThat(run.errorCode()).isEqualTo("UNKNOWN_STOCK_CODE");
        assertThat(store.items(run.runId())).isEmpty();
        verify(provider, never()).collect(any(), any());
    }
    @Test void queueRejectionPersistsFailureAndCanBeResumed() {
        var busy = new ScreenerService(store, provider, engine, task -> { throw new RejectedExecutionException(); }, clock);
        assertThatThrownBy(() -> busy.submit(request(), "full")).isInstanceOf(BusinessException.class);
        var run = store.findByRequestKey("full").orElseThrow();
        assertThat(run.status()).isEqualTo(Status.FAILED);
        assertThat(run.errorCode()).isEqualTo("QUEUE_FULL");
        when(provider.universe(any())).thenReturn(List.of(stock("000001", "")));
        when(provider.collect(any(), any())).thenAnswer(inv -> data(inv.getArgument(0), inv.getArgument(1)));
        assertThat(service.resume(run.runId()).status()).isEqualTo(Status.COMPLETED);
    }
    @Test void recoveryPreservesSnapshotAndCanResumeAfterRestart() {
        when(provider.universe(any())).thenReturn(List.of(stock("000001", "")));
        when(provider.collect(any(), any())).thenAnswer(inv -> data(inv.getArgument(0), inv.getArgument(1)));
        var completed = service.submit(request(), "recover");
        var before = store.items(completed.runId());
        store.saveRun(completed.progress(Status.RUNNING, 1, 1, 0, 0, null, null));
        var recovered = service.recover(completed.runId());
        assertThat(recovered.status()).isEqualTo(Status.INTERRUPTED);
        assertThat(recovered.finishedAt()).isEqualTo(clock.instant());
        assertThat(recovered.completedCount()).isEqualTo(1);
        assertThat(recovered.errorCode()).isEqualTo("STALE_RUN_RECOVERED");
        assertThat(store.items(completed.runId())).isEqualTo(before);
        assertThat(service.recover(completed.runId())).isEqualTo(recovered);
        assertThat(service.resume(completed.runId()).status()).isEqualTo(Status.COMPLETED);
        verify(provider, times(1)).collect(any(), any());
        assertThat(service.recover(completed.runId()).status()).isEqualTo(Status.COMPLETED);
    }
    @Test void recoveryRejectsLiveWorkerAndRecoversOrphanedReadyJob() {
        List<Runnable> queue = new ArrayList<>();
        var queued = new ScreenerService(store, provider, engine, queue::add, clock);
        var run = queued.submit(request(), "live");
        assertThatThrownBy(() -> queued.recover(run.runId())).isInstanceOf(BusinessException.class);
        assertThat(queued.get(run.runId()).status()).isEqualTo(Status.READY);
        // A newly started service has no worker for this old READY record.
        assertThat(service.recover(run.runId()).status()).isEqualTo(Status.INTERRUPTED);
        assertThatThrownBy(() -> service.recover("missing")).isInstanceOf(BusinessException.class);
    }
    @Test void recoveryRejectsRunningWorker() {
        when(provider.universe(any())).thenAnswer(inv -> {
            var run = store.findByRequestKey("running-recovery").orElseThrow();
            assertThat(run.status()).isEqualTo(Status.RUNNING);
            assertThatThrownBy(() -> service.recover(run.runId())).isInstanceOf(BusinessException.class);
            return List.of();
        });
        assertThat(service.submit(request(), "running-recovery").status()).isEqualTo(Status.COMPLETED);
    }
    @Test void pendingJobIsNonBlockingAndCannotBeResumedConcurrently() {
        List<Runnable> queue = new ArrayList<>();
        var queued = new ScreenerService(store, provider, engine, queue::add, clock);
        var run = queued.submit(request(), "pending");
        assertThat(run.status()).isEqualTo(Status.READY);
        verifyNoInteractions(provider);
        assertThatThrownBy(() -> queued.resume(run.runId())).isInstanceOf(BusinessException.class);
        assertThat(queue).hasSize(1);
    }
    @Test void truncatedSourceIsRetryableButNotSilentlyCompleted() {
        when(provider.universe(any())).thenReturn(List.of(stock("000001", "")));
        when(provider.collect(any(), any())).thenAnswer(inv -> {
            var input = data(inv.getArgument(0), inv.getArgument(1)).input();
            return new ScreenerDataProvider.Collected(input, false, List.of("SOURCE_WINDOW_TRUNCATED"));
        });
        var run = service.submit(request(), null);
        assertThat(run.status()).isEqualTo(Status.COMPLETED_WITH_ERRORS);
        assertThat(service.item(run.runId(), "000001").sourceComplete()).isFalse();
    }
    @Test void latestAndPaginationAreStableAndInvalidFiltersFail() {
        when(provider.universe(any())).thenReturn(List.of(stock("000002", ""), stock("000001", "")));
        when(provider.collect(any(), any())).thenAnswer(inv -> data(inv.getArgument(0), inv.getArgument(1)));
        var run = service.submit(request(), null);
        var query = new ScreenerQuery(null, null, null, null, null, null, null, null, null, null, null, null, null, "stage", true, 0, 1, null, null, null, null, null);
        var page = service.query(query);
        assertThat(page.run().runId()).isEqualTo(run.runId());
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content().get(0).stockCode()).isEqualTo("000001");
        assertThatThrownBy(() -> new ScreenerQuery(null,null,null,null,7,null,null,null,null,null,null,null,null,null,null,0,101,null,null,null,null,null)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void missingProjectionIsReportedInsteadOfSilentlyReturningAnEmptyRun() {
        when(provider.universe(any())).thenReturn(List.of(stock("000001", "")));
        when(provider.collect(any(), any())).thenAnswer(inv -> data(inv.getArgument(0), inv.getArgument(1)));
        var run = service.submit(request(), null);
        var incomplete = mock(ScreenerStore.class);
        when(incomplete.latest(Mode.SWING, 0)).thenReturn(Optional.of(run));
        when(incomplete.query(eq(run.runId()), any())).thenReturn(new ScreenerStore.ResultPage(0,Map.of(),0,0,List.of(),0));
        var reader = new ScreenerService(incomplete, provider, engine, Runnable::run, clock);
        var q = new ScreenerQuery(null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null);
        assertThatThrownBy(() -> reader.query(q)).isInstanceOf(BusinessException.class).hasMessageContaining("backfill");
        verify(incomplete, never()).items(any());
    }
    @Test void activeRunReturnsStatusWithoutReadingPartialList() {
        var queued = new ScreenerService(store, provider, engine, task -> {}, clock);
        var run = queued.submit(request(), "active-query");
        var q = new ScreenerQuery(run.runId(),null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null);
        var result = queued.query(q);
        assertThat(result.run().status()).isEqualTo(Status.READY);
        assertThat(result.content()).isEmpty();
    }
    @Test void mismatchedSourceIdentityCannotBecomeReplayInput() {
        when(provider.universe(any())).thenReturn(List.of(stock("000001", "")));
        when(provider.collect(any(), any())).thenAnswer(inv -> data("000002", inv.getArgument(1)));
        var run = service.submit(request(), null);
        assertThat(run.failedCount()).isEqualTo(1);
        assertThat(store.input(run.runId(), "000001")).isEmpty();
    }
    @Test void nonStAndUnclassifiedStocksAreDiscardedBeforePersistenceAndChartCollection() {
        when(provider.universe(any())).thenReturn(List.of(stock("005930", ""),
                new Stock("069500", "ETF", "KOSPI", "", null, "ETF"),
                new Stock("999999", "미분류", "KOSPI", "", null, null)));
        when(provider.collect(eq("005930"), any())).thenAnswer(inv -> data(inv.getArgument(0), inv.getArgument(1)));
        var run = service.submit(request(), null);
        assertThat(run.completedCount()).isEqualTo(1);
        assertThat(run.totalCount()).isEqualTo(1);
        assertThat(run.skippedCount()).isZero();
        assertThat(store.items(run.runId())).extracting(ScreenerItem::stockCode).containsExactly("005930");
        assertThat(store.input(run.runId(), "069500")).isEmpty();
        assertThat(store.input(run.runId(), "999999")).isEmpty();
        verify(provider, never()).collect(eq("069500"), any());
        verify(provider, never()).collect(eq("999999"), any());
    }
    @Test void explicitNonStOnlyRequestCompletesWithoutPersistingAnyStockItems() {
        when(provider.universe(any())).thenReturn(List.of(new Stock("069500", "ETF", "KOSPI", "", null, "EF")));
        var run = service.submit(new ScreenerRunRequest(null,null,null,null,List.of("069500")), null);
        assertThat(run.status()).isEqualTo(Status.COMPLETED);
        assertThat(run.totalCount()).isZero();
        assertThat(run.skippedCount()).isZero();
        assertThat(store.items(run.runId())).isEmpty();
        verify(provider, never()).collect(any(), any());
    }
    @Test void directEvaluationAndReplayCannotBypassStockTypePolicy() {
        doThrow(new IllegalArgumentException("STOCK_TYPE_NOT_ALLOWED")).when(provider).validateStockType("069500");
        var input = new ScreenerInput("069500", new ScreenerTimeframe(Mode.SWING, 0), LocalDateTime.of(2026,9,13,0,0), List.of(), List.of());
        assertThatThrownBy(() -> service.evaluate(input)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.replay("old-run", "069500")).isInstanceOf(IllegalArgumentException.class);
    }
}
