package com.example.stockanalysis.engine.model;

import java.time.Instant;
import java.util.List;

public record SignalReport(String version, Instant evaluatedAt, String positionScope, String completeness,
                           List<TradeSignal> signals, List<String> warnings) {
    public SignalReport { signals = List.copyOf(signals); warnings = List.copyOf(warnings); }
}
