package com.example.stockanalysis.unified.portfolio;

import static com.example.stockanalysis.unified.portfolio.PortfolioFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.stockanalysis.unified.performance.*;
import com.example.stockanalysis.unified.report.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mock.env.MockEnvironment;

class OperationsServiceTest {
    DriverManagerDataSource db;JdbcTemplate jdbc;TelegramGateway gateway;ReportStore reports;PortfolioServiceTest.MutableClock clock;
    ReportProperties settings(){return new ReportProperties(true,bd(".9"),bd("1000"),bd("500"),bd("1.5"),bd("40"),bd("12"),20);}
    @BeforeEach void setup() throws Exception {
        db=new DriverManagerDataSource("jdbc:h2:mem:operations_"+UUID.randomUUID()+";MODE=Oracle;DB_CLOSE_DELAY=-1","sa","");
        for(var name:List.of("11_unified_analysis.sql","14_daily_market_report.sql","15_report_performance.sql","16_portfolio_evaluation.sql"))
            new ResourceDatabasePopulator(new ByteArrayResource(Files.readAllBytes(Path.of("..","database","oracle","ddl",name)))).execute(db);
        jdbc=new JdbcTemplate(db);gateway=mock(TelegramGateway.class);reports=mock(ReportStore.class);clock=new PortfolioServiceTest.MutableClock();
    }
    OperationsService service(DataSource source,boolean enabled,boolean tests){return new OperationsService(source,new PortfolioProperties(true,enabled,tests),settings(),new PerformanceProperties(false,bd("30"),bd("5"),bd(".3")),new TelegramProperties(true,"123:FAKE_TOKEN","-10012345678",""),gateway,reports,calendar(),clock,new MockEnvironment());}
    @Test void disabledHasNoDatabaseOrMessagingSideEffects(){
        var source=mock(DataSource.class);var off=service(source,false,false);assertThat(off.status().enabled()).isFalse();
        assertThatThrownBy(()->off.preview("a")).isInstanceOf(RuntimeException.class);verifyNoInteractions(source,gateway,reports);
    }
    @Test void diagnosticsAndPreviewNeverSendAndNeverExposeCredentials(){
        var ops=service(db,true,false);var status=ops.status();assertThat(status.tables().get("ua_portfolio_run")).isTrue();
        assertThat(status.tables().get("ua_eod_run")).isTrue();assertThat(status.warnings()).isNotEmpty();
        assertThat(status.pipelines()).allSatisfy(p->assertThat(p.state()).isEqualTo("AVAILABLE"));
        assertThat(JSON.valueToTree(status).toString()).doesNotContain("FAKE_TOKEN","-10012345678");
        when(reports.view("a")).thenReturn(new ReportModels.View("READY",null,cohort("a",DAY,stock("001")).report(),"DISABLED"));
        assertThat(ops.preview("a").text()).contains("001");verifyNoInteractions(gateway);
    }
    @Test void testSendRequiresSeparateSwitchExplicitConfirmationAndMatchingDestination(){
        var off=service(db,true,false);assertThatThrownBy(()->off.test("key",new OperationsService.TestRequest(true,off.status().destinationFingerprint()))).isInstanceOf(RuntimeException.class);
        var ops=service(db,true,true);assertThatThrownBy(()->ops.test("key",new OperationsService.TestRequest(false,ops.status().destinationFingerprint()))).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->ops.test("key",new OperationsService.TestRequest(true,"changed"))).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(gateway);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_ops_delivery_test",Integer.class)).isZero();
    }
    @Test void missingSchemaIsReportedWithoutPreventingOtherDiagnostics(){
        jdbc.update("DROP TABLE ua_eod_item");var status=service(db,true,false).status();
        assertThat(status.tables().get("ua_eod_item")).isFalse();assertThat(status.warnings()).anyMatch(w->w.contains("마이그레이션"));
        verifyNoInteractions(gateway);
    }
    @Test void explicitSendIsIdempotentAndRateLimitedAcrossServiceRestart(){
        var ops=service(db,true,true);var request=new OperationsService.TestRequest(true,ops.status().destinationFingerprint());
        when(gateway.send(anyString(),anyString())).thenReturn(new TelegramGateway.Result("SENT","42",null,0));
        assertThat(ops.test("key",request).state()).isEqualTo("SENT");
        assertThat(service(db,true,true).test("key",request).messageId()).isEqualTo("42");
        assertThatThrownBy(()->ops.test("different-key",request)).isInstanceOf(RuntimeException.class);
        verify(gateway,times(1)).send(eq("-10012345678"),contains("연결 테스트"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_report_delivery",Integer.class)).isZero();
    }
    @Test void unknownDeliveryIsNeverAutomaticallyRetried(){
        var ops=service(db,true,true);var request=new OperationsService.TestRequest(true,ops.status().destinationFingerprint());
        when(gateway.send(anyString(),anyString())).thenThrow(new IllegalStateException("test"));
        assertThat(ops.test("key",request).state()).isEqualTo("UNKNOWN");clock.time=clock.time.plusSeconds(121);
        assertThat(ops.test("key",request).state()).isEqualTo("UNKNOWN");verify(gateway,times(1)).send(anyString(),anyString());
    }
}
