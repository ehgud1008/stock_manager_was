package com.example.stockanalysis.unified;

import static com.example.stockanalysis.unified.scan.UnifiedScanModels.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.stockanalysis.analysis.application.model.CollectedAnalysisData;
import com.example.stockanalysis.analysis.application.port.out.AnalysisDataProvider;
import com.example.stockanalysis.analysis.domain.DataQualityStatus;
import com.example.stockanalysis.engine.model.*;
import com.example.stockanalysis.screener.application.ScreenerDataProvider;
import com.example.stockanalysis.screener.domain.ScreenerItem.Stock;
import com.example.stockanalysis.screener.domain.ScreenerRunRequest.Market;
import com.example.stockanalysis.screener.infrastructure.ScreenerStockTypeCatalog;
import com.example.stockanalysis.unified.application.UnifiedAnalysisService;
import com.example.stockanalysis.unified.eod.*;
import com.example.stockanalysis.unified.internal.DefaultUnifiedStockAnalysisEngine;
import com.example.stockanalysis.unified.scan.UnifiedScanService;
import com.example.stockanalysis.unified.scan.UnifiedScanStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;

class UnifiedScanTest {
    private DriverManagerDataSource source;
    private UnifiedScanStore store;
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-19T02:00:00Z"), ZoneOffset.UTC);
    private final LocalDate date = LocalDate.of(2026, 9, 18);
    private AnalysisDataProvider provider;
    private ScreenerDataProvider universe;
    private UnifiedAnalysisService analyzer;
    private UnifiedScanService service;
    private EodStore jobs;
    private EodCoordinator coordinator;
    private ScheduledExecutorService heartbeats;
    private final Request previousClose = new Request(Market.ALL, PriceMode.PREVIOUS_CLOSE);

