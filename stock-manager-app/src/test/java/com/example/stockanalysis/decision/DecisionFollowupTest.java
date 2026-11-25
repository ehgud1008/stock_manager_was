package com.example.stockanalysis.decision;

import com.example.stockanalysis.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DecisionFollowupTest {
    private final DecisionJson json=new DecisionJson(new ObjectMapper());

    @Test void stableRepresentativeIsSelectedBeforeDateFiltersAndJoinsOnlyItsEngine() {
        var ds=new DriverManagerDataSource("jdbc:h2:mem:stats_"+UUID.randomUUID()+";MODE=Oracle;DB_CLOSE_DELAY=-1","sa","");
        new ResourceDatabasePopulator(ddl("12_trade_decision.sql"),ddl("26_integrated_decision.sql")).execute(ds);
        var jdbc=new JdbcTemplate(ds);
        addJob(jdbc,"snapshot","SNAPSHOT","owner",0,"unused");
        addJob(jdbc,"first","DECISION","owner",10,"first-model");
        addJob(jdbc,"revision","DECISION","owner",20,"second-model");
        addJob(jdbc,"other","DECISION","other",5,"outside-owner");
        for (String role : List.of("AI_FINAL","ENGINE")) {
            addPlan(jdbc,"first","owner",10,role);
            addPlan(jdbc,"revision","owner",20,role);
            addPlan(jdbc,"other","other",5,role);
        }
        var store=new DecisionOutcomeStore(ds,json);
        assertThat(store.samples("owner",15,30,null)).isEmpty();
        assertThat(store.samples("owner",0,30,"000660")).isEmpty();
        assertThat(store.samples("owner",0,30,"005930")).singleElement().satisfies(s -> {
            assertThat(s.model()).isEqualTo("first-model");
            assertThat(s.ai().path("marker").asText()).isEqualTo("first-AI_FINAL");
            assertThat(s.engine().path("marker").asText()).isEqualTo("first-ENGINE");
        });
    }

    @Test void maturityAndComparableCapitalSeparatePercentFromAmount() {
        var store=mock(DecisionOutcomeStore.class);
        var jobs=mock(DecisionStore.class);
        var settings=new DecisionSettings(true,"owner","","",100,30,600000,128000,12000);
        var same=new DecisionOutcomeStore.Sample("005930","p1","m1",PositionOutcomeEngine.VERSION,2,
                outcome(2, 10, 100),outcome(2, 5, 100));
        var different=new DecisionOutcomeStore.Sample("000660","p2","m2",PositionOutcomeEngine.VERSION,2,
                outcome(2, 20, 200),outcome(2, 10, 100));
        var immature=new DecisionOutcomeStore.Sample("005930","p1","m1",PositionOutcomeEngine.VERSION,2,
                outcome(1, 99, 100),outcome(2, 5, 100));
        when(store.samples(eq("owner"),anyLong(),anyLong(),isNull())).thenReturn(List.of(same,different,immature));
        var service=new DecisionPerformanceStatistics(store,jobs,settings);
        var date=LocalDate.parse("2026-11-18");
        var report=service.get(date,date,null,null,null,DecisionPerformanceStatistics.Group.POLICY);
        assertThat(report.totals().opportunities()).isEqualTo(3);
        assertThat(report.totals().comparable()).isEqualTo(2);
        assertThat(report.totals().comparablePercent()).isOne();
        assertThat(report.totals().meanDifferencePp()).isEqualByComparingTo("5");
        assertThat(report.totals().differenceAmount()).isEqualByComparingTo("15");
        assertThat(service.get(date,date,null,"p2","m2",DecisionPerformanceStatistics.Group.MODEL).totals().meanDifferencePp()).isNull();
    }

    @Test void cumulativeRoundingKeepsWholeSharesAndNeverExceedsPosition() {
        var prices=List.of(new BigDecimal("100"),new BigDecimal("150"),new BigDecimal("200"));
        var targets=TakeProfitPlanService.allocate(new BigDecimal("7"),prices,
                List.of(new BigDecimal("25"),new BigDecimal("25"),new BigDecimal("50")));
        assertThat(targets).extracting(TakeProfitPlanService.Target::quantity)
                .containsExactly(BigDecimal.ONE,new BigDecimal("2"),new BigDecimal("4"));
        assertThat(TakeProfitPlanService.allocate(BigDecimal.ONE,prices,List.of(BigDecimal.ZERO,new BigDecimal("49"),BigDecimal.ZERO)))
                .allSatisfy(t -> assertThat(t.quantity()).isZero());
        assertThatThrownBy(() -> TakeProfitPlanService.allocate(BigDecimal.TEN,prices,
                List.of(new BigDecimal("50"),new BigDecimal("50.01"),BigDecimal.ZERO))).isInstanceOf(BusinessException.class);
    }

    private com.fasterxml.jackson.databind.JsonNode outcome(int days,int pnl,int capital) {
        return json.tree(Map.of("status","CLOSED","observedTradingDays",days,"netReturnPct",pnl,"netPnlAmount",pnl,"startingCapital",capital));
    }

    private static FileSystemResource ddl(String name) { return new FileSystemResource(Path.of("..","database","oracle","ddl",name)); }
    private static void addJob(JdbcTemplate jdbc,String id,String kind,String owner,long time,String model) {
        jdbc.update("""
                INSERT INTO td_job(job_id,owner_scope,kind,stock_code,content_key,status,input_json,created_epoch,next_epoch)
                VALUES(?,?,?,'005930',?,'SUCCEEDED',?,?,?)
                """,id,owner,kind,id,"{\"versions\":{\"model\":\""+model+"\"}}",time,time);
    }
    private static void addPlan(JdbcTemplate jdbc,String job,String owner,long time,String role) {
        String id=job+"-"+role;
        jdbc.update("""
                INSERT INTO td_decision_plan(plan_id,owner_scope,job_id,snapshot_id,opportunity_key,plan_role,horizon_trading_days,policy_version,plan_json,created_epoch)
                VALUES(?,?,?,'snapshot','opportunity',?,20,'policy','{}',?)
                """,id,owner,job,role,time);
        jdbc.update("""
                INSERT INTO td_decision_outcome(owner_scope,plan_id,outcome_status,outcome_json,eval_policy_version,updated_epoch,next_epoch)
                VALUES(?,?,'CLOSED',?,'version',?,?)
                """,owner,id,"{\"marker\":\""+id+"\"}",time,time);
    }
}
