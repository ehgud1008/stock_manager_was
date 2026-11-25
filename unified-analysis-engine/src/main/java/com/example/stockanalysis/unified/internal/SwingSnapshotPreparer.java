package com.example.stockanalysis.unified.internal;

import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.AnalysisMode;
import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.engine.model.StockSnapshot;
import com.example.stockanalysis.engine.screener.DefaultScreenerEngine;
import com.example.stockanalysis.engine.screener.ScreenerBar;
import com.example.stockanalysis.engine.screener.ScreenerInput;
import com.example.stockanalysis.engine.screener.ScreenerTimeframe;
import com.example.stockanalysis.engine.screener.ScreenerTypes;
import com.example.stockanalysis.unified.model.UnifiedAnalysisInput;
import com.example.stockanalysis.unified.model.UnifiedWarning;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SwingSnapshotPreparer {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    record Snapshot(AnalysisInput analysis, ScreenerInput screener, List<UnifiedWarning> warnings) {}

    Snapshot prepare(UnifiedAnalysisInput input) {
        // Legacy callers retain next-day closure; EOD callers explicitly supply the verified trade date.
        List<ScreenerBar> eligible = input.dailyBars().stream()
                .filter(bar -> input.confirmedThrough() == null
                        ? bar.openTime().toLocalDate().isBefore(input.asOf().toLocalDate())
                        : !bar.openTime().toLocalDate().isAfter(input.confirmedThrough()))
                .filter(bar -> !bar.closedAt().isAfter(input.asOf()))
                .toList();
        List<ScreenerBar> bars = DefaultScreenerEngine.normalize(eligible, input.asOf());
        if (bars.isEmpty()) throw new IllegalArgumentException("no completed daily bars at asOf");
        if (bars.stream().map(bar -> bar.openTime().toLocalDate()).distinct().count() != bars.size()) {
            throw new IllegalArgumentException("multiple daily bars for one date");
        }
        List<UnifiedWarning> warnings = new ArrayList<>();
        if (eligible.size() < input.dailyBars().size()) {
            warnings.add(new UnifiedWarning(UnifiedWarning.Source.INPUT,
                    "UNAVAILABLE_DAILY_BARS_EXCLUDED: " + (input.dailyBars().size() - eligible.size())));
        }
        if (bars.size() < eligible.size()) {
            warnings.add(new UnifiedWarning(UnifiedWarning.Source.INPUT,
                    "IDENTICAL_DAILY_BARS_DEDUPLICATED: " + (eligible.size() - bars.size())));
        }
        List<DailyCandle> daily = bars.stream().map(bar -> new DailyCandle(bar.openTime().toLocalDate(),
                bar.open(), bar.high(), bar.low(), bar.close(), bar.volume())).toList();
        DailyCandle latest = daily.get(daily.size() - 1);
        var stock = new StockSnapshot(input.stockCode(), input.stockName(), input.marketCode(), latest.close());
        var analysis = new AnalysisInput(stock, AnalysisMode.SWING, latest.businessDate(),
                input.asOf().atZone(SEOUL).toInstant(), daily, aggregate(daily, true), aggregate(daily, false),
                input.observations(), input.position(), input.confirmedThrough());
        var screener = new ScreenerInput(input.stockCode(), new ScreenerTimeframe(ScreenerTypes.Mode.SWING, 0),
                input.asOf(), bars, List.of());
        return new Snapshot(analysis, screener, List.copyOf(warnings));
    }

    private List<DailyCandle> aggregate(List<DailyCandle> daily, boolean weekly) {
        Map<LocalDate, List<DailyCandle>> groups = new LinkedHashMap<>();
        for (DailyCandle bar : daily) {
            LocalDate key = weekly
                    ? bar.businessDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    : bar.businessDate().withDayOfMonth(1);
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(bar);
        }
        List<DailyCandle> result = new ArrayList<>();
        // Without a trading calendar, completeness of the initial period cannot be established.
        for (List<DailyCandle> group : groups.values().stream().skip(1).toList()) {
            DailyCandle first = group.get(0);
            DailyCandle last = group.get(group.size() - 1);
            result.add(new DailyCandle(last.businessDate(), first.open(),
                    group.stream().map(DailyCandle::high).max(BigDecimal::compareTo).orElseThrow(),
                    group.stream().map(DailyCandle::low).min(BigDecimal::compareTo).orElseThrow(),
                    last.close(), group.stream().mapToLong(DailyCandle::volume).reduce(0L, Math::addExact)));
        }
        return List.copyOf(result);
    }
}
