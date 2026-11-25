package com.example.stockanalysis.unified.portfolio;

import static com.example.stockanalysis.unified.portfolio.PortfolioFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.stockanalysis.unified.performance.PerformanceSources;
import com.example.stockanalysis.unified.report.*;
import com.example.stockanalysis.unified.report.ReportModels.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class PortfolioSourcesTest {
    @Test void v7ReconstructsQualifiedBWithSamePatternRankingAndRequiresCandleEvidence() {
        var stocks=new ArrayList<StockDecision>();
        for(var pattern:List.of("BREAKOUT","PULLBACK_RECOVERY")) {
            var original=stock(pattern.equals("BREAKOUT")?"001":"002");
            var evidence=new ArrayList<>(original.evidence());
            for(var key:List.of("BULLISH_STRUCTURE","SWING_ACTION","POSITIVE_RETURN20","OPEN_PRICE","COMMON_ENTRY","OPERATING_TIER","ENTRY_TIMING","PATTERN_VOLUME","BREAKOUT_CLOSE"))
                evidence.add(new Evidence(key,key,bd("1"),"",">=",bd("1"),true,"POSITIVE","GATE","OBSERVED","test",DAY,time(DAY),CandidateAnalyzer.VERSION));
            boolean b=pattern.equals("PULLBACK_RECOVERY");
            evidence.add(new Evidence("SWING_SCORE","80점 기준",bd(b?"75":"85"),"점",">=",bd("80"),!b,"POSITIVE","SUPPORT","OBSERVED","test",DAY,time(DAY),CandidateAnalyzer.VERSION));
            var signal=new com.example.stockanalysis.engine.model.TradeSignal(pattern,pattern,com.example.stockanalysis.engine.model.TradeSignal.Side.BUY,com.example.stockanalysis.engine.model.TradeSignal.State.CONFIRMED,DAY,DAY,0,5,bd("100"),bd("90"),original.scenario().entryFrom(),original.scenario().entryTo(),"IN_RANGE",null,List.of());
            var assessment=new com.example.stockanalysis.judgment.NewEntryAssessment("new-entry-v3-tier",com.example.stockanalysis.judgment.NewEntryAssessment.Status.ELIGIBLE,List.of(),signal,original.scenario(),bd("2"),List.of(),new com.example.stockanalysis.judgment.SuitabilityTier("suitability-tier-v1",b?"B":"A",bd(b?"75":"85"),List.of()),new com.example.stockanalysis.judgment.NewEntryAssessment.Timing(com.example.stockanalysis.judgment.NewEntryAssessment.Status.ELIGIBLE,List.of(),false));
            stocks.add(new StockDecision(original.code(),original.name(),original.market(),original.disposition(),pattern,evidence,List.of(),original.scenario(),original.netRewardRisk(),original.relativeReturn20(),original.liquidity(),original.dailyReturn(),true,1,false,assessment));
        }
        var old=cohort("r",DAY,stocks.toArray(StockDecision[]::new)).report();
        var report=new DailyReport(old.runId(),old.status(),old.tradeDate(),old.forecastDate(),old.informationAsOf(),old.finalizedAt(),"candidate-v7-profitability/test",old.validationStatus(),old.scope(),old.markets(),stocks,2,2,List.of());
        assertThat(PortfolioSources.select(stocks,report,true,20)).extracting(StockDecision::code).containsExactly("002","001");
        assertThat(ResearchSelections.rank(stocks,report,20)).extracting(StockDecision::code).containsExactly("002","001");
        assertThat(stocks.stream().sorted(CandidateRanking.comparator()).toList()).extracting(StockDecision::code).containsExactly("002","001");
        var b=stocks.get(1);
        var missing=new StockDecision(b.code(),b.name(),b.market(),b.disposition(),b.strategy(),b.evidence().stream().filter(e->!e.code().equals("BREAKOUT_CLOSE")).toList(),b.cautions(),b.scenario(),b.netRewardRisk(),b.relativeReturn20(),b.liquidity(),b.dailyReturn(),true,1,false,b.newEntry());
        assertThatThrownBy(()->PortfolioSources.select(List.of(missing),report,true,20)).hasMessage("ENTRY_EVIDENCE_INCOMPLETE");
    }
    JdbcTemplate jdbc;PortfolioSources source;PerformanceSources prices;
    @BeforeEach void setup() throws Exception {
        var db=new DriverManagerDataSource("jdbc:h2:mem:portfolio_source_"+UUID.randomUUID()+";MODE=Oracle;DB_CLOSE_DELAY=-1","sa","");
        for(var name:List.of("11_unified_analysis.sql","14_daily_market_report.sql"))new ResourceDatabasePopulator(new ByteArrayResource(Files.readAllBytes(Path.of("..","database","oracle","ddl",name)))).execute(db);
        jdbc=new JdbcTemplate(db);prices=mock(PerformanceSources.class);source=new PortfolioSources(db,JSON,prices);
        when(prices.anchor(anyString(),anyString(),anyString(),any())).thenAnswer(i->cohort("x",DAY,stock(i.getArgument(1))).anchors().get(0));
    }
    DailyReport saved(boolean falling) throws Exception {
        var c=cohort("a",DAY,stock("002","WATCH","TRANSITION_6_TO_1"),stock("003",falling?"WATCH":"ENTRY_CONDITIONAL","BREAKOUT"));
        var m=c.report().markets().get(0);if(falling)m=new MarketView(m.market(),m.dayReturn(),m.return20(),m.aboveEma20Percent(),m.advancingPercent(),m.validStocks(),m.eligibleStocks(),m.stages(),m.transitions61(),"하락 우세",m.baseScenario(),m.upsideScenario(),m.downsideScenario(),m.referenceClose(),m.support(),m.resistance(),m.cautions());
        var r=c.report();var all=List.of(stock("001","EXCLUDED","NONE"),stock("002","WATCH","TRANSITION_6_TO_1"),stock("003","RANK_LIMIT","BREAKOUT"));
        r=new DailyReport(r.runId(),r.status(),r.tradeDate(),r.forecastDate(),r.informationAsOf(),r.finalizedAt(),r.version(),r.validationStatus(),r.scope(),List.of(m),List.of(falling?all.get(1):stock("003")),3,3,List.of());
        var settings=new ReportProperties(true,bd(".9"),bd("1000"),bd("500"),bd("1.5"),bd("40"),bd("12"),1);
        jdbc.update("INSERT INTO ua_scan_run(run_id,request_key,market,base_date,engine_version,status,created_epoch) VALUES('a','a','ALL',?,'v1','COMPLETED',?)",java.sql.Date.valueOf(DAY),time(DAY).toEpochMilli());
        jdbc.update("INSERT INTO ua_daily_report(run_id,status,policy_version,policy_json,created_epoch,updated_epoch,deadline_epoch,report_json) VALUES('a','READY','test',?,1,1,1,?)",JSON.writeValueAsString(settings),JSON.writeValueAsString(r));
        for(var s:all)jdbc.update("INSERT INTO ua_candidate_evidence VALUES('a',?,?,?,?)",s.code(),s.disposition(),s.strategy(),JSON.writeValueAsString(s));
        return r;
    }
    @Test void marketFilterReconstructionRestoresRankLimitedEntryWithoutChangingOriginal() throws Exception {
        saved(true);var before=jdbc.queryForObject("SELECT report_json FROM ua_daily_report",String.class);var c=source.cohort("a",end());
        assertThat(c.baseline()).extracting(StockDecision::code).containsExactly("002");
        assertThat(c.withoutMarketFilter()).extracting(StockDecision::code).containsExactly("003");
        assertThat(c.withoutMarketFilter().get(0).disposition()).isEqualTo("ENTRY_CONDITIONAL");
        assertThat(jdbc.queryForObject("SELECT report_json FROM ua_daily_report",String.class)).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_report_delivery",Integer.class)).isZero();assertThat(source.available()).hasSize(1);
    }
    @Test void incompleteEvidenceAndUnreproducibleRankingAreRejected() throws Exception {
        saved(false);jdbc.update("DELETE FROM ua_candidate_evidence WHERE stock_code='001'");
        assertThatThrownBy(()->source.cohort("a",end())).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("불완전");
    }
    @Test void cannotUseReportNotKnownBeforeEntry() throws Exception {
        saved(false);assertThatThrownBy(()->source.cohort("a",time(DAY).minusSeconds(1))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void v2NeutralMarketAndNewEvidenceAreReconstructedWithoutChangingV1(){
        var original=stock("001");var evidence=new ArrayList<>(original.evidence());
        for(var key:List.of("BULLISH_STRUCTURE","SWING_SCORE","SWING_ACTION","POSITIVE_RETURN20","OPEN_PRICE"))
            evidence.add(new Evidence(key,key,bd("1"),"",">=",bd("1"),true,"POSITIVE","GATE","OBSERVED","test",DAY,time(DAY),"candidate-market-v2"));
        var s=new StockDecision(original.code(),original.name(),original.market(),original.disposition(),original.strategy(),evidence,original.cautions(),original.scenario(),original.netRewardRisk(),original.relativeReturn20(),original.liquidity(),original.dailyReturn(),original.aboveEma20(),original.stage(),original.transition61());
        var r=cohort("a",DAY,s).report();var m=r.markets().get(0);
        var neutral=new MarketView(m.market(),m.dayReturn(),m.return20(),m.aboveEma20Percent(),m.advancingPercent(),m.validStocks(),m.eligibleStocks(),m.stages(),m.transitions61(),"중립",m.baseScenario(),m.upsideScenario(),m.downsideScenario(),m.referenceClose(),m.support(),m.resistance(),m.cautions());
        for(var version:List.of("candidate-market-v1/test","candidate-market-v2/test","candidate-market-v3-open-price/test")) {
            var report=new DailyReport(r.runId(),r.status(),r.tradeDate(),r.forecastDate(),r.informationAsOf(),r.finalizedAt(),version,r.validationStatus(),r.scope(),List.of(neutral),List.of(s),1,1,List.of());
            assertThat(PortfolioSources.select(List.of(s),report,true,20).get(0).disposition()).isEqualTo(!version.contains("v1")?"WATCH":"ENTRY_CONDITIONAL");
            assertThat(PortfolioSources.select(List.of(s),report,false,20).get(0).disposition()).isEqualTo("ENTRY_CONDITIONAL");
            if(!version.contains("v1"))assertThatThrownBy(()->PortfolioSources.select(List.of(original),report,false,20)).hasMessage("ENTRY_EVIDENCE_INCOMPLETE");
        }
    }
    @Test void insufficientEntryEvidenceNeverBecomesTradeCandidate(){
        var s=stock("001");var incomplete=new StockDecision(s.code(),s.name(),s.market(),s.disposition(),s.strategy(),List.of(),s.cautions(),s.scenario(),s.netRewardRisk(),s.relativeReturn20(),s.liquidity(),s.dailyReturn(),s.aboveEma20(),s.stage(),s.transition61());
        assertThatThrownBy(()->PortfolioSources.select(List.of(incomplete),cohort("a",DAY,s).report(),false,20)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void v4RoundTripKeepsCommonAssessmentAndRejectsMissingOrMismatchedEvidence() throws Exception {
        var r=saved(false);var original=stock("003");var evidence=new ArrayList<>(original.evidence());
        for(var key:List.of("BULLISH_STRUCTURE","SWING_SCORE","SWING_ACTION","POSITIVE_RETURN20","OPEN_PRICE","COMMON_ENTRY"))
            evidence.add(new Evidence(key,key,bd("1"),"",">=",bd("1"),true,"POSITIVE","GATE","OBSERVED","test",DAY,time(DAY),CandidateAnalyzer.VERSION));
        var signal=new com.example.stockanalysis.engine.model.TradeSignal("selected","BREAKOUT",com.example.stockanalysis.engine.model.TradeSignal.Side.BUY,
                com.example.stockanalysis.engine.model.TradeSignal.State.CONFIRMED,DAY,DAY,0,5,bd("100"),bd("90"),
                original.scenario().entryFrom(),original.scenario().entryTo(),"IN_RANGE",null,List.of());
        var common=new com.example.stockanalysis.judgment.NewEntryAssessment("new-entry-v1",com.example.stockanalysis.judgment.NewEntryAssessment.Status.ELIGIBLE,
                List.of("통과"),signal,original.scenario(),bd("2"),List.of());
        var s=new StockDecision(original.code(),original.name(),original.market(),original.disposition(),original.strategy(),evidence,original.cautions(),
                original.scenario(),original.netRewardRisk(),original.relativeReturn20(),original.liquidity(),original.dailyReturn(),original.aboveEma20(),original.stage(),original.transition61(),common);
        var report=new DailyReport(r.runId(),r.status(),r.tradeDate(),r.forecastDate(),r.informationAsOf(),r.finalizedAt(),"candidate-market-v4-common-entry/test",
                r.validationStatus(),r.scope(),r.markets(),List.of(s),1,1,List.of());
        jdbc.update("UPDATE ua_daily_report SET report_json=? WHERE run_id='a'",JSON.writeValueAsString(report));
        jdbc.update("DELETE FROM ua_candidate_evidence WHERE run_id='a'");
        jdbc.update("INSERT INTO ua_candidate_evidence VALUES('a',?,?,?,?)",s.code(),s.disposition(),s.strategy(),JSON.writeValueAsString(s));
        var reconstructed=source.cohort("a",end());
        assertThat(reconstructed.baseline().get(0).newEntry()).isEqualTo(common);
        assertThat(reconstructed.comparisons()).containsKey("NO_RELATIVE_STRENGTH");
        assertThat(reconstructed.unavailableComparisons()).containsKeys("SIGNAL_AGE4","PATTERN_VOLUME");
        assertThat(source.audit("a").commonEligible()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT report_json FROM ua_daily_report WHERE run_id='a'",String.class)).isEqualTo(JSON.writeValueAsString(report));
        var replay=mock(SelectionResearchReplay.class);
        jdbc.update("UPDATE ua_daily_report SET market_json=? WHERE run_id='a'",JSON.writeValueAsString(new MarketSnapshot(List.of(),time(DAY))));
        when(replay.replay(any(),any(),any(),any(),eq(CandidateAnalyzer.ResearchMode.SIGNAL_AGE4))).thenThrow(new IllegalArgumentException("RESEARCH_INPUT_UNAVAILABLE"));
        when(replay.replay(any(),any(),any(),any(),eq(CandidateAnalyzer.ResearchMode.PATTERN_VOLUME))).thenReturn(List.of(s));
        var partial=new PortfolioSources(jdbc.getDataSource(),JSON,prices,replay).cohort("a",end());
        assertThat(partial.comparisons()).containsKey("PATTERN_VOLUME").doesNotContainKey("SIGNAL_AGE4");
        assertThat(partial.unavailableComparisons()).containsKey("SIGNAL_AGE4");
        assertThat(partial.baseline()).hasSize(1);
        var missing=new StockDecision(s.code(),s.name(),s.market(),s.disposition(),s.strategy(),s.evidence(),s.cautions(),s.scenario(),s.netRewardRisk(),s.relativeReturn20(),s.liquidity(),s.dailyReturn(),s.aboveEma20(),s.stage(),s.transition61());
        assertThatThrownBy(()->PortfolioSources.select(List.of(missing),report,false,20)).hasMessage("COMMON_ENTRY_EVIDENCE_INCOMPLETE");
        var wrongStrategy=new StockDecision(s.code(),s.name(),s.market(),s.disposition(),"BREAKOUT_RETEST",s.evidence(),s.cautions(),s.scenario(),s.netRewardRisk(),s.relativeReturn20(),s.liquidity(),s.dailyReturn(),s.aboveEma20(),s.stage(),s.transition61(),common);
        assertThatThrownBy(()->PortfolioSources.select(List.of(wrongStrategy),report,false,20)).hasMessage("COMMON_ENTRY_EVIDENCE_INCOMPLETE");
    }
}
