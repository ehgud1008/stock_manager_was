package com.example.stockanalysis.holdings;

import static com.example.stockanalysis.holdings.HoldingModels.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class HoldingServiceTest {
    JdbcTemplate jdbc; HoldingService service;
    ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    Clock clock=Clock.fixed(Instant.parse("2026-11-08T00:00:00Z"),ZoneOffset.UTC);
    LocalDate date=LocalDate.of(2026,11,6);
    @BeforeEach void setup() {
        jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=Oracle;DB_CLOSE_DELAY=-1","sa",""));
        new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(new org.springframework.core.io.FileSystemResource(java.nio.file.Path.of("..","database","oracle","ddl","25_personal_holdings.sql"))).execute(jdbc.getDataSource());
        service=spy(new HoldingService(jdbc,mapper,clock,"owner"));
    }
    Edit edit(int quantity,long revision) {return new Edit("삼성전자",BigDecimal.valueOf(quantity),quantity==0?null:new BigDecimal("50000"),null,date,null,null,revision);}
    @Test void twoThousandLookupsUseOneBalanceReadAndRemainFrozenAcrossEditsAndRestart() {
        service.update("005930",edit(100,0));
        Snapshot frozen=service.batch("UNIFIED:run1");
        for(int i=0;i<2000;i++)assertThat(service.batch("UNIFIED:run1")).isSameAs(frozen);
        verify(service,times(1)).list();
        service.update("005930",edit(50,1));
        assertThat(service.batch("UNIFIED:run1").items().get("005930").quantity()).isEqualByComparingTo("100");
        var restarted=spy(new HoldingService(jdbc,mapper,clock,"owner"));
        assertThat(restarted.batch("UNIFIED:run1")).isEqualTo(frozen);
        verify(restarted,never()).list();
        assertThat(service.batch("UNIFIED:run2").items().get("005930").quantity()).isEqualByComparingTo("50");
    }
    @Test void singleLookupDoesNotReadWholePortfolioAndOwnersStayIsolated() {
        service.update("005930",edit(100,0));
        assertThat(service.single("005930").items()).hasSize(1);verify(service,never()).list();
        assertThat(new HoldingService(jdbc,mapper,clock,"other").single("005930").items()).isEmpty();
    }
    @Test void preventsLostUpdateAndTreatsUnregisteredStockAsNotHeld() {
        service.update("005930",edit(100,0));
        assertThatThrownBy(()->service.update("005930",edit(20,0))).isInstanceOf(com.example.stockanalysis.shared.exception.BusinessException.class);
        service.update("005930",edit(0,1));
        assertThatThrownBy(()->service.update("005930",edit(20,1))).isInstanceOf(com.example.stockanalysis.shared.exception.BusinessException.class);
        var snapshot=service.batch("batch");
        assertThat(evaluate(snapshot,"005930",new BigDecimal("48000"),date,null,List.of(),null).positionStatus()).isEqualTo("NOT_HELD");
        assertThat(evaluate(snapshot,"000660",new BigDecimal("48000"),date,null,List.of(),null).positionStatus()).isEqualTo("NOT_HELD");
    }
    @Test void calculatesAmountsAndDefersWhenPricePredatesBalance() {
        service.update("005930",edit(100,0));var snapshot=service.single("005930");
        var result=evaluate(snapshot,"005930",new BigDecimal("48000"),date,new BigDecimal("45000"),List.of(new BigDecimal("60000")),null);
        assertThat(result.pnlAmount()).isEqualByComparingTo("-200000");
        assertThat(result.pnlPercent()).isEqualByComparingTo("-4");
        assertThat(result.additionalRiskAmount()).isEqualByComparingTo("300000");
        assertThat(result.stopPnlAmount()).isEqualByComparingTo("-500000");
        assertThat(result.targetPnlAmount()).isEqualByComparingTo("1000000");
        service.save("STOCK:1",result);
        service.update("005930",edit(0,1));
        assertThat(service.evaluation("STOCK:1")).isEqualTo(result);
        var old=evaluate(snapshot,"005930",new BigDecimal("48000"),date.plusDays(10),null,List.of(),null);
        assertThat(old.availability()).isEqualTo("PRICE_AFTER_SNAPSHOT");assertThat(old.pnlAmount()).isNull();assertThat(old.action()).isEqualTo("DEFER");
        var breached=evaluate(snapshot,"005930",new BigDecimal("44000"),date,new BigDecimal("45000"),List.of(),null);
        assertThat(breached.stopBreached()).isTrue();assertThat(breached.additionalRiskAmount()).isZero();
    }
    @Test void additionalBuyRequiresBothHoldingTrendAndEligibleEntryAndNeverOverridesSell() {
        service.update("005930",edit(100,0));var snapshot=service.single("005930");
        var entry=new com.example.stockanalysis.judgment.NewEntryAssessment("test",com.example.stockanalysis.judgment.NewEntryAssessment.Status.ELIGIBLE,List.of("유효한 진입 신호"),null,null,BigDecimal.TEN,List.of());
        var hold=new com.example.stockanalysis.judgment.TradeJudgment.Decision(com.example.stockanalysis.judgment.TradeJudgment.Action.HOLD,List.of("추세 유지"),List.of());
        var sell=new com.example.stockanalysis.judgment.TradeJudgment.Decision(com.example.stockanalysis.judgment.TradeJudgment.Action.SELL_PRIORITY,List.of("추세 훼손"),List.of());
        var judgment=new com.example.stockanalysis.judgment.TradeJudgment("test","test",clock.instant(),date,"UNKNOWN",hold,hold,List.of(),entry);
        assertThat(evaluate(snapshot,"005930",new BigDecimal("48000"),date,new BigDecimal("45000"),List.of(),judgment).action()).isEqualTo("ADD_AVERAGING_DOWN");
        assertThat(evaluate(snapshot,"005930",new BigDecimal("52000"),date,new BigDecimal("45000"),List.of(),judgment).action()).isEqualTo("ADD_PYRAMID");
        var noEntry=new com.example.stockanalysis.judgment.TradeJudgment("test","test",clock.instant(),date,"UNKNOWN",hold,hold,List.of());
        assertThat(evaluate(snapshot,"005930",new BigDecimal("48000"),date,null,List.of(),noEntry).action()).isEqualTo("HOLD");
        var selling=new com.example.stockanalysis.judgment.TradeJudgment("test","test",clock.instant(),date,"UNKNOWN",hold,sell,List.of(),entry);
        assertThat(evaluate(snapshot,"005930",new BigDecimal("48000"),date,null,List.of(),selling).action()).isEqualTo("SELL_PRIORITY");
        assertThat(evaluate(snapshot,"005930",new BigDecimal("44000"),date,new BigDecimal("45000"),List.of(),judgment).action()).isEqualTo("REVIEW_HOLDING");
    }
    @Test void databaseFailureNeverBecomesNotHeld() {
        jdbc.execute("DROP TABLE ph_holding");
        var snapshot=service.single("005930");
        assertThat(snapshot.status()).isEqualTo("UNAVAILABLE");
        assertThat(evaluate(snapshot,"005930",BigDecimal.TEN,date,null,List.of(),null).positionStatus()).isEqualTo("UNKNOWN");
    }
}
