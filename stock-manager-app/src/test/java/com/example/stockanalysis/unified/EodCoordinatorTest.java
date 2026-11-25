package com.example.stockanalysis.unified;

import com.example.stockanalysis.analysis.application.model.CollectedAnalysisData;
import com.example.stockanalysis.analysis.application.port.out.AnalysisDataProvider;
import com.example.stockanalysis.analysis.domain.DataQualityStatus;
import com.example.stockanalysis.engine.model.*;
import com.example.stockanalysis.kiwoom.client.KiwoomApiException;
import com.example.stockanalysis.screener.application.ScreenerDataProvider;
import com.example.stockanalysis.screener.domain.ScreenerItem.Stock;
import com.example.stockanalysis.screener.domain.ScreenerRunRequest.Market;
import com.example.stockanalysis.screener.infrastructure.ScreenerStockTypeCatalog;
import com.example.stockanalysis.unified.application.UnifiedAnalysisService;
import com.example.stockanalysis.unified.eod.*;
import com.example.stockanalysis.unified.internal.DefaultUnifiedStockAnalysisEngine;
import com.example.stockanalysis.unified.scan.*;
import com.example.stockanalysis.unified.scan.UnifiedScanModels.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EodCoordinatorTest {
    static final LocalDate DAY=LocalDate.of(2026,10,13);
    static class MutableClock extends Clock {
        Instant time=Instant.parse("2026-10-13T11:11:00Z");
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return Clock.fixed(time,zone);}
        public Instant instant(){return time;}
        void advance(Duration d){time=time.plus(d);}
    }
    MutableClock clock; DriverManagerDataSource source; JdbcTemplate jdbc;
    ObjectMapper mapper; UnifiedScanStore scans; EodStore jobs; EodProperties settings; TradingCalendar calendar;
    AnalysisDataProvider provider; ScreenerDataProvider universe; UnifiedAnalysisService analyzer;
    ScheduledExecutorService heartbeats; EodCoordinator coordinator;
    @BeforeEach void setup() throws Exception {
        clock=new MutableClock(); source=new DriverManagerDataSource("jdbc:h2:mem:eod_"+UUID.randomUUID()+";MODE=Oracle;DB_CLOSE_DELAY=-1","sa","");
        var sql=Files.readString(Path.of("..","database","oracle","ddl","11_unified_analysis.sql"));
        new ResourceDatabasePopulator(new ByteArrayResource(sql.getBytes(java.nio.charset.StandardCharsets.UTF_8))).execute(source);
        jdbc=new JdbcTemplate(source); mapper=new ObjectMapper().registerModule(new JavaTimeModule());
        scans=new UnifiedScanStore(source,mapper); jobs=new EodStore(source);
        settings=new EodProperties("classpath:unified-calendar-2026.csv",Duration.ofHours(8),Duration.ofMinutes(10),Duration.ofSeconds(30),
                Duration.ofMinutes(90),Duration.ofHours(12),Duration.ofSeconds(10),3,Duration.ofMinutes(2));
        calendar=new TradingCalendar(settings,new DefaultResourceLoader());
        provider=mock(AnalysisDataProvider.class); universe=mock(ScreenerDataProvider.class);
        when(provider.collectDaily(anyString(),any())).thenCallRealMethod();
        when(provider.completeDaily(any())).thenCallRealMethod();
        analyzer=new UnifiedAnalysisService(provider,new DefaultUnifiedStockAnalysisEngine(),mock(ScreenerStockTypeCatalog.class),clock);
        heartbeats=Executors.newSingleThreadScheduledExecutor(); coordinator=coordinator(scans);
        when(universe.universe(any())).thenReturn(List.of(stock("000660"),stock("005930")));
        when(provider.collect(anyString(),eq(AnalysisMode.SWING),any())).thenAnswer(i -> data(i.getArgument(0),i.getArgument(2),i.getArgument(2)));
    }
    @AfterEach void stop(){heartbeats.shutdownNow();}
    EodCoordinator coordinator(UnifiedScanStore store){return new EodCoordinator(jobs,store,analyzer,universe,calendar,settings,mapper,clock,heartbeats);}
    Stock stock(String code){return new Stock(code,"종목"+code,"KOSPI","",DAY.minusYears(10),"ST");}
    CollectedAnalysisData data(String code,LocalDate requested,LocalDate priceDate){
        var bars=new ArrayList<DailyCandle>();
        for(int i=0;i<300;i++){
            var value=BigDecimal.valueOf(1000+100*Math.sin(i*Math.PI/10));
            bars.add(new DailyCandle(priceDate.minusDays(299-i),value,value.add(BigDecimal.TEN),value.subtract(BigDecimal.TEN),value,1000000));
        }
        return new CollectedAnalysisData(new AnalysisInput(new StockSnapshot(code,"종목"+code,"KRX",BigDecimal.valueOf(999999)),
                AnalysisMode.SWING,requested,clock.instant(),bars,Map.of()),List.of(),List.of("선택 자료 미수집"),DataQualityStatus.PARTIAL);
    }
    @Test void eodUsesOneFrozenBalanceAcrossTicksAndPersistsPersonalResult() {
        new ResourceDatabasePopulator(new FileSystemResource(Path.of("..","database","oracle","ddl","25_personal_holdings.sql"))).execute(source);
        var holdings=spy(new com.example.stockanalysis.holdings.HoldingService(jdbc,mapper,clock,"owner"));
        holdings.update("005930",new com.example.stockanalysis.holdings.HoldingModels.Edit("삼성전자",BigDecimal.TEN,BigDecimal.valueOf(100),null,DAY,null,null,0));
        analyzer.setHoldings(holdings);
        var run=submit();sampleBoth();
        assertThat(run.engineVersion()).hasSizeLessThanOrEqualTo(160);
        holdings.update("005930",new com.example.stockanalysis.holdings.HoldingModels.Edit("삼성전자",BigDecimal.ZERO,null,null,DAY,null,null,1));
        finishBoth();
        verify(holdings,times(1)).list();
        var detail=scans.detail(run.runId(),"005930").orElseThrow();
        assertThat(detail.holdingAnalysis().positionStatus()).isEqualTo("HELD");
        assertThat(detail.holdingAnalysis().holding().quantity()).isEqualByComparingTo("10");
        // The balance lookup succeeded; absence from that frozen list means not held.
        assertThat(scans.detail(run.runId(),"000660").orElseThrow().holdingAnalysis().positionStatus()).isEqualTo("NOT_HELD");
    }
    Run submit(){return coordinator.submit(new Request(Market.ALL),"request-1");}
    void sampleBoth(){coordinator.tick();coordinator.tick();coordinator.tick();}
    void finishBoth(){clock.advance(Duration.ofSeconds(31));coordinator.tick();coordinator.tick();}

    @Test void restoresCallerMdcAfterCollectionFailure() {
        var previous = org.slf4j.MDC.getCopyOfContextMap();
        try {
            org.slf4j.MDC.put("jobId", "outer-job");
            org.slf4j.MDC.put("stockCode", "outer-stock");
            org.slf4j.MDC.put("requestId", "outer-request");
            var run = submit();
            doAnswer(call -> {
                assertThat(org.slf4j.MDC.get("jobId")).isEqualTo(run.runId());
                assertThat(org.slf4j.MDC.get("stockCode")).isEqualTo(call.getArgument(0, String.class));
                assertThat(org.slf4j.MDC.get("requestId")).isEqualTo("outer-request");
                throw new IllegalArgumentException("Invalid sample");
            }).when(provider).collectDaily(anyString(), any());
            sampleBoth();
            verify(provider, atLeastOnce()).collectDaily(anyString(), any());
            assertThat(org.slf4j.MDC.get("jobId")).isEqualTo("outer-job");
            assertThat(org.slf4j.MDC.get("stockCode")).isEqualTo("outer-stock");
            assertThat(org.slf4j.MDC.get("requestId")).isEqualTo("outer-request");
        } finally {
            if (previous == null) org.slf4j.MDC.clear();
            else org.slf4j.MDC.setContextMap(previous);
        }
    }

    void useProxiedStore() {
        var proxy=new org.springframework.aop.framework.ProxyFactory(jobs);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new org.springframework.dao.support.PersistenceExceptionTranslationInterceptor(
                (org.springframework.dao.support.PersistenceExceptionTranslator) exception -> null));
        jobs=(EodStore)proxy.getProxy();
        assertThat(org.springframework.aop.support.AopUtils.isCglibProxy(jobs)).isTrue();
        coordinator=coordinator(scans);
    }

    @Test void resumeThroughRepositoryProxyOnlyRequeuesEligibleFailures() {
        useProxiedStore();
        var run=submit();coordinator.tick();
        jdbc.update("UPDATE ua_scan_run SET status='FAILED' WHERE run_id=?",run.runId());
        jdbc.update("UPDATE ua_scan_item SET status='FAILED' WHERE run_id=?",run.runId());
        jdbc.update("UPDATE ua_eod_item SET retryable=0 WHERE run_id=? AND stock_code='005930'",run.runId());

        assertThat(coordinator.resume(run.runId()).status()).isEqualTo("READY");
        assertThat(scans.getItem(run.runId(),"000660").status()).isEqualTo("RETRY_WAIT");
        assertThat(scans.getItem(run.runId(),"005930").status()).isEqualTo("FAILED");
        sampleBoth();finishBoth();
        assertThat(scans.find(run.runId()).orElseThrow().status()).isEqualTo("COMPLETED_WITH_ERRORS");
        verify(provider,never()).collect(eq("005930"),any(),any());
    }

    @Test void resumeThroughRepositoryProxyPreservesExhaustedRetryBudget() {
        useProxiedStore();
        var run=submit();coordinator.tick();
        jdbc.update("UPDATE ua_scan_run SET status='FAILED' WHERE run_id=?",run.runId());
        jdbc.update("UPDATE ua_scan_item SET status='FAILED' WHERE run_id=?",run.runId());
        jdbc.update("UPDATE ua_eod_item SET attempts=? WHERE run_id=?",settings.maxAttempts(),run.runId());

        assertThatThrownBy(() -> coordinator.resume(run.runId())).hasMessageContaining("재시도 가능한");
        assertThat(scans.find(run.runId()).orElseThrow().status()).isEqualTo("FAILED");
        assertThat(scans.getItem(run.runId(),"000660").status()).isEqualTo("FAILED");
    }

    @Test void interruptedRunRecoversThroughRepositoryProxy() {
        useProxiedStore();
        var run=submit();coordinator.tick();
        jdbc.update("UPDATE ua_scan_run SET status='INTERRUPTED' WHERE run_id=?",run.runId());

        coordinator.tick();
        var saved=scans.find(run.runId()).orElseThrow();
        assertThat(saved.eod().recoveryCount()).isEqualTo(1);
        assertThat(saved.status()).isEqualTo("RETRY_WAIT");
        coordinator.tick();finishBoth();
        assertThat(scans.find(run.runId()).orElseThrow().status()).isEqualTo("COMPLETED");
    }

    @Test void enrichesOnlyVerifiedSamplesAndPersistsSupplementaryInputs() throws Exception {
        when(provider.completeDaily(any())).thenAnswer(i -> {
            CollectedAnalysisData daily=i.getArgument(0); var input=daily.input();
            return new CollectedAnalysisData(new AnalysisInput(input.stock(),input.mode(),input.baseDate(),clock.instant(),
                    input.dailyCandles(),Map.of("roe",new BigDecimal("12.34"))),daily.sources(),daily.warnings(),daily.dataQualityStatus());
        });
        var run=submit();sampleBoth();verify(provider,never()).completeDaily(any());
        finishBoth();verify(provider,times(2)).completeDaily(any());
        var saved=mapper.readTree(jdbc.<String>queryForObject("SELECT input_json,input_gzip,input_sha256,input_utf8_bytes FROM ua_eod_item WHERE run_id=? AND stock_code='005930'",(r,n)->EodSnapshotCodec.read(r),run.runId()));
        assertThat(saved.path("input").path("observations").path("roe").decimalValue()).isEqualByComparingTo("12.34");
        assertThat(scans.find(run.runId()).orElseThrow().completed()).isEqualTo(2);
    }

    @Test void enrichmentCannotReplaceVerifiedDailyPrices() {
        when(provider.completeDaily(any())).thenAnswer(i -> {
            CollectedAnalysisData daily=i.getArgument(0);var input=daily.input();
            if(!input.stock().stockCode().equals("000660"))return daily;
            var bars=new ArrayList<>(input.dailyCandles());var last=bars.remove(bars.size()-1);
            bars.add(new DailyCandle(last.businessDate(),last.open(),last.high(),last.low(),last.close().add(BigDecimal.ONE),last.volume()));
            return new CollectedAnalysisData(new AnalysisInput(input.stock(),input.mode(),input.baseDate(),clock.instant(),bars,Map.of()),
                    daily.sources(),daily.warnings(),daily.dataQualityStatus());
        });
        var run=submit();sampleBoth();finishBoth();
        assertThat(scans.detail(run.runId(),"000660")).isEmpty();
        assertThat(scans.detail(run.runId(),"005930")).isPresent();
        assertThat(scans.find(run.runId()).orElseThrow().status()).isEqualTo("COMPLETED_WITH_ERRORS");
    }

    @Test void confirmsTodayAfterTwoSamplesAndPersistsInputWithoutForgingMidnight() {
        var run=submit();sampleBoth();
        assertThat(scans.find(run.runId()).orElseThrow().completed()).isZero();
        finishBoth();var saved=scans.find(run.runId()).orElseThrow();
        assertThat(saved.status()).isEqualTo("COMPLETED");assertThat(saved.completed()).isEqualTo(2);
        var detail=scans.detail(run.runId(),"005930").orElseThrow();
        assertThat(detail.result().priceDate()).isEqualTo(DAY);
        assertThat(detail.result().asOf()).isEqualTo(LocalDateTime.ofInstant(clock.instant(),ZoneId.of("Asia/Seoul")));
        assertThat(detail.eod().collectedAt()).isEqualTo(clock.instant());
        assertThat(detail.tradeJudgment()).isNotNull();
        assertThat(detail.tradeJudgment().priceDate()).isEqualTo(DAY);
        assertThat(detail.tradeJudgment().policyVersion()).isEqualTo(com.example.stockanalysis.judgment.TradeJudgmentEngine.VERSION);
        assertThat(detail.collectionQuality()).isEqualTo(DataQualityStatus.PARTIAL);
        assertThat(jdbc.<String>queryForObject("SELECT input_json,input_gzip,input_sha256,input_utf8_bytes FROM ua_eod_item WHERE run_id=? AND stock_code='005930'",(r,n)->EodSnapshotCodec.read(r),run.runId()))
                .contains("dailyCandles","observations","firstStableSampleEpoch","KIWOOM_KRX_DAILY_ADJUSTED");
        verify(provider,times(2)).collect("005930",AnalysisMode.SWING,DAY);
    }
    @Test void waitsForSettlementBeforeCallingProvider() {
        clock.time=Instant.parse("2026-10-13T11:00:00Z");var run=submit();coordinator.tick();
        assertThat(scans.find(run.runId()).orElseThrow().status()).isEqualTo("WAITING_DATA");verifyNoInteractions(provider,universe);
    }
    @Test void stalePriceNeverBecomesTodayAndEventuallyTimesOut() {
        when(provider.collect(anyString(),any(),any())).thenAnswer(i -> data(i.getArgument(0),DAY,DAY.minusDays(1)));
        var run=submit();sampleBoth();clock.advance(Duration.ofMinutes(91));coordinator.tick();coordinator.tick();
        var saved=scans.find(run.runId()).orElseThrow();
        assertThat(saved.status()).isEqualTo("FAILED");assertThat(saved.completed()).isZero();assertThat(saved.eod().dataUnavailable()).isEqualTo(2);
        assertThat(scans.detail(run.runId(),"005930")).isEmpty();
    }
    @Test void midnightAndRestartKeepDateUniverseAndCompletedSnapshot() {
        clock.time=Instant.parse("2026-10-13T14:59:00Z");var run=submit();sampleBoth();
        clock.advance(Duration.ofMinutes(2));coordinator.tick();
        var first=scans.detail(run.runId(),"000660").orElseThrow();
        jdbc.update("UPDATE ua_scan_run SET status='RUNNING' WHERE run_id=?",run.runId());
        coordinator=coordinator(scans);coordinator.tick();
        assertThat(scans.find(run.runId()).orElseThrow().baseDate()).isEqualTo(DAY);
        assertThat(scans.find(run.runId()).orElseThrow().eod().recoveryCount()).isEqualTo(1);
        assertThat(scans.detail(run.runId(),"000660")).contains(first);
        verify(universe,times(1)).universe(Market.ALL);verify(provider,times(2)).collect("000660",AnalysisMode.SWING,DAY);
    }
    @Test void transientErrorsRetryWithBudgetButPermanentErrorsDoNot() {
        when(provider.collect(eq("000660"),any(),any())).thenThrow(new KiwoomApiException("rate limit",429,"RATE_LIMIT"));
        when(provider.collect(eq("005930"),any(),any())).thenThrow(new KiwoomApiException("bad request",400,"INVALID"));
        var run=submit();sampleBoth();clock.advance(Duration.ofSeconds(11));coordinator.tick();
        clock.advance(Duration.ofSeconds(21));coordinator.tick();
        assertThat(scans.find(run.runId()).orElseThrow().failed()).isEqualTo(2);
        verify(provider,times(3)).collect("000660",AnalysisMode.SWING,DAY);verify(provider,times(1)).collect("005930",AnalysisMode.SWING,DAY);
    }
    @Test void onePermanentFailureDoesNotStopOtherStock() {
        when(provider.collect(eq("000660"),any(),any())).thenThrow(new IllegalArgumentException("bad"));
        var run=submit();sampleBoth();finishBoth();
        assertThat(scans.find(run.runId()).orElseThrow().status()).isEqualTo("COMPLETED_WITH_ERRORS");
        assertThat(scans.find(run.runId()).orElseThrow().completed()).isEqualTo(1);
    }
    @Test void absentProviderFailureCodeDoesNotBreakFailureHandlingOrBlockOtherStocks() {
        when(provider.collect(eq("000660"),any(),any())).thenThrow(new KiwoomApiException("provider error",400,null));
        var run=submit();sampleBoth();finishBoth();
        var saved=scans.find(run.runId()).orElseThrow();
        assertThat(saved.status()).isEqualTo("COMPLETED_WITH_ERRORS");
        assertThat(saved.failed()).isEqualTo(1);assertThat(saved.completed()).isEqualTo(1);
        verify(provider,times(1)).collect("000660",AnalysisMode.SWING,DAY);
    }
    @Test void absentUniverseFailureCodeReachesTerminalFailureInsteadOfRecoveryLoop() {
        when(universe.universe(any())).thenThrow(new KiwoomApiException("provider error",null,null));
        var run=submit();coordinator.tick();coordinator.tick();
        assertThat(scans.find(run.runId()).orElseThrow().status()).isEqualTo("FAILED");
        verify(universe,times(1)).universe(Market.ALL);verifyNoInteractions(provider);
    }
    @Test void duplicateRegistrationAndManualScheduledCollisionAreSerialized() throws Exception {
        var run=submit();coordinator.ensureScheduled();coordinator(scans).ensureScheduled();
        assertThat(scans.recent()).hasSize(1);assertThat(coordinator.submit(new Request(Market.ALL),"request-1").runId()).isEqualTo(run.runId());
        assertThatThrownBy(() -> coordinator.submit(new Request(Market.KOSPI),"other")).hasMessageContaining("대기");
        assertThat(jdbc.queryForObject("SELECT run_id FROM ua_eod_schedule",String.class)).isEqualTo(run.runId());
    }
    @Test void queuedScheduledRunDoesNotOverwriteDifferentManualMarketRun() {
        coordinator.submit(new Request(Market.KOSPI),"kospi");coordinator.ensureScheduled();
        assertThat(scans.recent()).hasSize(2);coordinator.ensureScheduled();assertThat(scans.recent()).hasSize(2);
    }
    @Test void catchupAcrossMidnightAndExpiredWindowAreRecorded() {
        clock.time=Instant.parse("2026-10-13T18:00:00Z");coordinator.ensureScheduled();
        assertThat(scans.recent()).singleElement().extracting(Run::baseDate).isEqualTo(DAY);
        clock.time=Instant.parse("2026-10-14T20:00:00Z");coordinator.ensureScheduled();
        assertThat(scans.recent()).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT status FROM ua_eod_schedule WHERE trade_date=DATE '2026-10-14'",String.class)).isEqualTo("MISSED_WINDOW");
    }
    @Test void holidaysAndUnknownCoverageDoNotRunOrGuess() {
        for(var date:List.of(LocalDate.of(2026,7,17),LocalDate.of(2026,9,24),LocalDate.of(2026,9,25),LocalDate.of(2026,10,9),LocalDate.of(2026,10,10)))
            assertThat(calendar.day(date).status()).isEqualTo(TradingCalendar.Status.CLOSED);
        clock.time=Instant.parse("2026-10-09T11:01:00Z");coordinator.ensureScheduled();assertThat(scans.recent()).isEmpty();
        clock.time=Instant.parse("2027-01-04T11:01:00Z");coordinator.ensureScheduled();assertThat(scans.recent()).isEmpty();
        assertThatThrownBy(this::submit).hasMessageContaining("달력");
        verifyNoInteractions(provider,universe);
    }
    @Test void fridayWorkIsPausedOverTheWeekendAndExpiredBeforeAnyMondayCollection(){
        clock.time=Instant.parse("2026-10-23T11:11:00Z");var run=submit();
        clock.time=Instant.parse("2026-10-23T15:00:00Z");
        coordinator.ensureScheduled();coordinator.tick();
        assertThat(scans.find(run.runId()).orElseThrow().status()).isEqualTo("READY");
        assertThat(jdbc.queryForObject("SELECT heartbeat_epoch FROM ua_eod_lease WHERE lease_id=1",Long.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_eod_schedule",Long.class)).isZero();
        assertThat(submit().runId()).isEqualTo(run.runId());
        assertThatThrownBy(()->coordinator.submit(new Request(Market.ALL),"weekend-new-request")).hasMessageContaining("주말");
        assertThatThrownBy(()->coordinator.resume(run.runId())).hasMessageContaining("주말");
        clock.time=Instant.parse("2026-10-25T14:59:59Z");coordinator.tick();verifyNoInteractions(provider,universe);
        clock.time=Instant.parse("2026-10-25T15:00:00Z");coordinator.tick();
        assertThat(scans.find(run.runId()).orElseThrow().errorCode()).isEqualTo("RUN_DEADLINE_EXCEEDED");
        verifyNoInteractions(provider,universe);
    }
    @Test void liveLeaseBlocksOtherWorkerAndRecoveryAndExpiredOwnerCannotWrite() {
        var run=submit();var other=new EodStore(source);
        assertThat(jobs.acquire("owner-a",Duration.ofMinutes(2))).isTrue();
        coordinator.tick();verifyNoInteractions(provider,universe);
        assertThatThrownBy(() -> coordinator.recover(run.runId())).hasMessageContaining("실행 중");
        assertThat(other.acquire("owner-b",Duration.ofMinutes(2))).isFalse();
        assertThat(jobs.heartbeat("owner-a",Duration.ofMinutes(2))).isTrue();
        jdbc.update("UPDATE ua_eod_lease SET expires_epoch=0");
        assertThat(other.acquire("owner-b",Duration.ofMinutes(2))).isTrue();
        assertThatThrownBy(() -> jobs.owned("owner-a",() -> scans.state(run.runId(),"FAILED","STALE_WRITE",0L))).isInstanceOf(EodStore.LeaseLost.class);
        assertThat(scans.find(run.runId()).orElseThrow().status()).isEqualTo("READY");
    }
    @Test void engineVersionChangeRefusesAutomaticResume() {
        var run=submit();jdbc.update("UPDATE ua_scan_run SET engine_version='old' WHERE run_id=?",run.runId());coordinator.tick();
        assertThat(scans.find(run.runId()).orElseThrow().errorCode()).isEqualTo("ENGINE_VERSION_CHANGED");verifyNoInteractions(provider,universe);
    }
    @Test void summaryDetailAndInputRollbackTogether() {
        var broken=spy(scans);
        doAnswer(i -> {i.callRealMethod();throw new IllegalArgumentException("save failed");}).when(broken).complete(anyString(),any(),any());
        coordinator=coordinator(broken);var run=submit();sampleBoth();finishBoth();
        assertThat(scans.find(run.runId()).orElseThrow().completed()).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_scan_item WHERE result_json IS NOT NULL",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_eod_item WHERE input_json IS NOT NULL",Integer.class)).isZero();
    }
    @Test void changedDailyHistoryRequiresAnotherStableSample() {
        var run=submit();sampleBoth();
        var changed=data("000660",DAY,DAY);var bars=new ArrayList<>(changed.input().dailyCandles());
        var last=bars.remove(bars.size()-1);bars.add(new DailyCandle(DAY,last.open(),last.high(),last.low(),last.close().add(BigDecimal.ONE),last.volume()));
        when(provider.collect("000660",AnalysisMode.SWING,DAY)).thenReturn(new CollectedAnalysisData(new AnalysisInput(changed.input().stock(),AnalysisMode.SWING,DAY,clock.instant(),bars,Map.of()),List.of(),List.of(),DataQualityStatus.COMPLETE));
        clock.advance(Duration.ofSeconds(31));coordinator.tick();assertThat(scans.detail(run.runId(),"000660")).isEmpty();
        coordinator.tick();clock.advance(Duration.ofSeconds(31));coordinator.tick();assertThat(scans.detail(run.runId(),"000660")).isPresent();
    }
    @Test void deadlineTerminatesUnprocessedRunWithoutInfiniteRetry() {
        var run=submit();clock.advance(Duration.ofHours(13));coordinator.tick();
        assertThat(scans.find(run.runId()).orElseThrow().status()).isEqualTo("FAILED");
        assertThatThrownBy(() -> coordinator.resume(run.runId())).hasMessageContaining("기한");verifyNoInteractions(provider,universe);
    }

    @Test void dueConfirmationPrecedesUntouchedUniverseSoLargeRunsDoNotStarveIt() {
        var run=submit();coordinator.tick();coordinator.tick();
        clock.advance(Duration.ofSeconds(31));coordinator.tick();
        assertThat(scans.detail(run.runId(),"000660")).isPresent();
        verify(provider,never()).collect("005930",AnalysisMode.SWING,DAY);
    }

    @Test void simultaneousScheduledRegistrationCreatesOneRun() throws Exception {
        var workers=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);
        try {
            var first=workers.submit(() -> {gate.await();coordinator.ensureScheduled();return null;});
            var second=workers.submit(() -> {gate.await();coordinator(scans).ensureScheduled();return null;});
            gate.countDown();first.get(5,TimeUnit.SECONDS);second.get(5,TimeUnit.SECONDS);
            assertThat(scans.recent()).hasSize(1);
        } finally {workers.shutdownNow();}
    }

    @Test void cancelledCollectionDoesNotCommitAndRestartCanContinue() {
        var run=submit();sampleBoth();clock.advance(Duration.ofSeconds(31));
        when(provider.collect("000660",AnalysisMode.SWING,DAY)).thenAnswer(i -> {
            Thread.currentThread().interrupt();return data("000660",DAY,DAY);
        });
        try {coordinator.tick();assertThat(scans.detail(run.runId(),"000660")).isEmpty();}
        finally {Thread.interrupted();}
        doReturn(data("000660",DAY,DAY)).when(provider).collect("000660",AnalysisMode.SWING,DAY);
        coordinator=coordinator(scans);coordinator.tick();
        assertThat(scans.detail(run.runId(),"000660")).isPresent();
    }

    @Test void normalizedAmountChangesResetStabilityButObservationTimestampsDoNot() {
        var amount=new java.util.concurrent.atomic.AtomicReference<>("100");
        when(provider.collect(anyString(),any(),any())).thenAnswer(i -> {
            String code=i.getArgument(0);var data=data(code,DAY,DAY);
            var record=new com.example.stockanalysis.backdata.validation.model.BackdataRecord("ka10081",
                    com.example.stockanalysis.backdata.validation.model.BackdataDatasetType.DAILY_CANDLE,code,null,null,DAY,clock.instant(),Map.of("tradingAmount",amount.get()));
            var payload=new com.example.stockanalysis.analysis.application.model.AnalysisSourcePayload("ka10081","response","DAILY",Map.of(),null,null,List.of(record),clock.instant(),clock.instant());
            return new CollectedAnalysisData(data.input(),List.of(payload),List.of(),DataQualityStatus.COMPLETE);
        });
        var run=submit();sampleBoth();amount.set("200");clock.advance(Duration.ofSeconds(31));coordinator.tick();
        assertThat(scans.detail(run.runId(),"000660")).isEmpty();
        clock.advance(Duration.ofSeconds(31));coordinator.tick();coordinator.tick();
        assertThat(scans.detail(run.runId(),"000660")).isPresent();
        assertThat(scans.detail(run.runId(),"000660").orElseThrow().tradingAmountMillionWon()).isEqualByComparingTo("200");
    }

    @Test void migrationPreservesLegacyResultAndMissingEodMetadata() throws Exception {
        var legacy=new DriverManagerDataSource("jdbc:h2:mem:legacy_"+UUID.randomUUID()+";MODE=Oracle;DB_CLOSE_DELAY=-1","sa","");
        var ddl=Files.readString(Path.of("..","database","oracle","ddl","11_unified_analysis.sql"));
        var migration=Files.readString(Path.of("..","database","oracle","migration","20261013_01_unified_eod.sql"));
        new ResourceDatabasePopulator(new ByteArrayResource(ddl.substring(0,ddl.indexOf("-- Apply once before deploying the EOD")).getBytes(java.nio.charset.StandardCharsets.UTF_8))).execute(legacy);
        var db=new JdbcTemplate(legacy);
        db.update("INSERT INTO ua_scan_run(run_id,request_key,market,base_date,engine_version,status,created_epoch,initialized) VALUES('legacy','legacy','ALL',DATE '2026-10-12','old','COMPLETED',1,1)");
        var captured=analyzer.analyzeEod("005930",DAY,LocalDateTime.of(2026,10,13,20,11),data("005930",DAY,DAY),LocalDateTime.of(2026,10,13,20,0)).analysis();
        var tree=mapper.valueToTree(captured);((com.fasterxml.jackson.databind.node.ObjectNode)tree).remove(java.util.List.of("eod", "tradeJudgment"));
        var oldJson=mapper.writeValueAsString(tree);
        db.update("INSERT INTO ua_scan_item(run_id,stock_code,stock_name,market,status,result_json) VALUES('legacy','005930','삼성전자','KOSPI','COMPLETED',?)",oldJson);
        new ResourceDatabasePopulator(new ByteArrayResource(migration.getBytes(java.nio.charset.StandardCharsets.UTF_8))).execute(legacy);
        var store=new UnifiedScanStore(legacy,mapper);
        assertThat(store.find("legacy").orElseThrow().eod()).isNull();
        assertThat(store.detail("legacy","005930").orElseThrow().eod()).isNull();
        assertThat(store.detail("legacy","005930").orElseThrow().tradeJudgment()).isNull();
        assertThat(db.queryForObject("SELECT result_json FROM ua_scan_item",String.class)).isEqualTo(oldJson);
    }

    @Test void springWiresManualExecutionThroughDurableCoordinatorEvenWithSchedulerDisabled() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withBean(com.example.stockanalysis.holdings.HoldingService.class,()->mock(com.example.stockanalysis.holdings.HoldingService.class))
                .withUserConfiguration(com.example.stockanalysis.unified.config.UnifiedAnalysisConfiguration.class,
                        com.example.stockanalysis.unified.config.UnifiedScheduleConfiguration.class,EodConfiguration.class,
                        EodCoordinator.class,EodStore.class,TradingCalendar.class,UnifiedScanStore.class,UnifiedScanService.class,UnifiedAnalysisService.class)
                .withBean(javax.sql.DataSource.class,() -> source).withBean(ObjectMapper.class,() -> mapper).withBean(Clock.class,() -> clock)
                .withBean(AnalysisDataProvider.class,() -> provider).withBean(ScreenerDataProvider.class,() -> universe)
                .withBean(ScreenerStockTypeCatalog.class,() -> mock(ScreenerStockTypeCatalog.class))
                .withBean(com.example.stockanalysis.storage.SnapshotRepository.class,()->mock(com.example.stockanalysis.storage.SnapshotRepository.class))
                .withPropertyValues("unified.analysis-schedule.enabled=false","unified.eod.worker-enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed().doesNotHaveBean(UnifiedAnalysisScheduler.class).doesNotHaveBean(EodConfiguration.Worker.class);
                    var run=context.getBean(UnifiedScanService.class).submit(new Request(Market.ALL),"spring-manual");
                    assertThat(run.eod()).isNotNull();assertThat(run.baseDate()).isEqualTo(DAY);
                    verifyNoInteractions(provider,universe);
                });
    }
    @Test void sharedSnapshotsKeepEodReportAndUnifiedDetailsReadable() throws Exception {
        for(String file:List.of("08_screener.sql","18_snapshot_storage.sql"))new ResourceDatabasePopulator(new FileSystemResource(Path.of("..","database","oracle","ddl",file))).execute(source);
        var props=new com.example.stockanalysis.storage.StorageProperties(true,false,"",90,100,true,false,0,80,90,14,false,5);
        var repository=new com.example.stockanalysis.storage.SnapshotRepository(source,mapper,props);
        scans=new UnifiedScanStore(source,mapper,repository);jobs=new EodStore(source,repository);coordinator=coordinator(scans);
        var run=submit();sampleBoth();finishBoth();
        assertThat(scans.find(run.runId()).orElseThrow().status()).isEqualTo("COMPLETED");
        assertThat(scans.detail(run.runId(),"005930").orElseThrow().result().priceDate()).isEqualTo(DAY);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_eod_item WHERE input_snapshot_id IS NOT NULL AND input_json IS NULL AND input_gzip IS NULL",Long.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_scan_item WHERE result_snapshot_id IS NOT NULL AND result_json IS NULL",Long.class)).isEqualTo(2);
        var reports=new com.example.stockanalysis.unified.report.ReportStore(source,mapper,repository);
        assertThat(reports.input(run.runId(),"005930").path("input").path("stock").path("stockCode").asText()).isEqualTo("005930");
        new EodStorageStartupCheck(source,repository).afterSingletonsInstantiated();
    }
}
