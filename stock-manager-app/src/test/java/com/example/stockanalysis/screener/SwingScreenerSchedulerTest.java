package com.example.stockanalysis.screener;

import com.example.stockanalysis.screener.application.*;
import com.example.stockanalysis.screener.config.*;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SwingScreenerSchedulerTest {
    private Clock at(String local) { return Clock.fixed(LocalDateTime.parse(local).atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneOffset.UTC); }
    @Test void weekdayStartupWithinWindowCatchesUpUsingPlannedCutoff() {
        var service = mock(ScreenerService.class);
        var scheduler = new SwingScreenerScheduler(service, new SwingScheduleProperties(null,null,null), at("2026-09-15T06:30:00"));
        scheduler.checkSchedule(); verifyNoInteractions(service);
        scheduler.onReady(); scheduler.checkSchedule();
        verify(service, times(2)).ensureScheduledSwing(LocalDateTime.parse("2026-09-15T04:00:00"));
    }
    @Test void weekendsExclusionsAndOutsideWindowDoNotCollect() {
        for (String time : List.of("2026-09-15T03:59:59", "2026-09-15T08:00:00", "2026-09-15T12:00:00", "2026-09-19T04:00:00", "2026-09-20T04:00:00")) {
            var service = mock(ScreenerService.class);
            new SwingScreenerScheduler(service,new SwingScheduleProperties(null,null,null),at(time)).onReady();
            verifyNoInteractions(service);
        }
        var service = mock(ScreenerService.class);
        new SwingScreenerScheduler(service,new SwingScheduleProperties(null,null,List.of(LocalDate.of(2026,9,15))),at("2026-09-15T04:00:00")).onReady();
        verifyNoInteractions(service);
    }
    @Test void failureDoesNotTerminateLaterScheduleChecks() {
        var service = mock(ScreenerService.class);
        when(service.ensureScheduledSwing(any())).thenThrow(new IllegalStateException("test"));
        var scheduler = new SwingScreenerScheduler(service,new SwingScheduleProperties(null,null,null),at("2026-09-15T04:00:00"));
        assertThatCode(scheduler::onReady).doesNotThrowAnyException();
        assertThatCode(scheduler::checkSchedule).doesNotThrowAnyException();
        verify(service,times(2)).ensureScheduledSwing(any());
    }
    @Test void configurationCanDisableSchedulingAndBindsEmptyExclusions() {
        var runner = new ApplicationContextRunner().withUserConfiguration(SwingScheduleConfiguration.class)
                .withBean(ScreenerService.class, () -> mock(ScreenerService.class)).withBean(Clock.class, () -> at("2026-09-15T04:00:00"));
        runner.withPropertyValues("screener.swing-schedule.enabled=false").run(c -> assertThat(c).doesNotHaveBean(SwingScreenerScheduler.class));
        runner.withPropertyValues("screener.swing-schedule.enabled=true", "screener.swing-schedule.excluded-dates=").run(c -> {
            assertThat(c).hasNotFailed().hasSingleBean(SwingScreenerScheduler.class);
            assertThat(c.getBean(SwingScheduleProperties.class).excludedDates()).isEmpty();
        });
        assertThatThrownBy(() -> new SwingScheduleProperties(LocalTime.of(8,0),LocalTime.of(4,0),null)).isInstanceOf(IllegalArgumentException.class);
    }
}
