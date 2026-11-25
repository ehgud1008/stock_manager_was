package com.example.stockanalysis.decision;

import java.time.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.FileSystemResource;
import com.example.stockanalysis.shared.exception.BusinessException;
import static org.assertj.core.api.Assertions.*;

class AccountRiskServiceTest {
    JdbcTemplate jdbc;AccountRiskService service;Clock clock=Clock.fixed(Instant.parse("2026-11-17T01:00:00Z"),ZoneOffset.UTC);
    DecisionSettings settings(String owner){return new DecisionSettings(true,owner,"model","",100,30,300000,128000,12000);}
    @BeforeEach void setup(){
        var ds=new DriverManagerDataSource("jdbc:h2:mem:risk_"+UUID.randomUUID()+";MODE=Oracle;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000","sa","");
        jdbc=new JdbcTemplate(ds);new ResourceDatabasePopulator(new FileSystemResource(Path.of("..","database","oracle","ddl","27_account_risk.sql"))).execute(ds);
        service=new AccountRiskService(jdbc,settings("owner"),clock);
    }
    AccountRiskModels.Edit edit(long revision){return new AccountRiskModels.Edit(new BigDecimal("100000"),new BigDecimal("10000"),new BigDecimal("500"),new BigDecimal("5"),LocalDate.of(2026,11,17),revision);}
    @Test void getIsReadOnlyAndOwnersAreIsolated(){
        assertThat(service.get().status()).isEqualTo("NOT_CONFIGURED");assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM td_account_risk",Integer.class)).isZero();
        var saved=service.save(edit(0));assertThat(saved.revision()).isEqualTo(1);assertThat(service.get().settings().availableCash()).isEqualByComparingTo("10000");
        assertThat(new AccountRiskService(jdbc,settings("other"),clock).get().status()).isEqualTo("NOT_CONFIGURED");
    }
    @Test void optimisticRevisionRejectsLostUpdatesIncludingSimultaneousWriters() throws Exception {
        service.save(edit(0));assertThatThrownBy(()->service.save(edit(0))).isInstanceOf(BusinessException.class);
        var pool=Executors.newFixedThreadPool(2);
        try {
            List<Callable<Boolean>> calls=List.of(()->saveAttempt(),()->saveAttempt());int winners=0;
            for(var f:pool.invokeAll(calls))if(f.get())winners++;
            assertThat(winners).isEqualTo(1);assertThat(service.get().settings().revision()).isEqualTo(2);
        }finally{pool.shutdownNow();}
    }
    boolean saveAttempt(){try{service.save(edit(1));return true;}catch(BusinessException e){return false;}}
    @Test void inconsistentAmountsFutureDatesAndExcessPrecisionAreRejectedWithoutWriting(){
        var valid=edit(0);
        var bad=List.of(
            new AccountRiskModels.Edit(valid.totalAssets(),new BigDecimal("100001"),valid.maxLossPerTrade(),valid.maxPositionPercent(),valid.balanceDate(),0),
            new AccountRiskModels.Edit(valid.totalAssets(),valid.availableCash(),valid.maxLossPerTrade(),new BigDecimal("100.01"),valid.balanceDate(),0),
            new AccountRiskModels.Edit(valid.totalAssets(),valid.availableCash(),new BigDecimal("0.001"),valid.maxPositionPercent(),valid.balanceDate(),0),
            new AccountRiskModels.Edit(valid.totalAssets(),valid.availableCash(),valid.maxLossPerTrade(),valid.maxPositionPercent(),valid.balanceDate().plusDays(1),0));
        for(var request:bad)assertThatThrownBy(()->service.save(request)).isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM td_account_risk",Integer.class)).isZero();
    }
    @Test void missingMigrationIsAnExplicitUnavailableState(){
        jdbc.execute("DROP TABLE td_account_risk");assertThat(service.get().status()).isEqualTo("UNAVAILABLE");
        assertThat(service.get().message()).contains("20261117_01_account_risk.sql");
        assertThatThrownBy(()->service.save(edit(0))).isInstanceOf(BusinessException.class);
    }
}
