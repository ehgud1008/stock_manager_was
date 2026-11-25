package com.example.stockanalysis.unified.eod;

import static org.mockito.Mockito.*;
import java.time.*;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class EodWorkerWeekendTest {
    static class Time extends Clock {
        Instant now=Instant.parse("2026-10-23T14:59:59Z");
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return Clock.fixed(now,zone);}
        public Instant instant(){return now;}
    }
    @Test void weekendTriggersOnlyOnePreviousClosePollAtATime() {
        var clock = new Time();
        clock.now = Instant.parse("2026-10-23T15:00:00Z");
        var coordinator = mock(EodCoordinator.class);
        var tasks = new ArrayList<Runnable>();
        var worker = new EodConfiguration.Worker(coordinator, tasks::add, clock);
        worker.start();
        worker.poll();
        worker.pollPreviousCloseOnWeekend();
        org.assertj.core.api.Assertions.assertThat(tasks).hasSize(1);
        tasks.remove(0).run();
        verify(coordinator).tick();

        clock.now = Instant.parse("2026-10-25T15:00:00Z");
        worker.pollPreviousCloseOnWeekend();
        org.assertj.core.api.Assertions.assertThat(tasks).isEmpty();
    }
    @Test void aFridayQueuedTaskCannotStartCollectingAfterKoreanMidnight(){
        var clock=new Time();var coordinator=mock(EodCoordinator.class);var tasks=new ArrayList<Runnable>();
        var worker=new EodConfiguration.Worker(coordinator,tasks::add,clock);worker.start();
        clock.now=Instant.parse("2026-10-23T15:00:00Z");tasks.remove(0).run();worker.poll();verifyNoInteractions(coordinator);
        clock.now=Instant.parse("2026-10-25T15:00:00Z");worker.poll();tasks.remove(0).run();verify(coordinator).tick();
    }
}
