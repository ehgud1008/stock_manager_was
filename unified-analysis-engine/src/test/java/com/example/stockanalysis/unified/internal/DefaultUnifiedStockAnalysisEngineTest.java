package com.example.stockanalysis.unified.internal;

import com.example.stockanalysis.engine.factor.internal.SwingSuitabilityEngine;
import com.example.stockanalysis.engine.model.AnalysisResult;
import com.example.stockanalysis.engine.model.PositionContext;
import com.example.stockanalysis.engine.model.TradeSignal;
import com.example.stockanalysis.engine.screener.DefaultScreenerEngine;
import com.example.stockanalysis.engine.screener.ScreenerBar;
import com.example.stockanalysis.engine.screener.ScreenerConfig;
import com.example.stockanalysis.engine.screener.ScreenerTypes;
import com.example.stockanalysis.engine.signal.SwingSignalEngine;
import com.example.stockanalysis.unified.model.UnifiedAnalysisInput;
import com.example.stockanalysis.unified.model.UnifiedWarning;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static com.example.stockanalysis.unified.internal.UnifiedFixtures.*;
import static org.assertj.core.api.Assertions.*;

class DefaultUnifiedStockAnalysisEngineTest {
    private final DefaultUnifiedStockAnalysisEngine engine = new DefaultUnifiedStockAnalysisEngine();

    @Test
    void diagnosticsPreserveResultsAndDoNotRunWhenDisabled() {
        var sample = input(bars(300, false));
        var timings = new java.util.ArrayList<DefaultUnifiedStockAnalysisEngine.Timing>();
        var measured = new DefaultUnifiedStockAnalysisEngine(ScreenerConfig.defaults(), () -> true, timings::add);
        assertThat(measured.analyze(sample)).isEqualTo(engine.analyze(sample));
        assertThat(timings).hasSize(1);
        var timing = timings.get(0);
        assertThat(timing.stockCode()).isEqualTo(sample.stockCode());
        assertThat(timing.prepareNanos()).isGreaterThanOrEqualTo(0);
        assertThat(timing.screenerNanos()).isGreaterThanOrEqualTo(0);
        assertThat(timing.suitabilityNanos()).isGreaterThanOrEqualTo(0);
        assertThat(timing.signalNanos()).isGreaterThanOrEqualTo(0);
        var disabled = new DefaultUnifiedStockAnalysisEngine(ScreenerConfig.defaults(), () -> false,
                t -> { throw new AssertionError("Disabled diagnostic callback invoked"); });
        assertThat(disabled.analyze(sample)).isEqualTo(engine.analyze(sample));
    }

    @Test
    void diagnosticSinkFailureDoesNotFailAnalysis() {
        var sample = input(bars(300, false));
        var measured = new DefaultUnifiedStockAnalysisEngine(ScreenerConfig.defaults(), () -> true,
                t -> { throw new IllegalStateException("Diagnostic sink unavailable"); });
        assertThat(measured.analyze(sample)).isEqualTo(engine.analyze(sample));
    }

    static Stream<Arguments> snapshots() {
        return Stream.of(Arguments.of(1, false), Arguments.of(66, false),
                Arguments.of(300, false), Arguments.of(300, true), Arguments.of(600, true));
    }

    @ParameterizedTest
    @MethodSource("snapshots")
    void preservesAllExistingCalculationsForTheSharedSnapshot(int count, boolean oscillating) {
        var input = input(bars(count, oscillating));
        var snapshot = new SwingSnapshotPreparer().prepare(input);
        var calculated = new SwingSuitabilityEngine().analyze(snapshot.analysis());
        var signals = new SwingSignalEngine().evaluate(snapshot.analysis(), calculated.scenario());
        var expected = new AnalysisResult(calculated.engineVersion(), calculated.mode(), calculated.totalScore(),
                calculated.factors(), calculated.strategy(), calculated.scenario(), calculated.warnings(), signals);
        var result = engine.analyze(input);
        assertThat(result.analysis()).isEqualTo(expected);
        assertThat(result.screener()).isEqualTo(new DefaultScreenerEngine(ScreenerConfig.defaults()).analyze(snapshot.screener()));
        assertThat(result.stock().currentPrice()).isEqualByComparingTo(result.screener().features().close());
        assertThat(result.priceDate()).isEqualTo(result.screener().features().high52Week().referenceDate());
        assertThat(result.asOf()).isEqualTo(result.screener().asOf());
        assertThat(result.analysis().signalReport().evaluatedAt()).isEqualTo(input.asOf().atZone(ZoneId.of("Asia/Seoul")).toInstant());
        assertThat(result.engineVersion()).isEqualTo("unified-swing-v1");
        assertThat(result.analysis().engineVersion()).isEqualTo(SwingSuitabilityEngine.VERSION);
        assertThat(result.analysis().signalReport().version()).isEqualTo(SwingSignalEngine.VERSION);
        assertThat(result.screener().engineVersion()).isEqualTo("screener-ema-v1");
        assertThat(result.screener().trendScore()).isNull();
        assertThat(result.screener().entryScore()).isNull();
        assertThat(result.screener().signal()).isNull();
        assertThat(result).isEqualTo(engine.analyze(input));
    }

