package com.example.stockanalysis.engine.screener;

import java.util.Set;
import static com.example.stockanalysis.engine.screener.ScreenerTypes.Mode;

public record ScreenerTimeframe(Mode mode, int interval) {
    public ScreenerTimeframe {
        if (mode == null || (mode == Mode.SWING ? interval != 0 : !Set.of(1, 3, 5, 15).contains(interval))) {
            throw new IllegalArgumentException("SWING interval must be 0; SHORT_TERM supports 1, 3, 5, 15 minutes");
        }
    }
}