    @BeforeEach void setup() throws Exception {
        source = new DriverManagerDataSource("jdbc:h2:mem:unified_" + UUID.randomUUID() + ";MODE=Oracle;DB_CLOSE_DELAY=-1", "sa", "");
        var sql = Files.readString(Path.of("..", "database", "oracle", "ddl", "11_unified_analysis.sql"));
        new ResourceDatabasePopulator(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8))).execute(source);
        store = new UnifiedScanStore(source, new ObjectMapper().registerModule(new JavaTimeModule()));
        provider = mock(AnalysisDataProvider.class); universe = mock(ScreenerDataProvider.class);
        var types = mock(ScreenerStockTypeCatalog.class); when(types.typeOf(anyString())).thenReturn("ST");
        analyzer = new UnifiedAnalysisService(provider, new DefaultUnifiedStockAnalysisEngine(), types, clock);
        jobs = new EodStore(source);
        heartbeats = Executors.newSingleThreadScheduledExecutor();
        coordinator = coordinator(store);
        service = new UnifiedScanService(store, coordinator);
        when(universe.universe(Market.ALL)).thenReturn(List.of(stock("005930", "ST", ""), stock("000660", "ST", "")));
        when(provider.collect(anyString(), eq(AnalysisMode.SWING), eq(date))).thenAnswer(i -> data(i.getArgument(0)));
    }
    @AfterEach void stop() { heartbeats.shutdownNow(); }

    private EodCoordinator coordinator(UnifiedScanStore scans) {
        var settings = new EodProperties("classpath:unified-calendar-2026.csv", Duration.ofHours(8), Duration.ofMinutes(10),
                Duration.ofSeconds(30), Duration.ofMinutes(90), Duration.ofHours(12), Duration.ofSeconds(10), 3, Duration.ofMinutes(2));
        var calendar = new TradingCalendar(settings, new org.springframework.core.io.DefaultResourceLoader());
        return new EodCoordinator(jobs, scans, analyzer, universe, calendar, settings,
                new ObjectMapper().registerModule(new JavaTimeModule()), clock, heartbeats);
    }

    private void runToEnd() {
        for (int tick = 0; tick < 4; tick++) coordinator.tick();
    }

    private Stock stock(String code, String type, String state) { return new Stock(code, "종목" + code, "KOSPI", state, date.minusYears(10), type); }
    private CollectedAnalysisData data(String code) {
        List<DailyCandle> bars = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            var close = BigDecimal.valueOf(1000 + 100 * Math.sin(i * Math.PI / 10));
            bars.add(new DailyCandle(date.minusDays(299 - i), close, close.add(BigDecimal.TEN), close.subtract(BigDecimal.TEN), close, 1_000_000));
        }
        return new CollectedAnalysisData(new AnalysisInput(new StockSnapshot(code, "종목" + code, "KOSPI", BigDecimal.valueOf(999999)),
                AnalysisMode.SWING, date, clock.instant(), bars, Map.of()), List.of(), List.of(), DataQualityStatus.COMPLETE);
    }
    private Query query() { return new Query(null, null, null, null, null, null, null, null, null, null, null, "totalScore", false, 0, 20, null); }

    @Test void asyncRunPersistsUnifiedSnapshotAndIdempotencyDoesNotEnqueueTwice() {
        var r = service.submit(previousClose, "request-1");
        assertThat(r.status()).isEqualTo("READY");
        verifyNoInteractions(provider, universe);
        assertThat(service.submit(previousClose, "request-1").runId()).isEqualTo(r.runId());
        assertThatThrownBy(() -> service.submit(new Request(Market.KOSPI, PriceMode.PREVIOUS_CLOSE), "request-1")).hasMessageContaining("다른 시장");
        assertThatThrownBy(() -> service.submit(new Request(Market.ALL), "request-1")).hasMessageContaining("가격 기준");
        assertThatThrownBy(() -> service.submit(previousClose, "other")).hasMessageContaining("실행 중");
        runToEnd();
        var completed = service.get(r.runId());
        assertThat(completed.completed()).isEqualTo(2); assertThat(completed.status()).isEqualTo("COMPLETED");
        var page = service.query(r.runId(), query()); assertThat(page.totalElements()).isEqualTo(2);
        var item = page.content().get(0); var detail = service.detail(r.runId(), item.stockCode());
        assertThat(detail.result().engineVersion()).isEqualTo("unified-swing-v1");
        assertThat(detail.priceHistory()).hasSize(300).allMatch(b -> !b.businessDate().isAfter(detail.result().priceDate()));
        assertThat(store.detail(r.runId(),item.stockCode()).orElseThrow().priceHistory()).isEqualTo(detail.priceHistory());
        assertThat(detail.result().stock().currentPrice()).isNotEqualByComparingTo("999999");
        assertThat(item.totalScore()).isEqualByComparingTo(detail.result().analysis().totalScore());
        assertThat(item.stage()).isEqualTo(detail.result().screener().stage());
        assertThat(item.volume()).isEqualTo(1_000_000L);
        assertThat(detail.volume()).isEqualTo(item.volume());
        assertThat(item.tradingAmountMillionWon()).isNull();
        assertThat(detail.result().asOf()).isEqualTo(date.plusDays(1).atStartOfDay());
        verify(provider, times(1)).collect(item.stockCode(), AnalysisMode.SWING, date);
        assertThat(store.recent()).hasSize(1);
        assertThatThrownBy(() -> service.resume(r.runId())).hasMessageContaining("재개");
    }
    @Test void retryOnlyFailedStocksKeepsOriginalSnapshotAndUniverse() {
        when(provider.collect("000660", AnalysisMode.SWING, date)).thenThrow(new IllegalStateException("unavailable"));
        var r = service.submit(previousClose, "retry"); runToEnd();
        assertThat(service.get(r.runId()).status()).isEqualTo("COMPLETED_WITH_ERRORS");
        var saved = service.detail(r.runId(), "005930");
        doReturn(data("000660")).when(provider).collect("000660", AnalysisMode.SWING, date);
        service.resume(r.runId()); runToEnd();
        assertThat(service.get(r.runId()).failed()).isZero();
        assertThat(service.detail(r.runId(), "005930")).isEqualTo(saved);
        verify(provider, times(1)).collect("005930", AnalysisMode.SWING, date);
        verify(universe, times(1)).universe(Market.ALL);
    }
    @Test
    @org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
    void collectionFailureLogsLocationAndCauseWithoutSensitiveMessages(
            org.springframework.boot.test.system.CapturedOutput output) {
        when(provider.collect("000660", AnalysisMode.SWING, date)).thenThrow(
                new IllegalStateException("secret-provider-body",
                        new java.sql.SQLException("secret-sql-values", "42000", 904)));
        var run = service.submit(previousClose, "diagnostic-collection");
        runToEnd();
        assertThat(service.get(run.runId()).failed()).isEqualTo(1);
        assertThat(service.get(run.runId()).completed()).isEqualTo(1);
        assertThat(output.getOut()).contains("Previous close item failed: run=" + run.runId(),
                "stock=000660, phase=COLLECT_AND_ANALYZE", "java.sql.SQLException", "sqlErrorCode=904")
                .doesNotContain("secret-provider-body", "secret-sql-values");
    }
    @Test
    @org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
    void saveFailureLogsSavePhaseAndKeepsOtherResults(
            org.springframework.boot.test.system.CapturedOutput output) {
        var failingStore = spy(store);
        doThrow(new IllegalStateException("secret-storage-value")).when(failingStore)
                .complete(anyString(), argThat(item -> item.stockCode().equals("000660")), any());
        coordinator = coordinator(failingStore);
        var run = service.submit(previousClose, "diagnostic-save");
        runToEnd();
        assertThat(service.get(run.runId()).failed()).isEqualTo(1);
        assertThat(service.get(run.runId()).completed()).isEqualTo(1);
        assertThat(output.getOut()).contains("stock=000660, phase=SAVE_RESULT")
                .doesNotContain("secret-storage-value");
    }
    @Test void previousCloseIncludesSuspendedCommonStocksAndExcludesEtfs() {
        when(universe.universe(Market.ALL)).thenReturn(List.of(stock("005930", "ST", "거래정지"), stock("000660", "ETF", "")));
        var r = service.submit(previousClose, "excluded"); runToEnd();
        assertThat(service.get(r.runId()).total()).isEqualTo(1);
        assertThat(service.get(r.runId()).completed()).isEqualTo(1);
        verify(provider).collect("005930", AnalysisMode.SWING, date);
        verifyNoMoreInteractions(provider);
        when(universe.universe(Market.ALL)).thenReturn(List.of());
        var empty = service.submit(previousClose, "empty"); runToEnd();
        assertThat(service.get(empty.runId()).initialized()).isTrue(); assertThat(service.get(empty.runId()).status()).isEqualTo("COMPLETED");
    }
    @Test void restartRecoveryProtectsLiveWorkAndVersionedResume() {
        var r = service.submit(previousClose, "restart");
        assertThat(jobs.acquire("other-worker", Duration.ofMinutes(2))).isTrue();
        assertThatThrownBy(() -> service.recover(r.runId())).hasMessageContaining("실행 중");
        jobs.release("other-worker");
        var restarted = new UnifiedScanService(store, coordinator(store));
        assertThatThrownBy(() -> restarted.resume(r.runId())).hasMessageContaining("재개");
        assertThat(restarted.recover(r.runId()).status()).isEqualTo("INTERRUPTED");
        new JdbcTemplate(source).update("UPDATE ua_scan_run SET engine_version='previous-close/old' WHERE run_id=?", r.runId());
        assertThatThrownBy(() -> restarted.resume(r.runId())).hasMessageContaining("버전");
    }
    @Test void failedSaveRollsBackSnapshotAndSummaryTogether() {
        var broken = spy(store);
        doAnswer(invocation -> { invocation.callRealMethod(); throw new IllegalStateException("after write"); }).when(broken).complete(anyString(), any(), any());
        coordinator = coordinator(broken);
        var r = service.submit(previousClose, "atomic"); runToEnd();
        coordinator = coordinator(store);
        assertThat(service.get(r.runId()).failed()).isEqualTo(2);
        assertThat(new JdbcTemplate(source).queryForObject("SELECT COUNT(*) FROM ua_scan_item WHERE result_json IS NOT NULL", Integer.class)).isZero();
        service.resume(r.runId()); runToEnd();
        assertThat(service.get(r.runId()).completed()).isEqualTo(2);
    }
    @Test void combinesStageScoresAndAllActiveSignalsWithoutReadingDetailJson() {
        var r = service.submit(previousClose, "filters"); runToEnd();
        var jdbc = new JdbcTemplate(source);
        jdbc.update("UPDATE ua_scan_item SET stage=6,transition_type='FORWARD',bars_since_transition=2,all_slopes_up=1,total_score=80,reward_risk=2,buy_signal='BREAKOUT',active_signals='|BREAKOUT|PULLBACK_RECOVERY|' WHERE stock_code='005930'");
        jdbc.update("UPDATE ua_scan_item SET total_score=NULL,buy_signal=NULL WHERE stock_code='000660'");
        // Corrupting only the CLOB must not affect summary search.
        jdbc.update("UPDATE ua_scan_item SET result_json='invalid JSON'");
        var q = new Query("", "COMPLETED", 6, "FORWARD", true, 2, "PULLBACK_RECOVERY", null,
                BigDecimal.valueOf(70), BigDecimal.valueOf(90), BigDecimal.valueOf(1.5), "totalScore", false, 0, 20, null);
        assertThat(service.query(r.runId(), q).content()).extracting(Item::stockCode).containsExactly("005930");
        assertThat(service.query(r.runId(), query()).content().get(1).totalScore()).isNull();
        assertThat(service.query(r.runId(), new Query("%", null, null, null, null, null, null, null, null, null, null, null, false, 0, 20, null)).totalElements()).isZero();
        assertThatThrownBy(() -> new Query(null, null, 7, null, null, null, null, null, null, null, null, null, false, 0, 20, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Query(null, null, null, null, null, null, null, null, null, null, null, "DROP TABLE", false, 0, 20, null)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void searchesSetupAndPreliminarySignalsAndSortsByEntryScoreBeforePagination() {
        var run = service.submit(previousClose, "new-filters");
        runToEnd();
        var jdbc = new JdbcTemplate(source);
        jdbc.update("UPDATE ua_scan_item SET setup_type='PULLBACK',buy_signal='BREAKOUT',buy_state='PRELIMINARY',entry_score=70 WHERE stock_code='005930'");
        jdbc.update("UPDATE ua_scan_item SET setup_type='PULLBACK',buy_signal='BREAKOUT',buy_state='CONFIRMED',entry_score=90 WHERE stock_code='000660'");
        assertThat(service.query(run.runId(), scenarioQuery(null, 0)).content()).extracting(Item::stockCode).containsExactly("000660");
        assertThat(service.query(run.runId(), scenarioQuery(null, 1)).content()).extracting(Item::stockCode).containsExactly("005930");
        assertThat(service.query(run.runId(), scenarioQuery("BUY_PRELIMINARY", 0)).content())
                .extracting(Item::stockCode).containsExactly("005930");
        assertThat(service.query(run.runId(), scenarioQuery("BUY", 0)).content()).extracting(Item::stockCode).containsExactly("000660");
    }

    @Test void readsStoredLegacyRowsWithSharedFiltersAndOriginalDetailId() {
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE an_analysis_run (analysis_run_id NUMBER(19) PRIMARY KEY)");
        var ddl = Path.of("..", "database", "oracle", "ddl", "10_analysis_scan.sql");
        new ResourceDatabasePopulator(new org.springframework.core.io.FileSystemResource(ddl)).execute(source);
        jdbc.update("INSERT INTO an_analysis_run (analysis_run_id) VALUES (42)");
        jdbc.update("INSERT INTO an_scan_run (run_id,request_key,market,base_date,engine_version,status,created_epoch) "
                + "VALUES ('legacy','legacy-key','ALL',?,'legacy','COMPLETED',1)", java.sql.Date.valueOf(date));
        jdbc.update("INSERT INTO an_scan_item (run_id,stock_code,stock_name,market,status,analysis_run_id,setup_type,buy_signal,buy_state,entry_score,target_two) "
                + "VALUES ('legacy','005930','삼성전자','KOSPI','COMPLETED',42,'PULLBACK','BREAKOUT','PRELIMINARY',75,80000)");
        assertThat(service.recentArchived()).extracting(Run::runId).containsExactly("legacy");
        assertThat(service.recent()).isEmpty();
        var page = service.queryArchived("legacy", scenarioQuery("BUY_PRELIMINARY", 0));
        assertThat(page.totalElements()).isEqualTo(1);
        var item = page.content().get(0);
        assertThat(item.entryScore()).isEqualByComparingTo("75");
        assertThat(item.targetTwo()).isEqualByComparingTo("80000");
        assertThat(item.stage()).isNull();
        assertThat(service.archivedAnalysisId("legacy", "005930")).isEqualTo(42L);
        verifyNoInteractions(provider, universe);
    }

    private Query scenarioQuery(String signal, int page) {
        return new Query(null, null, null, null, null, null, signal, null, null, null, null,
                "entryScore", false, page, 1, null, null, null, null, "PULLBACK");
    }

    @Test void interruptionLeavesPendingItemsForResume() {
        when(provider.collect(anyString(), eq(AnalysisMode.SWING), eq(date))).thenThrow(new CancellationException());
        var r = service.submit(previousClose, "interrupt");
        coordinator.tick();
        coordinator.tick();
        assertThat(service.get(r.runId()).status()).isEqualTo("RUNNING");
        assertThat(service.get(r.runId()).failed()).isZero();
        assertThat(store.nextPreviousCloseItem(r.runId())).isPresent();
    }
    @Test void filtersEveryEntryStatusBeforeCountingAndPaging() {
        var r = service.submit(previousClose, "entry-filter"); runToEnd();
        var jdbc = new JdbcTemplate(source);
        jdbc.update("UPDATE ua_scan_item SET entry_status=NULL");
        for (String entry : List.of("IN_RANGE", "CHASE_BLOCKED", "OUTSIDE_ENTRY_ZONE", "SCENARIO_UNAVAILABLE", "SELL_SIGNAL_CONFLICT", "NONE")) {
            jdbc.update("UPDATE ua_scan_item SET entry_status=? WHERE stock_code='005930'", entry);
            var q = new Query(null, "COMPLETED", null, null, null, null, null, null, null, null, null, "stockCode", false, 0, 1, entry);
            var result = service.query(r.runId(), q);
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.content()).extracting(Item::stockCode).containsExactly("005930");
            var next = new Query(null, "COMPLETED", null, null, null, null, null, null, null, null, null, "stockCode", false, 1, 1, entry);
            assertThat(service.query(r.runId(), next).content()).isEmpty();
        }
        assertThat(service.query(r.runId(), query()).totalElements()).isEqualTo(2);
        var blank = new Query(null, null, null, null, null, null, null, null, null, null, null, "stockCode", false, 0, 20, "");
        assertThat(service.query(r.runId(), blank).totalElements()).isEqualTo(2);
    }
    @Test void duplicateUniverseInitializationRollsBack() {
        var stocks=new ArrayList<Stock>();
        for(int i=1;i<=251;i++)stocks.add(stock(String.format("%06d",i),"ST",""));
        stocks.add(stocks.get(0)); // Failure after the first 250-row batch must roll back every row.
        when(universe.universe(Market.ALL)).thenReturn(stocks);
        var r = service.submit(previousClose, "duplicate"); runToEnd();
        assertThat(service.get(r.runId()).status()).isEqualTo("FAILED"); assertThat(service.get(r.runId()).total()).isZero();
    }

    private Query liquidity(String market, Long volume, String amount, int page) {
        return new Query(null, null, null, null, null, null, null, null, null, null, null,
                "stockCode", false, page, 1, null, market, volume, amount == null ? null : new BigDecimal(amount));
    }
    @Test void persistsActualAmountTogetherWithSnapshotWithoutAdditionalCollection() {
        var original = data("005930");
        var record = new com.example.stockanalysis.backdata.validation.model.BackdataRecord("ka10081",
                com.example.stockanalysis.backdata.validation.model.BackdataDatasetType.DAILY_CANDLE,
                "005930", null, null, date, clock.instant(), Map.of("tradingAmount", "12345.5"));
        var payload = new com.example.stockanalysis.analysis.application.model.AnalysisSourcePayload("ka10081", "test", "DAILY_CANDLE",
                Map.of(), null, null, List.of(record), clock.instant(), clock.instant());
        when(provider.collect("005930", AnalysisMode.SWING, date)).thenReturn(new CollectedAnalysisData(
                original.input(), List.of(payload), List.of(), DataQualityStatus.COMPLETE));
        var run = service.submit(previousClose, "actual-amount"); runToEnd();
        var saved = service.query(run.runId(), liquidity(null, 1_000_000L, "123.455", 0));
        assertThat(saved.content()).extracting(Item::stockCode).containsExactly("005930");
        assertThat(saved.content().get(0).tradingAmountMillionWon()).isEqualByComparingTo("12345.5");
        assertThat(service.detail(run.runId(), "005930").tradingAmountMillionWon()).isEqualByComparingTo("12345.5");
        verify(provider, times(1)).collect("005930", AnalysisMode.SWING, date);
    }
    @Test void liquidityFiltersUseInclusiveBoundsUnitsAndNullSemanticsBeforePaging() {
        var r = service.submit(previousClose, "liquidity"); runToEnd();
        var jdbc = new JdbcTemplate(source);
        jdbc.update("UPDATE ua_scan_item SET market='KOSDAQ',volume=100000,trading_amt_mil_krw=10025 WHERE stock_code='005930'");
        jdbc.update("UPDATE ua_scan_item SET market='KOSPI',volume=NULL,trading_amt_mil_krw=NULL WHERE stock_code='000660'");
        jdbc.update("UPDATE ua_scan_item SET result_json='invalid JSON'");
        var q = liquidity("KOSDAQ", 100000L, "100.25", 0);
        assertThat(service.query(r.runId(), q).content()).extracting(Item::stockCode).containsExactly("005930");
        assertThat(service.query(r.runId(), q).totalElements()).isEqualTo(1);
        assertThat(service.query(r.runId(), liquidity("KOSDAQ", 100000L, "100.25", 1)).content()).isEmpty();
        assertThat(service.query(r.runId(), liquidity(null, 100001L, null, 0)).totalElements()).isZero();
        assertThat(service.query(r.runId(), liquidity(null, null, "100.250001", 0)).totalElements()).isZero();
        assertThat(service.query(r.runId(), liquidity("KOSPI", 0L, null, 0)).totalElements()).isZero();
        assertThat(service.query(r.runId(), liquidity(null, null, "0", 0)).totalElements()).isEqualTo(1);
        assertThat(service.query(r.runId(), liquidity("ALL", null, null, 0)).totalElements()).isEqualTo(2);
        jdbc.update("UPDATE ua_scan_item SET volume=0,trading_amt_mil_krw=0 WHERE stock_code='000660'");
        assertThat(service.query(r.runId(), liquidity("KOSPI", 0L, "0", 0)).totalElements()).isEqualTo(1);
    }

    @Test void migrationPreservesOldRowsAndLegacyJsonRemainsReadable() throws Exception {
        var r = service.submit(previousClose, "migration"); runToEnd();
        var jdbc = new JdbcTemplate(source);
        var detail = service.detail(r.runId(), "005930");
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        var old = mapper.valueToTree(detail); ((com.fasterxml.jackson.databind.node.ObjectNode) old).remove(List.of("volume", "tradingAmountMillionWon"));
        jdbc.update("UPDATE ua_scan_item SET result_json=? WHERE stock_code='005930'", old.toString());
        jdbc.execute("ALTER TABLE ua_scan_item DROP COLUMN volume");
        jdbc.execute("ALTER TABLE ua_scan_item DROP COLUMN trading_amt_mil_krw");
        var sql = Files.readString(Path.of("..", "database", "oracle", "migration", "20261004_01_unified_liquidity_filters.sql"));
        new ResourceDatabasePopulator(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8))).execute(source);
        assertThat(service.query(r.runId(), query()).totalElements()).isEqualTo(2);
        assertThat(service.query(r.runId(), query()).content()).allMatch(i -> i.volume() == null && i.tradingAmountMillionWon() == null);
        assertThat(service.detail(r.runId(), "005930").volume()).isNull();

        for (String column : List.of("rise_score", "entry_score", "risk_score", "buy_state", "sell_state", "setup_type", "target_two")) {
            jdbc.execute("ALTER TABLE ua_scan_item DROP COLUMN " + column);
        }
        var consolidated = Path.of("..", "database", "oracle", "migration", "20261118_01_unified_scan_features.sql");
        new ResourceDatabasePopulator(new org.springframework.core.io.FileSystemResource(consolidated)).execute(source);
        assertThat(service.query(r.runId(), query()).content()).hasSize(2).allMatch(item -> item.entryScore() == null);
        assertThat(service.detail(r.runId(), "005930").result().priceDate()).isEqualTo(detail.result().priceDate());
    }
}
