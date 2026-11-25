package com.example.stockanalysis.unified.internal;

import com.example.stockanalysis.engine.model.PositionContext;
import com.example.stockanalysis.engine.screener.ScreenerBar;
import com.example.stockanalysis.unified.model.UnifiedAnalysisInput;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static com.example.stockanalysis.unified.internal.UnifiedFixtures.*;
import static org.assertj.core.api.Assertions.*;

class SwingSnapshotPreparerTest {
    @Test void explicitEodConfirmationIncludesTodayOnlyAfterItsAvailability() {
        var day=LocalDate.of(2026,10,13);
        var previous=bar(day.minusDays(1),100,1000);
        var today=new ScreenerBar(day.atStartOfDay(),day.atTime(20,0),n(120),n(130),n(110),n(120),1000,null);
        var input=new UnifiedAnalysisInput("005930","테스트","KRX",day.atTime(20,11),List.of(previous,today),Map.of(),null,day);
        var snapshot=new SwingSnapshotPreparer().prepare(input);
        assertThat(snapshot.analysis().baseDate()).isEqualTo(day);
        assertThat(snapshot.analysis().confirmedThrough()).isEqualTo(day);
        assertThat(snapshot.screener().bars()).containsExactly(previous,today);
        var early=new UnifiedAnalysisInput("005930","테스트","KRX",day.atTime(19,59),List.of(previous,today),Map.of(),null,day.minusDays(1));
        assertThat(new SwingSnapshotPreparer().prepare(early).analysis().baseDate()).isEqualTo(day.minusDays(1));
    }
    private final SwingSnapshotPreparer preparer = new SwingSnapshotPreparer();

    @Test void sortsAndDeduplicatesWithoutChangingCallerInput() {
        var bars = bars(10, false);
        var original = input(bars);
        Collections.reverse(bars);
        bars.add(bars.get(0));
        var shuffled = new UnifiedAnalysisInput(original.stockCode(), original.stockName(), original.marketCode(),
                original.asOf(), bars, Map.of());
        var prepared = preparer.prepare(shuffled);
        assertThat(prepared.analysis()).isEqualTo(preparer.prepare(original).analysis());
        assertThat(prepared.screener()).isEqualTo(preparer.prepare(original).screener());
        assertThat(shuffled.dailyBars()).containsExactlyElementsOf(bars);
        assertThat(prepared.warnings()).anyMatch(w -> w.message().equals("IDENTICAL_DAILY_BARS_DEDUPLICATED: 1"));
    }

    @Test void cutoffExcludesTodayEvenIfDeclaredClosedAndHonorsLateAvailability() {
        LocalDate day = LocalDate.of(2026, 9, 18);
        var closed = bar(day.minusDays(1), 100, 1000);
        var sameDay = new ScreenerBar(day.atStartOfDay(), day.atTime(15, 30),
                n(120), n(130), n(110), n(120), 1000, null);
        var late = new ScreenerBar(day.minusDays(2).atStartOfDay(), day.plusDays(1).atStartOfDay(),
                n(140), n(150), n(130), n(140), 1000, null);
        var future = bar(day.plusDays(1), 200, 1000);
        var input = new UnifiedAnalysisInput("005930", "테스트", "KOSPI", day.atTime(17, 0),
                List.of(sameDay, closed, late, future), Map.of());
        var snapshot = preparer.prepare(input);
        assertThat(snapshot.screener().bars()).containsExactly(closed);
        assertThat(snapshot.analysis().dailyCandles()).hasSize(1);
        assertThat(snapshot.analysis().stock().currentPrice()).isEqualByComparingTo("100");
        assertThat(snapshot.analysis().baseDate()).isEqualTo(day.minusDays(1));
        assertThat(snapshot.analysis().dataCutoffAt()).isEqualTo(input.asOf().atZone(ZoneId.of("Asia/Seoul")).toInstant());
        assertThat(snapshot.warnings()).anyMatch(w -> w.message().equals("UNAVAILABLE_DAILY_BARS_EXCLUDED: 3"));
    }

    @Test void midnightBoundaryIncludesPreviousDayAndWeekendPreservesPriceDate() {
        var friday = bar(LocalDate.of(2026, 9, 18), 100, 1000);
        var midnight = input(List.of(friday));
        assertThat(preparer.prepare(midnight).screener().bars()).containsExactly(friday);
        var sunday = new UnifiedAnalysisInput("005930", "테스트", "KOSPI",
                LocalDateTime.of(2026, 9, 20, 12, 0), List.of(friday), Map.of());
        assertThat(preparer.prepare(sunday).analysis().baseDate()).isEqualTo(LocalDate.of(2026, 9, 18));
    }

