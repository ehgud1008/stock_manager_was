package com.example.stockanalysis.unified;

import com.example.stockanalysis.unified.config.UnifiedScheduleConfiguration;
import com.example.stockanalysis.unified.scan.UnifiedAnalysisScheduler;
import com.example.stockanalysis.unified.eod.EodCoordinator;
import com.example.stockanalysis.screener.config.SwingScheduleConfiguration;
import com.example.stockanalysis.screener.application.SwingScreenerScheduler;
import com.example.stockanalysis.screener.application.ScreenerService;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UnifiedAnalysisSchedulerTest {
    private static final Clock WEEKDAY=Clock.fixed(Instant.parse("2026-10-13T11:00:00Z"),ZoneOffset.UTC);
    @Test void startupAndRepeatedChecksUseDurableCoordinator() {
        var coordinator=mock(EodCoordinator.class);
        var scheduler=new UnifiedAnalysisScheduler(coordinator,WEEKDAY);
        scheduler.runScheduledAnalysis(); verifyNoInteractions(coordinator);
        scheduler.onReady(); scheduler.catchUp();
        verify(coordinator,times(2)).ensureScheduled();
    }
    @Test void twentyKoreaRegardlessOfServerTimezone() throws Exception {
        var annotation=UnifiedAnalysisScheduler.class.getMethod("runScheduledAnalysis").getAnnotation(Scheduled.class);
        assertThat(annotation.zone()).isEqualTo("Asia/Seoul");
        assertThat(annotation.cron()).isEqualTo("${unified.analysis-schedule.cron:0 0 20 * * MON-FRI}");
        var now=Instant.parse("2026-10-13T10:59:00Z").atZone(ZoneId.of(annotation.zone()));
        assertThat(CronExpression.parse("0 0 20 * * MON-FRI").next(now).toInstant()).isEqualTo(Instant.parse("2026-10-13T11:00:00Z"));
        var friday=Instant.parse("2026-10-23T11:01:00Z").atZone(ZoneId.of(annotation.zone()));
        assertThat(CronExpression.parse("0 0 20 * * MON-FRI").next(friday).toInstant()).isEqualTo(Instant.parse("2026-10-26T11:00:00Z"));
        var catchUp=UnifiedAnalysisScheduler.class.getMethod("catchUp").getAnnotation(Scheduled.class);
        assertThat(catchUp.zone()).isEqualTo("Asia/Seoul");
        assertThat(CronExpression.parse(catchUp.cron()).next(friday.withHour(23).withMinute(59)).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }
    @Test void shippedConfigurationEnablesOnlyUnifiedAndCanDisableIt() throws Exception {
        var properties=new YamlPropertySourceLoader().load("application",new ClassPathResource("application.yml"));
        var production=new YamlPropertySourceLoader().load("production",new ClassPathResource("application-prod.yml"));
        var runner=new ApplicationContextRunner().withInitializer(c -> properties.forEach(p -> c.getEnvironment().getPropertySources().addLast(p)))
                .withUserConfiguration(UnifiedScheduleConfiguration.class,SwingScheduleConfiguration.class)
                .withBean(EodCoordinator.class,() -> mock(EodCoordinator.class))
                .withBean(ScreenerService.class,() -> mock(ScreenerService.class)).withBean(Clock.class,Clock::systemUTC);
        runner.run(c -> {
            assertThat(c).hasNotFailed().hasSingleBean(UnifiedAnalysisScheduler.class).doesNotHaveBean(SwingScreenerScheduler.class);
            assertThat(c.getEnvironment().getProperty("unified.analysis-schedule.cron")).isEqualTo("0 0 20 * * MON-FRI");
        });
        runner.withPropertyValues("unified.analysis-schedule.enabled=false").run(c -> assertThat(c).hasNotFailed().doesNotHaveBean(UnifiedAnalysisScheduler.class));
        runner.withInitializer(c -> production.forEach(p -> c.getEnvironment().getPropertySources().addFirst(p))).run(c -> {
            assertThat(c).hasNotFailed().hasSingleBean(UnifiedAnalysisScheduler.class).doesNotHaveBean(SwingScreenerScheduler.class);
            assertThat(c.getEnvironment().getProperty("unified.analysis-schedule.cron")).isEqualTo("0 0 20 * * MON-FRI");
        });
    }
    @Test void failureDoesNotTerminateScheduler() {
        var coordinator=mock(EodCoordinator.class); doThrow(new IllegalStateException()).when(coordinator).ensureScheduled();
        assertThatCode(new UnifiedAnalysisScheduler(coordinator,WEEKDAY)::onReady).doesNotThrowAnyException();
    }
    @Test void weekendStartupAndExplicitSchedulerCallsDoNotReachTheDatabaseCoordinator(){
        for(String instant:java.util.List.of("2026-10-23T15:00:00Z","2026-10-24T11:00:00Z","2026-10-25T14:59:59Z")){
            var coordinator=mock(EodCoordinator.class);
            var scheduler=new UnifiedAnalysisScheduler(coordinator,Clock.fixed(Instant.parse(instant),ZoneId.of("America/New_York")));
            scheduler.onReady();scheduler.catchUp();scheduler.runScheduledAnalysis();verifyNoInteractions(coordinator);
        }
    }
    @Test void mondayInKoreaRunsEvenWhileUtcIsStillSunday(){
        var coordinator=mock(EodCoordinator.class);
        new UnifiedAnalysisScheduler(coordinator,Clock.fixed(Instant.parse("2026-10-25T15:00:00Z"),ZoneOffset.UTC)).onReady();
        verify(coordinator).ensureScheduled();
    }
}
