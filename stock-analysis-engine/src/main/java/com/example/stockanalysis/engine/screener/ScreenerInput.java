package com.example.stockanalysis.engine.screener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record ScreenerInput(String stockCode, ScreenerTimeframe timeframe, LocalDateTime asOf,
                            List<ScreenerBar> bars, List<ScreenerBar> dailyBars) {
    public ScreenerInput {
        if (stockCode == null || !stockCode.matches("[0-9A-Z]{6}")) throw new IllegalArgumentException("invalid stockCode");
        Objects.requireNonNull(timeframe, "timeframe");
        Objects.requireNonNull(asOf, "asOf");
        bars = List.copyOf(Objects.requireNonNull(bars, "bars"));
        dailyBars = dailyBars == null ? List.of() : List.copyOf(dailyBars);
        if (bars.size() > 20000 || dailyBars.size() > 20000) throw new IllegalArgumentException("too many bars");
    }
}
