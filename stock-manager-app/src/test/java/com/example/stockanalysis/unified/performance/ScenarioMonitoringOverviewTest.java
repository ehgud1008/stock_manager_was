package com.example.stockanalysis.unified.performance;

import static com.example.stockanalysis.unified.performance.PerformanceFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.example.stockanalysis.engine.model.TradeScenario;
import com.example.stockanalysis.unified.eod.TradingCalendar;
import com.example.stockanalysis.unified.report.ReportModels.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ScenarioMonitoringOverviewTest {
    JdbcTemplate jdbc;PerformanceSources sources;ScenarioMonitoringOverviewService service;
    final Clock clock=Clock.fixed(Instant.parse("2026-10-22T00:00:00Z"),ZoneOffset.UTC);
    @BeforeEach void setup()throws Exception{
        var ds=new DriverManagerDataSource("jdbc:h2:mem:overview_"+UUID.randomUUID()+";MODE=Oracle;DB_CLOSE_DELAY=-1","sa","");
        for(var file:List.of("11_unified_analysis.sql","14_daily_market_report.sql"))new ResourceDatabasePopulator(new ByteArrayResource(Files.readAllBytes(Path.of("..","database","oracle","ddl",file)))).execute(ds);
        jdbc=new JdbcTemplate(ds);sources=mock(PerformanceSources.class);
        var calendar=mock(TradingCalendar.class);when(calendar.day(any())).thenAnswer(i->{LocalDate date=i.getArgument(0);return new TradingCalendar.Day(date,date.getDayOfWeek().getValue()<6?TradingCalendar.Status.OPEN:TradingCalendar.Status.CLOSED,LocalTime.of(20,0),"test");});
        when(sources.anchor(anyString(),anyString(),anyString(),any())).thenAnswer(i->new PerformanceModels.Anchor(i.getArgument(1),i.getArgument(2),"KIWOOM_KRX_DAILY_ADJUSTED",base()));
        when(sources.monitoringHistory(any(),anyList(),any())).thenReturn(List.of());
        var monitor=new ScenarioMonitoringService(sources,new ScenarioMonitoringEngine(),calendar,clock);
        service=new ScenarioMonitoringOverviewService(ds,sources,monitor,clock);
    }
    String save(LocalDate date,String code,String name,String disposition,String target)throws Exception{
        String id=UUID.randomUUID().toString();long epoch=date.atTime(21,0).toInstant(ZoneOffset.UTC).toEpochMilli();
        jdbc.update("INSERT INTO ua_scan_run(run_id,request_key,market,base_date,engine_version,status,created_epoch) VALUES(?,?,'ALL',?,'v1','COMPLETED',?)",id,id,java.sql.Date.valueOf(date),epoch);
        jdbc.update("INSERT INTO ua_eod_run(run_id,trigger_kind,price_basis,cutoff_epoch,eligible_epoch,deadline_epoch,calendar_version,policy_version,updated_epoch) VALUES(?,'SCHEDULED','KIWOOM_KRX_DAILY_ADJUSTED',0,0,0,'v1','v1',0)",id);
        var original=candidate(code,disposition);var stock=new StockDecision(code,name,"KOSPI",disposition,"BREAKOUT",List.of(),List.of(),new TradeScenario(bd("95"),bd("105"),List.of(bd(target)),bd("90")),bd("2"),bd("1"),bd("5000"),bd("1"),true,1,false);
        var report=new DailyReport(id,"READY",date,date.plusDays(1),Instant.ofEpochMilli(epoch),Instant.ofEpochMilli(epoch),"v1","UNVALIDATED_RULES","ALL",List.of(),disposition.equals("EXCLUDED")?List.of():List.of(stock),1,1,List.of());
        jdbc.update("INSERT INTO ua_daily_report(run_id,status,policy_version,policy_json,created_epoch,updated_epoch,deadline_epoch,report_json) VALUES(?,'READY','v1','{}',?,?,?,?)",id,epoch,epoch,epoch+86400000,JSON.writeValueAsString(report));
        jdbc.update("INSERT INTO ua_scan_item(run_id,stock_code,stock_name,market,status) VALUES(?,?,?,'KOSPI','COMPLETED')",id,code,name);
        jdbc.update("INSERT INTO ua_candidate_evidence(run_id,stock_code,disposition,strategy,detail_json) VALUES(?,?,?,'BREAKOUT','{}')",id,code,disposition);
        when(sources.report(id)).thenReturn(report);
        if(disposition.equals("RESEARCH_B")){
            when(sources.report(id)).thenReturn(new DailyReport(id,"READY",date,date.plusDays(1),Instant.ofEpochMilli(epoch),Instant.ofEpochMilli(epoch),"v1","UNVALIDATED_RULES","ALL",List.of(),List.of(),1,1,List.of()));
            when(sources.comparisonSelections(eq(id),any())).thenReturn(List.of(stock));
        }
        return id;
    }
    @Test void bComparisonsJoinHistoryWithoutOperatingSelectionAndCanBeFilteredAndRestored()throws Exception{
        String b=save(LocalDate.of(2026,10,20),"373220","B 비교","RESEARCH_B","130");
        save(LocalDate.of(2026,10,21),"005930","A 선정","ENTRY_CONDITIONAL","120");
        save(LocalDate.of(2026,10,21),"000001","제외","EXCLUDED","120");
        var result=service.list(null,null,"","ALL",0,20);
        assertThat(result.totalElements()).isEqualTo(2);
        var filtered=service.list(null,null,"","RESEARCH_B",0,20);
        assertThat(filtered.totalElements()).isEqualTo(1);
        assertThat(filtered.rows().get(0).errorCode()).isNull();
        assertThat(filtered.rows().get(0).stock().disposition()).isEqualTo("RESEARCH_B");
        assertThat(filtered.rows().get(0).stock().scenario().targets().get(0)).isEqualByComparingTo("130");
        jdbc.execute(Files.readString(Path.of("..","database","oracle","ddl","19_monitoring_hidden.sql")));
        service.visibility(b,"373220",true);
        assertThat(service.list(null,null,"","RESEARCH_B",0,20).totalElements()).isZero();
        assertThat(service.list(null,null,"","RESEARCH_B",0,20,true).totalElements()).isEqualTo(1);
        service.visibility(b,"373220",false);
        assertThat(service.list(null,null,"","RESEARCH_B",0,20).totalElements()).isEqualTo(1);
    }
    @Test void combinesDatesKeepsRepeatedStockScenariosAndIgnoresTheNewestEmptyReport()throws Exception{
        String older=save(LocalDate.of(2026,10,19),"373220","LG에너지솔루션","WATCH","120");
        String newer=save(LocalDate.of(2026,10,20),"373220","LG에너지솔루션","ENTRY_CONDITIONAL","130");
        save(LocalDate.of(2026,10,21),"000001","excluded","EXCLUDED","120");
        var result=service.list(null,null,"","ALL",0,20);
        assertThat(result.totalElements()).isEqualTo(2);assertThat(result.rows()).extracting(ScenarioMonitoringModels.Selection::runId).containsExactly(newer,older);
        assertThat(result.rows().get(0).stock().scenario().targets().get(0)).isEqualByComparingTo("130");
        assertThat(result.rows().get(1).stock().scenario().targets().get(0)).isEqualByComparingTo("120");
        assertThat(result.rows().get(1).stock().status()).isEqualTo("WATCH_ONLY");
        assertThat(result.checkedAt()).isEqualTo(clock.instant());
        verify(sources,times(2)).monitoringHistory(any(),anyList(),any());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_daily_report",Long.class)).isEqualTo(3);
    }
    @Test void searchDispositionAndPaginationApplyAcrossAllReportsNotOnlyTheLoadedPage()throws Exception{
        save(LocalDate.of(2026,10,19),"373220","LG에너지솔루션","WATCH","120");
        save(LocalDate.of(2026,10,20),"373220","LG에너지솔루션","WATCH","125");
        save(LocalDate.of(2026,10,21),"005930","삼성전자","ENTRY_CONDITIONAL","130");
        var first=service.list(null,null,"LG","WATCH",0,1);var second=service.list(null,null,"373220","WATCH",1,1);
        assertThat(first.totalElements()).isEqualTo(2);assertThat(first.hasMore()).isTrue();assertThat(first.rows().get(0).selectionDate()).isEqualTo(LocalDate.of(2026,10,20));
        assertThat(second.totalElements()).isEqualTo(2);assertThat(second.hasMore()).isFalse();assertThat(second.rows().get(0).selectionDate()).isEqualTo(LocalDate.of(2026,10,19));
        assertThat(service.list(null,null,"%","ALL",0,20).totalElements()).isZero();
        assertThat(service.list(LocalDate.of(2026,10,20),LocalDate.of(2026,10,21),"","ALL",0,20).totalElements()).isEqualTo(2);
    }
    @Test void aBrokenSourceRemainsVisibleAndDoesNotHideOtherSelections()throws Exception{
        String old=save(LocalDate.of(2026,10,19),"373220","LG","WATCH","120");
        String bad=save(LocalDate.of(2026,10,20),"005930","삼성전자","WATCH","130");
        when(sources.report(bad)).thenThrow(new IllegalArgumentException("invalid snapshot"));
        var result=service.list(null,null,"","ALL",0,20);
        assertThat(result.totalElements()).isEqualTo(2);assertThat(result.rows().get(0).errorCode()).isEqualTo("SOURCE_UNAVAILABLE");
        assertThat(result.rows().get(1).runId()).isEqualTo(old);assertThat(result.rows().get(1).stock()).isNotNull();
    }
    @Test void overviewEndpointIsReadOnlyAndRejectsInvalidRangesAndPageSizes()throws Exception{
        var mvc=MockMvcBuilders.standaloneSetup(new ScenarioMonitoringOverviewController(service)).setMessageConverters(new MappingJackson2HttpMessageConverter(JSON)).build();
        mvc.perform(get("/api/v1/unified-analysis/monitoring")).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(get("/api/v1/unified-analysis/monitoring").param("from","2026-10-22").param("to","2026-10-19")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/unified-analysis/monitoring").param("size","1000")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/unified-analysis/monitoring").param("disposition","EXCLUDED")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/unified-analysis/monitoring")).andExpect(status().isMethodNotAllowed());
    }
    @Test void hiddenSelectionsAreDurableRestorableAndDoNotDeleteSourceReports()throws Exception{
        String older=save(LocalDate.of(2026,10,19),"373220","LG","WATCH","120");
        String newer=save(LocalDate.of(2026,10,20),"373220","LG","WATCH","130");
        assertThat(service.list(null,null,"","ALL",0,20).deletionEnabled()).isFalse();
        jdbc.execute(Files.readString(Path.of("..","database","oracle","ddl","19_monitoring_hidden.sql")));
        var mvc=MockMvcBuilders.standaloneSetup(new ScenarioMonitoringOverviewController(service)).setMessageConverters(new MappingJackson2HttpMessageConverter(JSON)).build();
        mvc.perform(delete("/api/v1/unified-analysis/monitoring/"+older+"/stocks/373220")).andExpect(status().isOk());
        service.visibility(older,"373220",true);
        var visible=service.list(null,null,"","ALL",0,20);
        assertThat(visible.deletionEnabled()).isTrue();assertThat(visible.totalElements()).isEqualTo(1);
        assertThat(visible.rows().get(0).runId()).isEqualTo(newer);
        assertThat(service.list(null,null,"LG","ALL",0,1,true).rows().get(0).runId()).isEqualTo(older);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_candidate_evidence",Long.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_daily_report",Long.class)).isEqualTo(2);
        mvc.perform(post("/api/v1/unified-analysis/monitoring/"+older+"/stocks/373220/restore")).andExpect(status().isOk());
        assertThat(service.list(null,null,"","ALL",0,20).totalElements()).isEqualTo(2);
        assertThat(service.list(null,null,"","ALL",0,20,true).totalElements()).isZero();
        mvc.perform(delete("/api/v1/unified-analysis/monitoring/missing/stocks/373220")).andExpect(status().isBadRequest());
    }
}
