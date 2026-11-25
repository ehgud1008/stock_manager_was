package com.example.stockanalysis.unified.internal;

import com.example.stockanalysis.engine.screener.ScreenerBar;
import com.example.stockanalysis.unified.model.UnifiedAnalysisInput;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class UnifiedFixtures {
    static BigDecimal n(double value) { return BigDecimal.valueOf(value); }

    static ScreenerBar bar(LocalDate date, double price, long volume) {
        return new ScreenerBar(date.atStartOfDay(), date.plusDays(1).atStartOfDay(),
                n(price), n(price + 10), n(price - 10), n(price), volume, null);
    }

    static List<ScreenerBar> bars(int count, boolean oscillating) {
        List<ScreenerBar> bars = new ArrayList<>();
        LocalDate date = LocalDate.of(2023, 1, 2);
        while (bars.size() < count) {
            if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                int i = bars.size();
                bars.add(bar(date, oscillating ? 1000 + 100 * Math.sin(i * Math.PI / 10) : 1000 + i * 10,
                        1_000_000));
            }
            date = date.plusDays(1);
        }
        return bars;
    }

    static UnifiedAnalysisInput input(List<ScreenerBar> bars) {
        return new UnifiedAnalysisInput("005930", "테스트", "KOSPI",
                bars.get(bars.size() - 1).closedAt(), bars, Map.of());
    }
}
