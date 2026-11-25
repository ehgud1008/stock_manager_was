package com.example.stockanalysis.unified.eod;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.ZoneId;

/** Entire-market EOD execution is paused on Korean weekends, including restart recovery. */
public final class EodExecutionDays {
    private static final ZoneId SEOUL=ZoneId.of("Asia/Seoul");
    private EodExecutionDays(){}
    public static boolean isWeekday(Clock clock){
        var day=clock.instant().atZone(SEOUL).getDayOfWeek();
        return day!=DayOfWeek.SATURDAY&&day!=DayOfWeek.SUNDAY;
    }
}
