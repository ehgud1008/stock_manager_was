package com.example.stockanalysis.unified.portfolio;

import static com.example.stockanalysis.unified.portfolio.PortfolioFixtures.*;
import static com.example.stockanalysis.unified.portfolio.PortfolioModels.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.stockanalysis.unified.performance.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class PortfolioServiceTest {
    static class MutableClock extends Clock {Instant time=PortfolioFixtures.time(DAY);public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return Clock.fixed(time,z);}public Instant instant(){return time;}}
    DriverManagerDataSource db;JdbcTemplate jdbc;PortfolioStore store;PortfolioSources reports;PerformanceSources prices;MutableClock clock;PortfolioService service;Cohort cohort;List<Observed> observed;
    @BeforeEach void setup() throws Exception {
        db=new DriverManagerDataSource("jdbc:h2:mem:portfolio_"+UUID.randomUUID()+";MODE=Oracle;DB_CLOSE_DELAY=-1","sa","");
        new ResourceDatabasePopulator(new ByteArrayResource(Files.readAllBytes(Path.of("..","database","oracle","ddl","16_portfolio_evaluation.sql")))).execute(db);
        jdbc=new JdbcTemplate(db);store=new PortfolioStore(db,JSON);reports=mock(PortfolioSources.class);prices=mock(PerformanceSources.class);clock=new MutableClock();
        cohort=cohort("a",DAY,stock("001"));observed=observations(input(5,"20",cohort));when(reports.cohort(eq("a"),any())).thenReturn(cohort);
        when(prices.stock(any(),any(),any())).thenAnswer(i->observed.stream().map(Observed::observation).filter(o->o.kind().equals("STOCK")&&o.date().equals(i.getArgument(1))&&!o.collectedAt().isAfter((Instant)i.getArgument(2))).findFirst());
        when(prices.index(any(),any(),any(),any())).thenAnswer(i->observed.stream().map(Observed::observation).filter(o->o.kind().equals("INDEX")&&!o.collectedAt().isAfter((Instant)i.getArgument(3))).findFirst());
        service=service(true,store,reports,prices);
    }
    PortfolioService service(boolean enabled,PortfolioStore s,PortfolioSources r,PerformanceSources p){return new PortfolioService(new PortfolioProperties(enabled,false,false),s,r,p,new PerformanceProperties(false,bd("30"),bd("5"),bd("0.3")),new PortfolioEngine(),calendar(),clock);}
    Request request(){return new Request(List.of("a"),bd("10000"),5,bd("20"));}
    @Test void disabledNeverTouchesDatabaseOrSources(){
        var s=mock(PortfolioStore.class);var r=mock(PortfolioSources.class);var p=mock(PerformanceSources.class);var off=service(false,s,r,p);
        off.tick();assertThat(off.get("a").state()).isEqualTo("DISABLED");assertThat(off.recent()).isEmpty();assertThat(off.available()).isEmpty();
        assertThatThrownBy(()->off.create(request(),"key")).isInstanceOf(RuntimeException.class);verifyNoInteractions(s,r,p);
    }
    @Test void idempotencySurvivesRestartAndRejectsChangedPayload(){
        var first=service.create(request(),"key");service=service(true,store,reports,prices);
        assertThat(service.create(request(),"key").id()).isEqualTo(first.id());
        assertThatThrownBy(()->service.create(new Request(List.of("a"),bd("20000"),5,bd("20")),"key")).isInstanceOf(IllegalArgumentException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_portfolio_run",Integer.class)).isEqualTo(1);
    }
    @Test void capturesForwardThenCompletesAndRetainsImmutableHistory(){
        var created=service.create(request(),"key");var frozen=store.json(created.input());service.tick();var first=service.get(created.id());
        assertThat(first.state()).isEqualTo("WAITING_DATA");assertThat(first.input().captureMode()).isEqualTo("FORWARD_CAPTURE");
        clock.time=clock.time.plusSeconds(901);service.tick();assertThat(service.get(created.id()).revision()).isEqualTo(1);
        clock.time=end();service.tick();var done=service.get(created.id());assertThat(done.state()).isEqualTo("COMPLETE");
        assertThat(done.result().variants().get(0).metrics().closed()).isEqualTo(1);assertThat(store.json(done.input())).isEqualTo(frozen);
        assertThat(service.revision(created.id(),1)).isEqualTo(first.result());assertThat(done.revision()).isEqualTo(2);
    }
    @Test void missingDataExpiresWithoutFabricatingPerformance(){
        clock.time=end().plus(Duration.ofDays(16));observed=List.of();var run=service.create(request(),"key");service.tick();
        var value=service.get(run.id());assertThat(value.input().captureMode()).isEqualTo("RETROSPECTIVE");assertThat(value.state()).isEqualTo("DATA_INCOMPLETE");
        assertThat(value.result().variants().get(0).returnPercent()).isNull();
    }
    @Test void concurrentRegistrationAndLeaseTheftCannotDuplicateOrOverwrite(){
        var pool=Executors.newFixedThreadPool(2);
        try{var a=pool.submit(()->service.create(request(),"key"));var b=pool.submit(()->service.create(request(),"key"));assertThat(a.get().id()).isEqualTo(b.get().id());}
        catch(Exception e){throw new AssertionError(e);}finally{pool.shutdownNow();}
        var first=store.claim(clock.millis()).orElseThrow();jdbc.update("UPDATE ua_portfolio_run SET lease_epoch=0");var next=store.claim(clock.millis()).orElseThrow();
        assertThatThrownBy(()->store.observe(first,observed.get(0))).isInstanceOf(PortfolioStore.LostOwnership.class);
        store.observe(next,observed.get(0));store.observe(next,observed.get(0));assertThat(store.observations(next.id())).containsExactly(observed.get(0));
        assertThatThrownBy(()->store.complete(first,new PortfolioEngine().evaluate(first.input(),List.of(),clock.instant(),false),0)).isInstanceOf(PortfolioStore.LostOwnership.class);
    }
    @Test void revisionWriteFailureRollsBackAndPreservesLastGoodResult(){
        var run=service.create(request(),"key");service.tick();var before=service.get(run.id());
        jdbc.update("ALTER TABLE ua_portfolio_revision ADD CONSTRAINT revision_limit CHECK(revision<2)");clock.time=end();service.tick();
        var after=service.get(run.id());assertThat(after.state()).isEqualTo("RETRY_WAIT");assertThat(after.result()).isEqualTo(before.result());assertThat(after.revision()).isEqualTo(1);
        clock.time=clock.time.plusSeconds(180);service.tick();clock.time=clock.time.plusSeconds(180);service.tick();assertThat(service.get(run.id()).state()).isEqualTo("FAILED");
    }
    @Test void duplicateDatesAndCalendarHolesAreRejectedBeforeRegistering(){
        when(reports.cohort(eq("b"),any())).thenReturn(cohort("b",DAY,stock("002")));
        assertThatThrownBy(()->service.create(new Request(List.of("a","b"),bd("10000"),5,bd("20")),"key")).isInstanceOf(IllegalArgumentException.class);
        when(reports.cohort(eq("a"),any())).thenReturn(cohort("a",LocalDate.of(2026,12,28),stock("001")));
        assertThatThrownBy(()->service.create(request(),"key")).isInstanceOf(IllegalArgumentException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_portfolio_run",Integer.class)).isZero();
    }
}