    @Test void rejectsConflictingDuplicateAndMultipleTimesOnSameDate() {
        var date = LocalDate.of(2026, 9, 18);
        var first = bar(date, 100, 1000);
        assertThatIllegalArgumentException().isThrownBy(() -> preparer.prepare(input(List.of(first, bar(date, 101, 1000)))))
                .withMessageContaining("conflicting duplicate");
        var second = new ScreenerBar(date.atTime(9, 0), first.closedAt(), first.open(), first.high(), first.low(),
                first.close(), first.volume(), null);
        assertThatIllegalArgumentException().isThrownBy(() -> preparer.prepare(input(List.of(first, second))))
                .withMessageContaining("multiple daily bars");
    }

    @Test void rejectsSnapshotWithoutAnyCompletedBar() {
        var b = bar(LocalDate.of(2026, 9, 18), 100, 1000);
        var input = new UnifiedAnalysisInput("005930", "테스트", "KOSPI", b.openTime().plusHours(12), List.of(b), Map.of());
        assertThatIllegalArgumentException().isThrownBy(() -> preparer.prepare(input)).withMessageContaining("no completed daily bars");
    }

    @Test void aggregatesCalendarWeeksAndMonthsWithPartialInitialPeriodExcluded() {
        var bars = List.of(
                bar(LocalDate.of(2025, 12, 31), 100, 1),
                bar(LocalDate.of(2026, 1, 2), 120, 2),
                bar(LocalDate.of(2026, 1, 5), 110, 3),
                bar(LocalDate.of(2026, 1, 6), 140, 4),
                bar(LocalDate.of(2026, 2, 2), 130, 5));
        var snapshot = preparer.prepare(input(bars));
        assertThat(snapshot.analysis().weeklyCandles()).hasSize(2);
        var week = snapshot.analysis().weeklyCandles().get(0);
        assertThat(week.businessDate()).isEqualTo(LocalDate.of(2026, 1, 6));
        assertThat(week.open()).isEqualByComparingTo("110");
        assertThat(week.high()).isEqualByComparingTo("150");
        assertThat(week.low()).isEqualByComparingTo("100");
        assertThat(week.close()).isEqualByComparingTo("140");
        assertThat(week.volume()).isEqualTo(7);
        assertThat(snapshot.analysis().monthlyCandles()).hasSize(2);
        var month = snapshot.analysis().monthlyCandles().get(0);
        assertThat(month.open()).isEqualByComparingTo("120");
        assertThat(month.high()).isEqualByComparingTo("150");
        assertThat(month.low()).isEqualByComparingTo("100");
        assertThat(month.close()).isEqualByComparingTo("140");
        assertThat(month.volume()).isEqualTo(9);
        assertThat(snapshot.analysis().monthlyCandles().get(1).businessDate()).isEqualTo(LocalDate.of(2026, 2, 2));
    }

    @Test void aggregationOverflowFailsInsteadOfWrappingVolume() {
        var bars = List.of(bar(LocalDate.of(2026, 1, 2), 100, 1),
                bar(LocalDate.of(2026, 1, 5), 100, Long.MAX_VALUE),
                bar(LocalDate.of(2026, 1, 6), 100, 1));
        assertThatExceptionOfType(ArithmeticException.class).isThrownBy(() -> preparer.prepare(input(bars)));
    }

    @Test void inputCopiesCollectionsAndValidatesIdentitySizeAndFuturePosition() {
        var bars = new ArrayList<>(bars(1, false));
        var observations = new HashMap<String, java.math.BigDecimal>();
        observations.put("marketReturn20", n(5));
        var base = input(bars);
        var input = new UnifiedAnalysisInput("005930", "테스트", "KOSPI", base.asOf(), bars, observations);
        bars.clear();
        observations.clear();
        assertThat(input.dailyBars()).hasSize(1);
        assertThat(input.observations()).containsEntry("marketReturn20", n(5));
        assertThatIllegalArgumentException().isThrownBy(() -> new UnifiedAnalysisInput("INVALID", "테스트", "KOSPI",
                base.asOf(), base.dailyBars(), Map.of()));
        assertThatIllegalArgumentException().isThrownBy(() -> new UnifiedAnalysisInput("005930", "테스트", "KOSPI",
                base.asOf(), List.of(), Map.of()));
        assertThatIllegalArgumentException().isThrownBy(() -> new UnifiedAnalysisInput("005930", "테스트", "KOSPI",
                base.asOf(), Collections.nCopies(20001, base.dailyBars().get(0)), Map.of()));
        var position = new PositionContext(true, base.asOf().plusSeconds(1).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                n(1000), null, List.of(), PositionContext.StopPolicy.CLOSE);
        assertThatIllegalArgumentException().isThrownBy(() -> new UnifiedAnalysisInput("005930", "테스트", "KOSPI",
                base.asOf(), base.dailyBars(), Map.of(), position)).withMessageContaining("position opens after asOf");
    }
}
