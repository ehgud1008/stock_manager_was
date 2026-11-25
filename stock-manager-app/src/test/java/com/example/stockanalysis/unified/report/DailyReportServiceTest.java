package com.example.stockanalysis.unified.report;

import static com.example.stockanalysis.unified.report.ReportFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.stockanalysis.unified.eod.*;
import com.example.stockanalysis.unified.scan.*;
import com.example.stockanalysis.screener.domain.ScreenerItem.Stock;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class DailyReportServiceTest {
    static class MutableClock extends Clock {Instant time=NOW;public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return Clock.fixed(time,zone);}public Instant instant(){return time;}void advance(long seconds){time=time.plusSeconds(seconds);}}
    MutableClock clock;DriverManagerDataSource db;JdbcTemplate jdbc;ReportStore store;UnifiedScanStore scans;
    DailyReportService service;MarketDataProvider provider;TelegramProperties telegram;ReportDeliveryService delivery;TelegramGateway gateway;
    @BeforeEach void setup() throws Exception {
        clock=new MutableClock();db=new DriverManagerDataSource("jdbc:h2:mem:report_"+UUID.randomUUID()+";MODE=Oracle;DB_CLOSE_DELAY=-1","sa","");
        for(String file:List.of("11_unified_analysis.sql","14_daily_market_report.sql"))new ResourceDatabasePopulator(new ByteArrayResource(Files.readAllBytes(Path.of("..","database","oracle","ddl",file)))).execute(db);
        jdbc=new JdbcTemplate(db);store=new ReportStore(db,MAPPER);scans=new UnifiedScanStore(db,MAPPER);
        provider=mock(MarketDataProvider.class);when(provider.collect(DAY)).thenReturn(market());
        telegram=new TelegramProperties(true,"12345:test_token","123456","https://stocks.example");gateway=mock(TelegramGateway.class);
        when(gateway.send(anyString(),anyString())).thenReturn(new TelegramGateway.Result("SENT","1",null,0));
        service=service(store,settings());delivery=new ReportDeliveryService(store,telegram,gateway,clock);
    }
    DailyReportService service(ReportStore persistence,ReportProperties properties){
        var eod=new EodProperties("classpath:unified-calendar-2026.csv",Duration.ofHours(8),Duration.ofMinutes(10),Duration.ofSeconds(30),Duration.ofMinutes(90),Duration.ofHours(12),Duration.ofMinutes(1),3,Duration.ofMinutes(2));
        return new DailyReportService(persistence,scans,provider,new CandidateAnalyzer(MAPPER,properties),new TradingCalendar(eod,new DefaultResourceLoader()),properties,telegram,clock);
    }
    String run(boolean qualifies,String state) throws Exception {
        String id=UUID.randomUUID().toString();scans.create(id,id,"ALL",DAY,"eod-test",NOW.toEpochMilli());
        new EodStore(db).create(id,"SCHEDULED",DAY.atTime(20,0).atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli(),NOW.toEpochMilli(),NOW.plusSeconds(43200).toEpochMilli(),"KRX-2026-v1","EOD",NOW.toEpochMilli());
        scans.initialize(id,List.of(new Stock("005930","삼성전자","KOSPI","",DAY.minusYears(5),"ST"),new Stock("000660","테스트","KOSDAQ","",DAY.minusYears(5),"ST")));
        new EodStore(db).initializeItems(id);
        for(String code:List.of("005930","000660")){
            var data=bundle(code,qualifies);var item=scans.getItem(id,code);
            scans.complete(id,UnifiedScanService.summarize(item,data.detail()),data.detail());
            new EodStore(db).snapshot(id,code,MAPPER.writeValueAsString(data.input()),NOW.toEpochMilli(),NOW.toEpochMilli());
        }
        scans.state(id,state,null,NOW.toEpochMilli());return id;
    }
    void finish(){service.tick();clock.advance(2);service.tick();}
    @Test void tierListsPersistSeparatelyAndPortfolioRestoresOnlyEligibleB() throws Exception {
        var id=run(true,"COMPLETED");
        for(var code:List.of("005930","000660")) {
            var data=bundle(code,true);var tree=MAPPER.valueToTree(data.detail());
            var a=(com.fasterxml.jackson.databind.node.ObjectNode)tree.path("result").path("analysis");
            if(code.equals("005930")){a.put("totalScore",75);((com.fasterxml.jackson.databind.node.ObjectNode)a.path("strategy")).put("action","WATCH");}
            else ((com.fasterxml.jackson.databind.node.ObjectNode)a.path("signalReport")).set("signals",MAPPER.createArrayNode());
            var detail=MAPPER.treeToValue(tree,com.example.stockanalysis.unified.application.UnifiedAnalysisResponse.class);
            scans.complete(id,UnifiedScanService.summarize(scans.getItem(id,code),detail),detail);
        }
        finish();var r=service.get(id).report();assertThat(r.candidates()).isEmpty();
        assertThat(r.tierLists().comparisonBTotal()).isEqualTo(1);assertThat(r.tierLists().signalWaitingTotal()).isEqualTo(1);
        assertThat(r.tierLists().activeSignalsTotal()).isEqualTo(1);
        assertThat(r.tierLists().activeSignals()).extracting(ReportModels.StockDecision::code).containsExactly("005930");
        assertThat(store.evidence(id,"005930").orElseThrow().disposition()).isEqualTo("RESEARCH_B");
        var prices=mock(com.example.stockanalysis.unified.performance.PerformanceSources.class);
        var candle=bundle("005930",true).detail().priceHistory().get(99);
        when(prices.anchor(anyString(),anyString(),anyString(),any())).thenReturn(new com.example.stockanalysis.unified.performance.PerformanceModels.Anchor("005930","KOSPI","KIWOOM_KRX_DAILY_ADJUSTED",candle));
        var cohort=new com.example.stockanalysis.unified.portfolio.PortfolioSources(db,MAPPER,prices).cohort(id,clock.instant());
        assertThat(cohort.baseline()).isEmpty();assertThat(cohort.comparisons().get("TIER_A_B")).singleElement().satisfies(s->{
            assertThat(s.code()).isEqualTo("005930");assertThat(s.disposition()).isEqualTo("ENTRY_CONDITIONAL");assertThat(s.newEntry().suitability().tier()).isEqualTo("B");
        });
        assertThat(cohort.comparisons().get("TIER_B_ONLY")).hasSize(1);
        assertThat(service.get(id).report()).isEqualTo(r);
        assertThat(jdbc.queryForObject("SELECT message_text FROM ua_report_delivery WHERE run_id=?",String.class,id)).contains("B등급 비교 1종목","신호 대기 1종목","A/B등급 유효 매수 신호 1종목");
    }
    @Test void persistsCommonMarketOnceAndAtomicReportEvidenceOutboxThenSendsOnce() throws Exception {
        var id=run(true,"COMPLETED");finish();var view=service.get(id);
        assertThat(view.state()).isEqualTo("READY");assertThat(view.report().candidates()).hasSize(2);
        assertThat(view.report().version()).startsWith(CandidateAnalyzer.VERSION+"/"+com.example.stockanalysis.judgment.NewEntryEvaluator.VERSION+"/");
        assertThat(view.report().version()).hasSizeLessThanOrEqualTo(100);
        assertThat(view.report().candidates()).allSatisfy(s->{
            assertThat(s.newEntry().status()).isEqualTo(com.example.stockanalysis.judgment.NewEntryAssessment.Status.ELIGIBLE);
            assertThat(s.scenario()).isEqualTo(s.newEntry().effectiveScenario());
        });
        assertThat(view.report().forecastDate()).isEqualTo(DAY.plusDays(1));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_candidate_evidence",Integer.class)).isEqualTo(2);
        service.request(id);service.tick();delivery.tick();delivery.tick();
        verify(provider,times(1)).collect(DAY);verify(gateway,times(1)).send(eq("123456"),contains("조건부 진입"));
        assertThat(service.get(id).notificationState()).isEqualTo("SENT");
    }
    @Test void neutralMarketDoesNotHideOtherwiseQualifiedStocks() throws Exception {
        var snapshot=market();
        var flat=snapshot.indices().stream().map(i->new ReportModels.IndexSnapshot(i.market(),i.code(),i.unit(),i.tradeDate(),i.collectedAt(),
                i.bars().stream().map(b->new ReportModels.IndexBar(b.date(),java.math.BigDecimal.TEN,java.math.BigDecimal.TEN,java.math.BigDecimal.TEN,java.math.BigDecimal.TEN)).toList(),i.source(),i.warnings())).toList();
        when(provider.collect(DAY)).thenReturn(new ReportModels.MarketSnapshot(flat,NOW));
        var id=run(true,"COMPLETED");finish();
        assertThat(service.get(id).report().candidates()).hasSize(2).allMatch(s->s.disposition().equals("ENTRY_CONDITIONAL"));
        assertThat(service.get(id).report().candidates()).allMatch(s->s.newEntry()!=null);
        assertThat(jdbc.queryForObject("SELECT message_text FROM ua_report_delivery WHERE run_id=?",String.class,id))
                .contains("조건부 진입 2종목");
    }
    @Test void partialCoverageStillPublishesAndEmptyMarketIsUnknown() throws Exception {
        var id=run(false,"COMPLETED");finish();
        assertThat(service.get(id).state()).isEqualTo("READY");assertThat(service.get(id).report().candidates()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT message_text FROM ua_report_delivery WHERE run_id=?",String.class,id)).contains("오늘은 조건을 충족한 종목이 없습니다").doesNotContain("실패");
        var broken=run(true,"COMPLETED_WITH_ERRORS");jdbc.update("UPDATE ua_scan_item SET status='FAILED' WHERE run_id=? AND stock_code='005930'",broken);
        finish();assertThat(service.get(broken).state()).isEqualTo("READY");
        assertThat(service.get(broken).report().candidates()).hasSize(1);
        var emptyMarket=service.get(broken).report().markets().stream().filter(m->m.market().equals("KOSPI")).findFirst().orElseThrow();
        assertThat(emptyMarket.bias()).isEqualTo("판단 보류");assertThat(emptyMarket.advancingPercent()).isNull();
        assertThat(jdbc.queryForObject("SELECT message_text FROM ua_report_delivery WHERE run_id=?",String.class,broken)).contains("분석 범위 0/1종목","미확인").doesNotContain("작업에 실패");
    }
    @Test void noValidStockDataStillFailsInsteadOfReportingNoCandidates() throws Exception {
        var broken=run(true,"COMPLETED_WITH_ERRORS");jdbc.update("UPDATE ua_scan_item SET status='FAILED' WHERE run_id=?",broken);
        finish();assertThat(service.get(broken).errorCode()).isEqualTo("NO_VALID_STOCK_DATA");
        assertThat(jdbc.queryForObject("SELECT message_text FROM ua_report_delivery WHERE run_id=?",String.class,broken)).isEqualTo("오늘 종합분석 작업에 실패했습니다");
    }
    @Test void halfCoverageInBothMarketsDoesNotBlockReport() throws Exception {
        var id=run(true,"COMPLETED_WITH_ERRORS");
        for(String market:List.of("KOSPI","KOSDAQ"))
            jdbc.update("INSERT INTO ua_scan_item(run_id,stock_code,stock_name,market,status) VALUES(?,?,?,?,?)",id,market.equals("KOSPI")?"999991":"999992","실패 표본",market,"FAILED");
        finish();var report=service.get(id).report();
        assertThat(service.get(id).state()).isEqualTo("READY");
        assertThat(report.assessedStocks()).isEqualTo(2);assertThat(report.eligibleStocks()).isEqualTo(4);
        assertThat(report.markets()).allMatch(m->m.validStocks()==1&&m.eligibleStocks()==2);
        assertThat(report.candidates()).hasSize(2);
    }
    @Test void waitsForScanCompletionAndRestartReusesMarketSnapshot() throws Exception {
        var id=run(true,"RUNNING");service.tick();clock.advance(2);service.tick();
        assertThat(service.get(id).state()).isEqualTo("WAITING_SCAN");
        service=service(store,settings());scans.state(id,"COMPLETED",null,clock.millis());clock.advance(31);service.tick();
        assertThat(service.get(id).state()).isEqualTo("READY");verify(provider,times(1)).collect(DAY);
    }
    @Test void wholeScanFailureSkipsMarketAndSendsOnlyFailure() throws Exception {
        var id=run(true,"FAILED");service.tick();delivery.tick();
        assertThat(service.get(id).state()).isEqualTo("FAILED");verifyNoInteractions(provider);verify(gateway).send("123456","오늘 종합분석 작업에 실패했습니다");
    }
    @Test void recoveredFailedScanStillNotifiesAfterNextMarketOpen() throws Exception {
        var id=run(true,"FAILED");clock.advance(13*3600);
        service.tick();delivery.tick();
        assertThat(service.get(id).errorCode()).isEqualTo("SCAN_FAILED");
        verifyNoInteractions(provider);verify(gateway).send("123456","오늘 종합분석 작업에 실패했습니다");
    }
    @Test void disabledReportNeverTouchesNewTablesOrCollectsMarketData() throws Exception {
        var id=run(true,"COMPLETED");var persistence=mock(ReportStore.class);
        when(persistence.json(any())).thenAnswer(invocation->MAPPER.writeValueAsString(invocation.getArgument(0)));
        var configured=settings();
        var disabled=new ReportProperties(false,configured.minimumCoverage(),configured.minimumDailyAmountMillion(),configured.minimumAverageAmountMillion(),configured.minimumNetRewardRisk(),configured.roundTripCostBps(),configured.maximumEma20Distance(),configured.maximumCandidates());
        var off=service(persistence,disabled);clearInvocations(persistence);
        off.tick();assertThat(off.get(id).state()).isEqualTo("DISABLED");
        assertThat(off.library(0,30).enabled()).isFalse();
        assertThatThrownBy(()->off.request(id)).isInstanceOf(com.example.stockanalysis.shared.exception.BusinessException.class);
        assertThatThrownBy(()->off.evidence(id,"005930")).isInstanceOf(com.example.stockanalysis.shared.exception.BusinessException.class);
        verifyNoInteractions(persistence,provider);
    }
    @Test void responseLossAndExpiredSendingClaimsAreNeverAutomaticallyRepeated() throws Exception {
        var id=run(true,"COMPLETED");finish();
        when(gateway.send(anyString(),anyString())).thenReturn(new TelegramGateway.Result("UNKNOWN",null,"TIMEOUT",0));delivery.tick();delivery.tick();
        assertThat(service.get(id).notificationState()).isEqualTo("UNKNOWN");verify(gateway,times(1)).send(anyString(),anyString());
        var second=run(true,"COMPLETED");finish();var claimed=store.claimDelivery(clock.millis(),telegram.chatId()).orElseThrow();
        jdbc.update("UPDATE ua_report_delivery SET lease_epoch=0 WHERE run_id=?",second);delivery.tick();
        assertThat(service.get(second).notificationState()).isEqualTo("UNKNOWN");
        assertThatThrownBy(()->store.delivered(claimed,"SENT","old",null,0)).isInstanceOf(ReportStore.LostOwnership.class);
    }
    @Test void rateLimitHonorsDelayAndStopsAtBudget() throws Exception {
        var id=run(true,"COMPLETED");finish();when(gateway.send(anyString(),anyString())).thenReturn(new TelegramGateway.Result("RETRY_WAIT",null,"RATE_LIMIT",30));
        delivery.tick();delivery.tick();verify(gateway,times(1)).send(anyString(),anyString());
        clock.advance(31);delivery.tick();clock.advance(31);delivery.tick();delivery.tick();
        assertThat(service.get(id).notificationState()).isEqualTo("FAILED");verify(gateway,times(3)).send(anyString(),anyString());
    }
    @Test void simultaneousRegistrationAndLeaseFencingProtectOneImmutableReport() throws Exception {
        var id=run(true,"COMPLETED");var pool=Executors.newFixedThreadPool(2);
        try{var a=pool.submit(()->service.request(id));var b=pool.submit(()->service(store,settings()).request(id));a.get();b.get();}finally{pool.shutdownNow();}
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_daily_report",Integer.class)).isEqualTo(1);
        var first=store.claim(clock.millis()).orElseThrow();jdbc.update("UPDATE ua_daily_report SET lease_epoch=0");var second=store.claim(clock.millis()).orElseThrow();
        assertThatThrownBy(()->store.market(first,market())).isInstanceOf(ReportStore.LostOwnership.class);
        store.market(second,market());store.defer(second,"WAITING_SCAN",null,clock.millis());service.tick();
        assertThat(service.get(id).state()).isEqualTo("READY");
    }
    @Test void missingMarketDataIsBoundedAndNeverCreatesNormalEmptyReport() throws Exception {
        var id=run(true,"COMPLETED");when(provider.collect(DAY)).thenThrow(new IllegalStateException());
        service.tick();clock.advance(61);service.tick();clock.advance(121);service.tick();
        assertThat(service.get(id).state()).isEqualTo("FAILED");verify(provider,times(3)).collect(DAY);
    }
    @Test void disabledDeliveryDoesNotQueueOldMessagesForLaterActivation() throws Exception {
        var id=run(true,"COMPLETED");telegram=new TelegramProperties(false,"","","");service=service(store,settings());finish();
        assertThat(service.get(id).notificationState()).isEqualTo("DISABLED");delivery.tick();verifyNoInteractions(gateway);
    }
    @Test void failureInsideReportCommitRollsBackEvidenceAndOutbox() throws Exception {
        var id=run(true,"COMPLETED");service.request(id);service.tick();clock.advance(2);
        jdbc.update("ALTER TABLE ua_report_delivery ADD CONSTRAINT impossible_message CHECK (notification_kind='IMPOSSIBLE')");service.tick();
        assertThat(service.get(id).state()).isEqualTo("RETRY_WAIT");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_candidate_evidence",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_report_delivery",Integer.class)).isZero();
        assertThat(service.get(id).report()).isNull();
    }
    @Test void libraryFindsLatestReadyIndependentlyOfPendingPageAndNeverCreatesOrSends() throws Exception {
        var ready=run(true,"COMPLETED");finish();
        var original=jdbc.queryForObject("SELECT report_json FROM ua_daily_report WHERE run_id=?",String.class,ready);
        var pending=run(true,"RUNNING");jdbc.update("UPDATE ua_scan_run SET base_date=? WHERE run_id=?",java.sql.Date.valueOf(DAY.plusDays(1)),pending);
        scans.create("manual","manual","ALL",DAY.plusDays(2),"old",clock.millis());
        var first=service.library(0,1);assertThat(first.latestReady().runId()).isEqualTo(ready);
        assertThat(first.newestRun().runId()).isEqualTo(pending);assertThat(first.reports()).hasSize(1);
        assertThat(first.reports().get(0).reportState()).isEqualTo("NOT_CREATED");assertThat(first.hasMore()).isTrue();
        var second=service.library(1,1);assertThat(second.reports().get(0).runId()).isEqualTo(ready);assertThat(second.hasMore()).isFalse();
        assertThat(second.latestReady().runId()).isEqualTo(ready);assertThat(second.newestRun().runId()).isEqualTo(pending);
        assertThat(first.expectedTradeDate()).isEqualTo(DAY);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_daily_report",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT report_json FROM ua_daily_report WHERE run_id=?",String.class,ready)).isEqualTo(original);
        assertThat(jdbc.queryForObject("SELECT message_text FROM ua_report_delivery",String.class)).contains("https://stocks.example/daily-reports?runId="+ready);
        verifyNoInteractions(gateway);
    }
    @Test void libraryEmptyAndInvalidPagesAreDistinct(){
        var library=service.library(0,30);assertThat(library.enabled()).isTrue();assertThat(library.latestReady()).isNull();assertThat(library.newestRun()).isNull();assertThat(library.reports()).isEmpty();
        assertThatThrownBy(()->service.library(-1,30)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.library(0,101)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(provider,gateway);
    }
}
