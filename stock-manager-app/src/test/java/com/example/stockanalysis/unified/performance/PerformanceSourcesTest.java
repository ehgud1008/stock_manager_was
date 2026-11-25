package com.example.stockanalysis.unified.performance;

import static com.example.stockanalysis.unified.performance.PerformanceFixtures.*;
import static org.assertj.core.api.Assertions.*;
import com.example.stockanalysis.engine.model.*;
import com.example.stockanalysis.unified.report.ReportModels.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class PerformanceSourcesTest {
    @org.junit.jupiter.api.io.TempDir Path archiveDirectory;
    JdbcTemplate jdbc;PerformanceSources sources;
    @BeforeEach void setup() throws Exception {
        var db=new DriverManagerDataSource("jdbc:h2:mem:perf_sources_"+UUID.randomUUID()+";MODE=Oracle;DB_CLOSE_DELAY=-1","sa","");
        for(var file:List.of("11_unified_analysis.sql","14_daily_market_report.sql"))new ResourceDatabasePopulator(new ByteArrayResource(Files.readAllBytes(Path.of("..","database","oracle","ddl",file)))).execute(db);
        jdbc=new JdbcTemplate(db);sources=new PerformanceSources(db,JSON);
    }
    String save(LocalDate date,List<DailyCandle> bars,Instant collected) throws Exception {
        var id=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO ua_scan_run(run_id,request_key,market,base_date,engine_version,status,created_epoch) VALUES(?,?,'ALL',?,'v1','COMPLETED',?)",id,id,java.sql.Date.valueOf(date),collected.toEpochMilli());
        jdbc.update("INSERT INTO ua_scan_item(run_id,stock_code,stock_name,market,status) VALUES(?,'000001','test','KOSPI','COMPLETED')",id);
        jdbc.update("INSERT INTO ua_eod_run(run_id,trigger_kind,price_basis,cutoff_epoch,eligible_epoch,deadline_epoch,calendar_version,policy_version,updated_epoch) VALUES(?,'SCHEDULED','KIWOOM_KRX_DAILY_ADJUSTED',0,0,0,'v1','v1',0)",id);
        var analysis=new AnalysisInput(new StockSnapshot("000001","test","KRX",bars.get(bars.size()-1).close()),AnalysisMode.SWING,date,collected,bars,Map.of());
        jdbc.update("INSERT INTO ua_eod_item(run_id,stock_code,collected_epoch,input_json) VALUES(?,'000001',?,?)",id,collected.toEpochMilli(),JSON.writeValueAsString(Map.of("input",analysis)));
        return id;
    }
    @Test void readsFrozenBSelectionsFromEvidenceEvenWithoutReportDisplayEntries()throws Exception{
        String id=save(DAY,List.of(base()),NOW);
        jdbc.update("INSERT INTO ua_daily_report(run_id,status,policy_version,policy_json,created_epoch,updated_epoch,deadline_epoch,report_json) VALUES(?,'READY','v1','{}',0,0,0,'{}')",id);
        for(String disposition:List.of("RESEARCH_B","EXCLUDED","SIGNAL_WAIT")){
            String code=disposition.equals("RESEARCH_B")?"000001":disposition.equals("EXCLUDED")?"000002":"000003";
            jdbc.update("INSERT INTO ua_candidate_evidence(run_id,stock_code,disposition,strategy,detail_json) VALUES(?,?,?,'BREAKOUT',?)",id,code,disposition,JSON.writeValueAsString(candidate(code,disposition)));
        }
        assertThat(sources.comparisonSelections(id,null)).containsExactly(candidate("000001","RESEARCH_B"));
        assertThat(sources.comparisonSelections(id,List.of("000001"))).hasSize(1);
        assertThat(sources.comparisonSelections(id,List.of("000002"))).isEmpty();
        assertThat(sources.comparisonSelections(id,List.of())).isEmpty();
    }
    @Test void usesLaterSavedHistoryWithoutFutureObservationsAndChecksOriginalPriceBasis() throws Exception {
        var original=save(DAY,List.of(base()),NOW);var anchor=sources.anchor(original,"000001","KOSPI",DAY);
        var day=dates().get(0);var candle=bar(day,"100","105","95","102",1000);
        var later=save(dates().get(1),List.of(base(),candle,bar(dates().get(1),"102","105","95","103",1000)),collected(dates().get(1)));
        assertThat(sources.stock(anchor,day,collected(day))).isEmpty();
        var observation=sources.stock(anchor,day,collected(dates().get(1))).orElseThrow();
        assertThat(observation.sourceRunId()).isEqualTo(later);assertThat(observation.candle()).isEqualTo(candle);
        jdbc.update("UPDATE ua_eod_run SET price_basis='UNADJUSTED' WHERE run_id=?",later);
        assertThat(sources.stock(anchor,day,finalTime()).orElseThrow().quality()).isEqualTo("PRICE_BASIS_CHANGED");
        assertThat(sources.anchor(original,"000001","KOSPI",DAY)).isEqualTo(anchor);
    }
    @Test void adjustedHistoricalAnchorMismatchIsNotARealProfit() throws Exception {
        var original=save(DAY,List.of(base()),NOW);var anchor=sources.anchor(original,"000001","KOSPI",DAY);var day=dates().get(0);
        save(day,List.of(bar(DAY,"50","51","49","50",1000),bar(day,"51","53","50","52",1000)),collected(day));
        var result=sources.stock(anchor,day,finalTime()).orElseThrow();
        assertThat(result.quality()).isEqualTo("PRICE_BASIS_CHANGED");assertThat(result.candle()).isNull();
    }
    @Test void compressedAndLegacySourcesProduceIdenticalReportAndPerformanceInputs() throws Exception {
        var original=save(DAY,List.of(base()),NOW);
        var day=dates().get(0);var candle=bar(day,"100","105","95","102",1000);
        var later=save(day,List.of(base(),candle),collected(day));
        var anchor=sources.anchor(original,"000001","KOSPI",DAY);
        var observation=sources.stock(anchor,day,finalTime());
        var history=sources.monitoringHistory(anchor,dates(),finalTime());
        for(String id:List.of(original,later)){
            var raw=jdbc.queryForObject("SELECT input_json FROM ua_eod_item WHERE run_id=?",String.class,id);
            var encoded=com.example.stockanalysis.unified.eod.EodSnapshotCodec.encode(raw);
            jdbc.update("UPDATE ua_eod_item SET input_json=NULL,input_gzip=?,input_sha256=?,input_utf8_bytes=? WHERE run_id=?",encoded.gzip(),encoded.sha256(),encoded.utf8Bytes(),id);
        }
        assertThat(sources.anchor(original,"000001","KOSPI",DAY)).isEqualTo(anchor);
        assertThat(sources.stock(anchor,day,finalTime())).isEqualTo(observation);
        assertThat(sources.monitoringHistory(anchor,dates(),finalTime())).isEqualTo(history);
        var reports=new com.example.stockanalysis.unified.report.ReportStore(jdbc.getDataSource(),JSON);
        assertThat(reports.input(original,"000001").path("input").path("baseDate").asText()).isEqualTo(DAY.toString());
    }
    @Test void archivedSharedInputsPreserveAnchorsPricesAndObservations() throws Exception {
        var ds=jdbc.getDataSource();
        for(String file:List.of("08_screener.sql","18_snapshot_storage.sql"))new ResourceDatabasePopulator(new ByteArrayResource(Files.readAllBytes(Path.of("..","database","oracle","ddl",file)))).execute(ds);
        var original=save(DAY,List.of(base()),NOW);var day=dates().get(0);var candle=bar(day,"100","105","95","102",1000);
        save(day,List.of(base(),candle),collected(day));
        var anchor=sources.anchor(original,"000001","KOSPI",DAY);var observation=sources.stock(anchor,day,finalTime());var history=sources.monitoringHistory(anchor,dates(),finalTime());
        var props=new com.example.stockanalysis.storage.StorageProperties(true,true,archiveDirectory.toString(),90,100,true,false,0,80,90,14,false,5);
        var repository=new com.example.stockanalysis.storage.SnapshotRepository(ds,JSON,props);
        assertThat(new com.example.stockanalysis.storage.StorageMigration(ds,repository).batch(10)).isEqualTo(2);
        assertThat(repository.archiveBefore(Long.MAX_VALUE,100,finalTime().toEpochMilli())).isPositive();
        var archived=new PerformanceSources(ds,JSON,repository);
        assertThat(archived.anchor(original,"000001","KOSPI",DAY)).isEqualTo(anchor);
        assertThat(archived.stock(anchor,day,finalTime())).isEqualTo(observation);
        assertThat(archived.monitoringHistory(anchor,dates(),finalTime())).isEqualTo(history);
    }
    @Test void monitoringReadsManyDatesFromSavedHistoryAndPreservesFirstObservedEvidence() throws Exception {
        var original=save(DAY,List.of(base()),NOW);var anchor=sources.anchor(original,"000001","KOSPI",DAY);
        var first=bar(dates().get(0),"100","110","95","108",1000);
        var second=bar(dates().get(1),"108","121","100","120",1000);
        var saved=save(dates().get(1),List.of(base(),first,second),collected(dates().get(1)));
        assertThat(sources.monitoringHistory(anchor,dates(),NOW)).isEmpty();
        var result=sources.monitoringHistory(anchor,dates(),collected(dates().get(1)));
        assertThat(result).hasSize(2).allMatch(o->o.quality().equals("VALID")&&o.sourceRunId().equals(saved));
        assertThat(result.get(0).candle()).isEqualTo(first);
        save(dates().get(1),List.of(base(),bar(dates().get(0),"100","125","95","120",1000),second),collected(dates().get(1)).plusSeconds(1));
        assertThat(sources.monitoringHistory(anchor,dates(),collected(dates().get(1)).plusSeconds(2))).isEqualTo(result);
        jdbc.update("UPDATE ua_eod_run SET price_basis='UNADJUSTED' WHERE run_id=?",saved);
        assertThat(sources.monitoringHistory(anchor,dates(),collected(dates().get(1)).plusSeconds(2)))
                .allMatch(o->o.quality().equals("PRICE_BASIS_CHANGED")&&o.candle()==null);
    }
    @Test void monitoringShowsSelectionImmediatelyWithoutEvaluationTablesAndUsesSavedScenario() throws Exception {
        var original=save(DAY,List.of(base()),NOW);
        var report=input(original,candidate("000001","ENTRY_CONDITIONAL")).report();
        var reportJson=JSON.writeValueAsString(report);
        jdbc.update("INSERT INTO ua_daily_report(run_id,status,policy_version,policy_json,created_epoch,updated_epoch,deadline_epoch,report_json) VALUES(?,'READY','v1','{}',0,0,0,?)",original,reportJson);
        var calendar=org.mockito.Mockito.mock(com.example.stockanalysis.unified.eod.TradingCalendar.class);
        org.mockito.Mockito.when(calendar.day(org.mockito.ArgumentMatchers.any())).thenAnswer(i->{
            LocalDate date=i.getArgument(0);
            var state=date.getDayOfWeek().getValue()<6?com.example.stockanalysis.unified.eod.TradingCalendar.Status.OPEN:com.example.stockanalysis.unified.eod.TradingCalendar.Status.CLOSED;
            return new com.example.stockanalysis.unified.eod.TradingCalendar.Day(date,state,LocalTime.of(20,0),"test");
        });
        var initial=new ScenarioMonitoringService(sources,new ScenarioMonitoringEngine(),calendar,Clock.fixed(NOW,ZoneOffset.UTC)).get(original);
        assertThat(initial.stocks()).singleElement().satisfies(s->{
            assertThat(s.status()).isEqualTo("WAITING_ENTRY_DAY");assertThat(s.scenario().targets().get(0)).isEqualByComparingTo("120");
        });
        var date=dates().get(0);save(date,List.of(base(),bar(date,"100","121","95","119",1000)),collected(date));
        var updated=new ScenarioMonitoringService(sources,new ScenarioMonitoringEngine(),calendar,Clock.fixed(collected(date),ZoneOffset.UTC)).get(original);
        assertThat(updated.stocks()).singleElement().satisfies(s->{assertThat(s.status()).isEqualTo("TARGET_REACHED");assertThat(s.eventDate()).isEqualTo(date);});
        assertThat(jdbc.queryForObject("SELECT report_json FROM ua_daily_report WHERE run_id=?",String.class,original)).isEqualTo(reportJson);
    }
    @Test void monitoringDoesNotBackfillObservationsCollectedAfterItsGracePeriod() throws Exception {
        var original=save(DAY,List.of(base()),NOW);var anchor=sources.anchor(original,"000001","KOSPI",DAY);
        var late=collected(dates().get(19).plusDays(15));
        save(dates().get(0),List.of(base(),bar(dates().get(0),"100","121","95","120",1000)),late);
        assertThat(sources.monitoringHistory(anchor,dates(),late)).isEmpty();
    }
    @Test void marketIdentityUnitsAndActualObservationTimeAreRequired() throws Exception {
        var day=dates().get(0);var id=save(day,List.of(base(),bar(day,"100","105","95","102",1000)),collected(day));
        var bars=List.of(new IndexBar(DAY,bd("100"),bd("102"),bd("98"),bd("100")),new IndexBar(day,bd("100"),bd("102"),bd("99"),bd("101")));
        var market=new MarketSnapshot(List.of(new IndexSnapshot("KOSPI","001","INDEX_POINTS",day,collected(day),bars,List.of(),List.of())),collected(day));
        jdbc.update("INSERT INTO ua_daily_report(run_id,status,policy_version,policy_json,created_epoch,updated_epoch,deadline_epoch,market_json) VALUES(?,'WAITING_SCAN','v1','{}',0,0,0,?)",id,JSON.writeValueAsString(market));
        var expected=input("test").report().markets().get(0);
        assertThat(sources.index(expected,DAY,day,NOW)).isEmpty();
        assertThat(sources.index(expected,DAY,day,finalTime()).orElseThrow().quality()).isEqualTo("VALID");
        var wrong=new MarketSnapshot(List.of(new IndexSnapshot("KOSPI","101","INDEX_POINTS",day,collected(day),bars,List.of(),List.of())),collected(day));
        jdbc.update("UPDATE ua_daily_report SET market_json=?",JSON.writeValueAsString(wrong));
        assertThat(sources.index(expected,DAY,day,finalTime()).orElseThrow().quality()).isEqualTo("INVALID_SOURCE");
    }
}
