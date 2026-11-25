package com.example.stockanalysis.unified.performance;

import static com.example.stockanalysis.unified.performance.PerformanceFixtures.*;
import static com.example.stockanalysis.unified.performance.PerformanceModels.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.stockanalysis.unified.eod.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class PerformanceServiceTest {
    static class MutableClock extends Clock {Instant time=NOW;public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return Clock.fixed(time,zone);}public Instant instant(){return time;}}
    DriverManagerDataSource db;JdbcTemplate jdbc;PerformanceStore store;PerformanceSources sources;MutableClock clock;
    PerformanceService service;Input input;List<Observation> observed;
    @BeforeEach void setup() throws Exception {
        db=new DriverManagerDataSource("jdbc:h2:mem:performance_"+UUID.randomUUID()+";MODE=Oracle;DB_CLOSE_DELAY=-1","sa","");
        for(var file:List.of("11_unified_analysis.sql","14_daily_market_report.sql","15_report_performance.sql"))
            new ResourceDatabasePopulator(new ByteArrayResource(Files.readAllBytes(Path.of("..","database","oracle","ddl",file)))).execute(db);
        jdbc=new JdbcTemplate(db);store=new PerformanceStore(db,JSON);sources=mock(PerformanceSources.class);clock=new MutableClock();
        input=input(UUID.randomUUID().toString(),candidate("000001","ENTRY_CONDITIONAL"));observed=observations(input);
        jdbc.update("INSERT INTO ua_scan_run(run_id,request_key,market,base_date,engine_version,status,created_epoch) VALUES(?,?,'ALL',?,'v1','COMPLETED',?)",input.report().runId(),input.report().runId(),java.sql.Date.valueOf(DAY),NOW.toEpochMilli());
        jdbc.update("INSERT INTO ua_daily_report(run_id,status,policy_version,policy_json,created_epoch,updated_epoch,deadline_epoch,report_json) VALUES(?,'READY','v1','{}',?,?,?,?)",input.report().runId(),NOW.toEpochMilli(),NOW.toEpochMilli(),NOW.plusSeconds(3600).toEpochMilli(),JSON.writeValueAsString(input.report()));
        when(sources.report(input.report().runId())).thenReturn(input.report());
        when(sources.anchor(anyString(),anyString(),anyString(),any())).thenReturn(input.anchors().get(0));
        when(sources.stock(any(),any(),any())).thenAnswer(i->observed.stream().filter(o->o.kind().equals("STOCK")&&o.date().equals(i.getArgument(1))&&!o.collectedAt().isAfter((Instant)i.getArgument(2))).findFirst());
        when(sources.index(any(),any(),any(),any())).thenAnswer(i->observed.stream().filter(o->o.kind().equals("INDEX")&&o.date().equals(i.getArgument(2))&&!o.collectedAt().isAfter((Instant)i.getArgument(3))).findFirst());
        service=service(true,store,sources);
    }
    PerformanceService service(boolean enabled,PerformanceStore persistence,PerformanceSources source){
        var settings=new PerformanceProperties(enabled,bd("30"),bd("5"),bd("0.3"));
        var eod=new EodProperties("classpath:unified-calendar-2026.csv",Duration.ofHours(8),Duration.ofMinutes(10),Duration.ofSeconds(30),Duration.ofMinutes(90),Duration.ofHours(12),Duration.ofMinutes(1),3,Duration.ofMinutes(2));
        return new PerformanceService(settings,persistence,source,new PerformanceEngine(),new TradingCalendar(eod,new DefaultResourceLoader()),clock);
    }
    String id(){return input.report().runId();}
    @Test void disabledFeatureNeverTouchesNewOrExistingData(){
        var persistence=mock(PerformanceStore.class);var source=mock(PerformanceSources.class);var off=service(false,persistence,source);
        off.tick();assertThat(off.get("any").state()).isEqualTo("DISABLED");
        assertThatThrownBy(()->off.request("any")).isInstanceOf(com.example.stockanalysis.shared.exception.BusinessException.class);
        verifyNoInteractions(persistence,source);
    }
    @Test void automaticallyRegistersThenReusesFrozenInputAcrossRestartAndPreservesOriginalReport(){
        var original=jdbc.queryForObject("SELECT report_json FROM ua_daily_report",String.class);
        service.tick();assertThat(service.get(id()).state()).isEqualTo("WAITING_DATA");
        var frozen=jdbc.queryForObject("SELECT input_json FROM ua_eval_run",String.class);
        assertThat(service.get(id()).result().captureMode()).isEqualTo("FORWARD_CAPTURE");
        clock.time=finalTime();service=service(true,store,sources);service.tick();
        var view=service.get(id());assertThat(view.state()).isEqualTo("COMPLETE");assertThat(view.result().metrics().closed()).isEqualTo(1);
        service.request(id());service.tick();assertThat(service.get(id()).revision()).isEqualTo(view.revision());
        assertThat(jdbc.queryForObject("SELECT input_json FROM ua_eval_run",String.class)).isEqualTo(frozen);
        assertThat(jdbc.queryForObject("SELECT report_json FROM ua_daily_report",String.class)).isEqualTo(original);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_report_delivery",Integer.class)).isZero();
    }
    @Test void existingV1JobContinuesWithItsFrozenPolicy(){
        var p=input.policy();
        var legacy=new Input(input.report(),new Policy("forward-evaluation-v1",p.roundTripFeeBps(),p.slippageBps(),p.neutralBandPercent(),p.holdingDays(),p.calendarVersion()),input.dates(),input.anchors(),input.captureMode(),input.registeredAt());
        store.register(legacy,finalTime().plusSeconds(86400).toEpochMilli());
        clock.time=finalTime();service.tick();
        assertThat(service.get(id()).state()).isEqualTo("COMPLETE");
        assertThat(service.get(id()).result().stocks().get(0).horizons()).extracting(Horizon::days).containsExactly(5,10,20);
        verify(sources,times(1)).index(any(),any(),any(),any());
    }
    @Test void retrospectiveRegistrationIsLabeledAndMissingHistoryNeverLooksComplete(){
        clock.time=finalTime().plus(Duration.ofDays(15));observed=List.of();service.tick();
        var view=service.get(id());assertThat(view.state()).isEqualTo("DATA_INCOMPLETE");
        assertThat(view.result().captureMode()).isEqualTo("RETROSPECTIVE");assertThat(view.result().metrics().winRatePercent()).isNull();
    }
    @Test void pollingWithNoNewDataDoesNotCreateAnotherRevision(){
        service.tick();int revision=service.get(id()).revision();clock.time=clock.time.plusSeconds(901);service.tick();
        assertThat(service.get(id()).revision()).isEqualTo(revision);
    }
    @Test void reportsFinalizedInTheFutureOrAfterEntryOpenAreRejectedBeforeRegistration(){
        clock.time=NOW.minusSeconds(1);
        assertThatThrownBy(()->service.request(id())).isInstanceOf(com.example.stockanalysis.shared.exception.BusinessException.class);
        var old=input.report();var late=collected(dates().get(0));clock.time=late;
        var changed=new com.example.stockanalysis.unified.report.ReportModels.DailyReport(old.runId(),old.status(),old.tradeDate(),old.forecastDate(),late,late,old.version(),old.validationStatus(),old.scope(),old.markets(),old.candidates(),old.assessedStocks(),old.eligibleStocks(),old.limitations());
        when(sources.report(id())).thenReturn(changed);
        assertThatThrownBy(()->service.request(id())).isInstanceOf(com.example.stockanalysis.shared.exception.BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_eval_run",Integer.class)).isZero();
    }
    @Test void concurrentRegistrationAndExpiredLeaseCannotDuplicateOrOverwriteEvidence() throws Exception {
        var pool=Executors.newFixedThreadPool(2);
        try{var a=pool.submit(()->service.request(id()));var b=pool.submit(()->service.request(id()));a.get();b.get();}finally{pool.shutdownNow();}
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_eval_run",Integer.class)).isEqualTo(1);
        var first=store.claim(clock.millis()).orElseThrow();jdbc.update("UPDATE ua_eval_run SET lease_epoch=0");
        var second=store.claim(clock.millis()).orElseThrow();
        assertThatThrownBy(()->store.observe(first,observed.get(0))).isInstanceOf(PerformanceStore.LostOwnership.class);
        store.observe(second,observed.get(0));
        var changed=new Observation("STOCK","000001",observed.get(0).date(),"PRICE_BASIS_CHANGED",null,"different",observed.get(0).collectedAt(),"changed");
        store.observe(second,changed);assertThat(store.observations(id())).containsExactly(observed.get(0));
    }
    @Test void revisionFailureRollsBackResultAndRetainsPreviousRevision(){
        service.tick();var previous=service.get(id());
        jdbc.update("ALTER TABLE ua_eval_revision ADD CONSTRAINT block_next_revision CHECK(revision<2)");
        clock.time=finalTime();service.tick();
        assertThat(service.get(id()).state()).isEqualTo("RETRY_WAIT");assertThat(service.get(id()).revision()).isEqualTo(previous.revision());
        assertThat(service.get(id()).result()).isEqualTo(previous.result());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_eval_revision",Integer.class)).isEqualTo(1);
    }
    @Test void failedRegistrationIsDeferredWithoutStarvationAndCanRecover(){
        when(sources.anchor(anyString(),anyString(),anyString(),any())).thenThrow(new IllegalArgumentException());service.tick();
        assertThat(service.get(id()).state()).isEqualTo("FAILED");assertThat(store.discover(clock.millis())).isEmpty();
        doReturn(input.anchors().get(0)).when(sources).anchor(anyString(),anyString(),anyString(),any());clock.time=clock.time.plusSeconds(901);service.tick();
        assertThat(service.get(id()).state()).isEqualTo("WAITING_DATA");
    }
    @Test void repeatedCalculationFailuresStopAfterThreeAttempts(){
        service.request(id());clock.time=finalTime();when(sources.stock(any(),any(),any())).thenThrow(new IllegalStateException());
        service.tick();clock.time=clock.time.plusSeconds(61);service.tick();clock.time=clock.time.plusSeconds(121);service.tick();
        assertThat(service.get(id()).state()).isEqualTo("FAILED");
        verify(sources,times(3)).stock(any(),any(),any());
    }
}