    @Test void sufficientHistoryProducesAnActualScenarioAndScore() {
        var result = engine.analyze(input(bars(300, true)));
        assertThat(result.analysis().scenario()).isNotNull();
        assertThat(result.analysis().totalScore()).isNotNull();
        assertThat(result.screener().quality()).isEqualTo(ScreenerTypes.Quality.VALID);
        assertThat(result.dailyBarCount()).isEqualTo(300);
        assertThat(result.weeklyBarCount()).isEqualTo(59);
    }

    @Test void shortHistoryRetainsNullScoresAndIndependentQuality() {
        var result = engine.analyze(input(bars(1, false)));
        assertThat(result.analysis().totalScore()).isNull();
        assertThat(result.analysis().scenario()).isNull();
        assertThat(result.screener().stage()).isNull();
        assertThat(result.screener().quality()).isEqualTo(ScreenerTypes.Quality.INSUFFICIENT_DATA);
        assertThat(result.analysis().signalReport().completeness()).isEqualTo("INSUFFICIENT_DATA");
        assertThat(result.warnings()).extracting(UnifiedWarning::source)
                .contains(UnifiedWarning.Source.ANALYSIS, UnifiedWarning.Source.SIGNAL, UnifiedWarning.Source.SCREENER);
    }

    @Test void observationsReachAnalysisWithoutBeingInventedOrAffectingScreening() {
        var base = input(bars(300, true));
        var populated = new UnifiedAnalysisInput(base.stockCode(), base.stockName(), base.marketCode(), base.asOf(), base.dailyBars(),
                Map.of("swingNetBuy20", n(100), "swingFlowVolume20", n(20000), "swingPositiveFlowDays20", n(10),
                        "marketReturn20", n(0), "industryReturn20", n(0)));
        var without = engine.analyze(base);
        var with = engine.analyze(populated);
        assertThat(without.analysis().factors()).filteredOn(f -> f.factorCode().equals("SUPPLY_DEMAND"))
                .allMatch(f -> f.score() == null);
        assertThat(with.analysis().factors()).filteredOn(f -> f.factorCode().equals("SUPPLY_DEMAND"))
                .allMatch(f -> f.score().compareTo(n(50.7)) == 0);
        assertThat(with.screener()).isEqualTo(without.screener());
    }

    @Test void keepsConfirmedBreakoutSignalsIncludingWhenScenarioIsUnavailable() {
        var bars = bars(65, false);
        var last = bars.get(bars.size() - 1);
        var event=bar(last.openTime().toLocalDate().plusDays(1), 1680, 1_800_000);
        bars.add(new ScreenerBar(event.openTime(),event.closedAt(),event.open(),event.high(),n(1660),event.close(),event.volume(),null));
        var result = engine.analyze(input(bars));
        assertThat(result.analysis().signalReport().signals()).anySatisfy(signal -> {
            assertThat(signal.type()).isEqualTo("BREAKOUT");
            assertThat(signal.state()).isEqualTo(TradeSignal.State.CONFIRMED);
        });
    }

    @Test void preservesOptionalPositionStopWithoutInferringHoldings() {
        var bars = bars(66, false);
        var base = input(bars);
        var position = new PositionContext(true, bars.get(60).openTime().atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                n(1600), n(1700), List.of(), PositionContext.StopPolicy.CLOSE);
        var held = new UnifiedAnalysisInput(base.stockCode(), base.stockName(), base.marketCode(), base.asOf(),
                bars, Map.of(), position);
        assertThat(engine.analyze(held).analysis().signalReport().positionScope()).isEqualTo("HELD");
        assertThat(engine.analyze(held).analysis().signalReport().signals()).anyMatch(s -> s.type().equals("POSITION_STOP"));
        assertThat(engine.analyze(base).analysis().signalReport().positionScope()).isEqualTo("UNKNOWN");
        assertThat(engine.analyze(base).analysis().signalReport().signals()).noneMatch(s -> s.type().equals("POSITION_STOP"));
    }

    @Test void screenerConfigurationIsIsolatedFromExistingDefaultsAndAnalysis() {
        var defaults = ScreenerConfig.defaults();
        var custom = new ScreenerConfig(3, 10, 30, 100, 40, 2, 2, defaults.boundaryPercent(),
                defaults.flatSlopePercent(), defaults.strongSlopePercent(), defaults.gapFlatPercent(), "custom-test");
        var input = input(bars(120, false));
        var standard = engine.analyze(input);
        var customized = new DefaultUnifiedStockAnalysisEngine(custom).analyze(input);
        assertThat(customized.screener().config()).isEqualTo(custom);
        assertThat(customized.screener().quality()).isEqualTo(ScreenerTypes.Quality.VALID);
        assertThat(standard.screener().quality()).isEqualTo(ScreenerTypes.Quality.INSUFFICIENT_DATA);
        assertThat(customized.analysis()).isEqualTo(standard.analysis());
        assertThat(engine.analyze(input)).isEqualTo(standard);
    }

    @Test void retainsActualTradingAmountAndLeavesMissingAmountUnknown() {
        var bars = bars(210, false);
        assertThat(engine.analyze(input(bars)).screener().features().tradingAmountMillionWon()).isNull();
        var last = bars.remove(bars.size() - 1);
        bars.add(new ScreenerBar(last.openTime(), last.closedAt(), last.open(), last.high(), last.low(), last.close(),
                last.volume(), n(12345)));
        assertThat(engine.analyze(input(bars)).screener().features().tradingAmountMillionWon()).isEqualByComparingTo("12345");
    }
}
