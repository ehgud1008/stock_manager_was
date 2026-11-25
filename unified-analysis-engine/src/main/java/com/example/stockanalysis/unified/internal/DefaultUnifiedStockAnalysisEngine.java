package com.example.stockanalysis.unified.internal;

import com.example.stockanalysis.engine.factor.internal.SwingSuitabilityEngine;
import com.example.stockanalysis.engine.model.AnalysisResult;
import com.example.stockanalysis.engine.screener.DefaultScreenerEngine;
import com.example.stockanalysis.engine.screener.ScreenerConfig;
import com.example.stockanalysis.engine.signal.SwingSignalEngine;
import com.example.stockanalysis.unified.api.UnifiedStockAnalysisEngine;
import com.example.stockanalysis.unified.model.UnifiedAnalysisInput;
import com.example.stockanalysis.unified.model.UnifiedAnalysisResult;
import com.example.stockanalysis.unified.model.UnifiedWarning;
import java.util.ArrayList;
import java.util.Objects;

/** Composes the existing calculations without adding a score or changing a trading rule. */
public final class DefaultUnifiedStockAnalysisEngine implements UnifiedStockAnalysisEngine {
    public static final String VERSION = "unified-swing-v1";
    private final SwingSnapshotPreparer preparer = new SwingSnapshotPreparer();
    private final SwingSuitabilityEngine analysisEngine = new SwingSuitabilityEngine();
    private final SwingSignalEngine signalEngine = new SwingSignalEngine();
    private final DefaultScreenerEngine screenerEngine;
    private final java.util.function.BooleanSupplier diagnosticsEnabled;
    private final java.util.function.Consumer<Timing> diagnostics;

    public record Timing(String stockCode, long prepareNanos, long screenerNanos,
                         long suitabilityNanos, long signalNanos, int dailyBars, int weeklyBars,
                         int signalCount, String completeness, boolean scoreAvailable, String suitabilityStatus) {}

    public DefaultUnifiedStockAnalysisEngine() {
        this(ScreenerConfig.defaults());
    }

    public DefaultUnifiedStockAnalysisEngine(ScreenerConfig screenerConfig) {
        this(screenerConfig, () -> false, timing -> {});
    }

    public DefaultUnifiedStockAnalysisEngine(ScreenerConfig screenerConfig,
            java.util.function.BooleanSupplier diagnosticsEnabled, java.util.function.Consumer<Timing> diagnostics) {
        screenerEngine = new DefaultScreenerEngine(Objects.requireNonNull(screenerConfig, "screenerConfig"));
        this.diagnosticsEnabled = Objects.requireNonNull(diagnosticsEnabled, "diagnosticsEnabled");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    @Override
    public UnifiedAnalysisResult analyze(UnifiedAnalysisInput input) {
        boolean measured = diagnosticsEnabled.getAsBoolean();
        long started = measured ? System.nanoTime() : 0;
        var snapshot = preparer.prepare(Objects.requireNonNull(input, "input"));
        long prepared = measured ? System.nanoTime() : 0;
        var screener = screenerEngine.analyze(snapshot.screener());
        long screened = measured ? System.nanoTime() : 0;
        var calculated = analysisEngine.analyze(snapshot.analysis());
        long evaluated = measured ? System.nanoTime() : 0;
        var signals = signalEngine.evaluate(snapshot.analysis(), calculated.scenario());
        long signaled = measured ? System.nanoTime() : 0;
        var analysis = new AnalysisResult(calculated.engineVersion(), calculated.mode(), calculated.totalScore(),
                calculated.factors(), calculated.strategy(), calculated.scenario(), calculated.warnings(), signals);
        var warnings = new ArrayList<>(snapshot.warnings());
        analysis.warnings().forEach(message -> warnings.add(new UnifiedWarning(UnifiedWarning.Source.ANALYSIS, message)));
        signals.warnings().forEach(message -> warnings.add(new UnifiedWarning(UnifiedWarning.Source.SIGNAL, message)));
        screener.warnings().forEach(message -> warnings.add(new UnifiedWarning(UnifiedWarning.Source.SCREENER, message)));
        var result = new UnifiedAnalysisResult(VERSION, snapshot.analysis().stock(), input.asOf(),
                snapshot.analysis().baseDate(), snapshot.analysis().dailyCandles().size(),
                snapshot.analysis().weeklyCandles().size(), snapshot.analysis().monthlyCandles().size(),
                analysis, screener, warnings.stream().distinct().toList());
        if (measured) {
            try {
                diagnostics.accept(new Timing(input.stockCode(), prepared-started, screened-prepared,
                        evaluated-screened, signaled-evaluated, result.dailyBarCount(),
                        snapshot.analysis().weeklyCandles().size(), signals.signals().size(),
                        signals.completeness(), calculated.totalScore() != null,
                        result.dailyBarCount() < 60 || result.weeklyBarCount() < 20 ? "INSUFFICIENT_BARS"
                                : snapshot.analysis().dailyCandles().stream()
                                    .skip(Math.max(0, result.dailyBarCount()-20)).noneMatch(c -> c.volume() > 0)
                                    ? "NO_RECENT_VOLUME"
                                    : calculated.totalScore() == null ? "SCORE_DEFERRED" : "SCORED"));
            } catch (RuntimeException ignored) {
                // Optional diagnostics must never change the calculation result.
            }
        }
        return result;
    }
}
