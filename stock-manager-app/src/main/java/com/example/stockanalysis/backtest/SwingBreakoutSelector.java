package com.example.stockanalysis.backtest;

import static com.example.stockanalysis.backtest.SwingBacktestModels.Signal;
import com.example.stockanalysis.engine.model.DailyCandle;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

final class SwingBreakoutSelector {
    static final int WARMUP = 21;

    private SwingBreakoutSelector() {}

    static Signal select(String stockCode, List<LocalDate> dates, List<DailyCandle> window) {
        var current = window.get(WARMUP);
        var previous = window.get(WARMUP - 1);
        if (current.volume() == 0 || previous.volume() == 0) return null;
        var level = high(window.subList(1, WARMUP));
        var previousLevel = high(window.subList(0, WARMUP - 1));
        if (current.close().compareTo(level) <= 0 || previous.close().compareTo(previousLevel) > 0) return null;
        return new Signal(dates.get(WARMUP), stockCode, current.close(), level);
    }

    private static BigDecimal high(List<DailyCandle> candles) {
        return candles.stream().map(DailyCandle::high).max(Comparator.naturalOrder()).orElseThrow();
    }
}
