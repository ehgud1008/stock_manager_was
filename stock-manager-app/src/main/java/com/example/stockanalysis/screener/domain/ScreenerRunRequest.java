package com.example.stockanalysis.screener.domain;

import com.example.stockanalysis.engine.screener.ScreenerTimeframe;
import com.example.stockanalysis.engine.screener.ScreenerTypes.Mode;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record ScreenerRunRequest(Mode mode, Integer interval, Market market, LocalDateTime asOf,
        @Size(max = 5000) List<@Pattern(regexp = "[0-9A-Z]{6}") String> stockCodes) {
    public enum Market { ALL, KOSPI, KOSDAQ }
    public ScreenerRunRequest {
        mode = mode == null ? Mode.SWING : mode;
        interval = interval == null ? (mode == Mode.SWING ? 0 : 5) : interval;
        new ScreenerTimeframe(mode, interval);
        market = market == null ? Market.ALL : market;
        if (stockCodes != null && (stockCodes.size() > 5000 || stockCodes.stream().anyMatch(c -> c == null || !c.matches("[0-9A-Z]{6}"))))
            throw new IllegalArgumentException("stockCodes must contain at most 5000 valid codes");
        stockCodes = stockCodes == null ? List.of() : stockCodes.stream().distinct().sorted().toList();
    }
    public ScreenerTimeframe timeframe() { return new ScreenerTimeframe(mode, interval); }
}
