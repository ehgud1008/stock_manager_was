package com.example.stockanalysis.backtest;

import static com.example.stockanalysis.backtest.SwingBacktestModels.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.stockanalysis.engine.model.DailyCandle;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class SwingBacktestEngineTest {
    private static final String STOCK = "005930";
    private static final List<LocalDate> DATES = IntStream.range(0, 26)
            .mapToObj(i -> LocalDate.of(2026, 1, 5).plusDays(i)).toList();

    private final SwingBacktestEngine engine = new SwingBacktestEngine();

    @Test
    void selectsFromClosedHistoryAndChargesBothSidesOfTheFollowingTrade() {
        var run = run(2, 4);
        var bars = bars();
        var result = engine.replay(run, "HISTORICAL", DATES.get(24), historical(run, bars));

        assertThat(result.trades()).singleElement().satisfies(trade -> {
            assertThat(trade.signal().date()).isEqualTo(DATES.get(21));
            assertThat(trade.signal().breakoutPrice()).isEqualByComparingTo("101");
            assertThat(trade.entryDate()).isEqualTo(DATES.get(22));
            assertThat(trade.entryPrice()).isEqualByComparingTo("121.2");
            assertThat(trade.exitDate()).isEqualTo(DATES.get(23));
            assertThat(trade.exitPrice()).isEqualByComparingTo("128.7");
            assertThat(trade.netPnl()).isEqualByComparingTo("405.081");
        });
        assertThat(result.summary().winRate()).isEqualByComparingTo("100");
        assertThat(result.summary().closed()).isEqualTo(1);
        var paperResult = engine.replay(run, "PAPER", DATES.get(24), paper(bars));
        assertThat(paperResult.trades()).isEqualTo(result.trades());
    }

    @Test
    void freezesTheWindowAndKeepsLaterPaperClosesOutOfItsWinRate() {
        var run = run(2, 2);
        var frames = paper(bars());
        var atEnd = engine.replay(run, "PAPER", DATES.get(22), frames);
        var later = engine.replay(run, "PAPER", DATES.get(23), frames);

        assertThat(atEnd.summary().open()).isEqualTo(1);
        assertThat(atEnd.summary().winRate()).isNull();
        assertThat(later.summary()).isEqualTo(atEnd.summary());
        assertThat(later.equity()).isEqualTo(atEnd.equity());
        assertThat(later.followUpTrades()).singleElement().satisfies(trade ->
                assertThat(trade.exitDate()).isEqualTo(DATES.get(23)));
    }

    @Test
    void neverRecoversAMissedEntryFromALaterPaperCapture() {
        var run = run(2, 4);
        var frames = paper(bars());
        frames.remove(DATES.get(22));
        var result = engine.replay(run, "PAPER", DATES.get(24), frames);

        assertThat(result.status()).isEqualTo("PARTIAL");
        assertThat(result.capturedSessions()).isEqualTo(3);
        assertThat(result.trades()).singleElement().satisfies(trade -> assertThat(trade.status()).isEqualTo("DATA_GAP"));
        assertThat(result.summary().winRate()).isNull();
        assertThat(result.summary().drawdownPercent()).isNull();
    }

    @Test
    void cannotTradeOnARebasedPriceSeriesAcrossPaperCaptures() {
        var run = run(2, 4);
        var bars = bars();
        var frames = paper(bars);
        var rebased = new ArrayList<>(bars.subList(0, 23));
        rebased.set(21, candle(21, 80, 90, 89));
        frames.put(DATES.get(22), frame(rebased));
        var result = engine.replay(run, "PAPER", DATES.get(24), frames);

        assertThat(result.trades()).singleElement().satisfies(trade -> assertThat(trade.status()).isEqualTo("DATA_GAP"));
        assertThat(result.status()).isEqualTo("PARTIAL");
    }

    private Run run(int holding, int sessions) {
        var settings = new Request(List.of(STOCK), DATES.get(21), null, holding, new BigDecimal("10000"), 1, 100, 100);
        return new Run(UUID.randomUUID(), STRATEGY, settings, "TEST", DATES.subList(0, 21),
                DATES.subList(21, 21 + sessions), DATES.subList(21 + sessions, 26), Instant.EPOCH);
    }

    private List<DailyCandle> bars() {
        var bars = new ArrayList<DailyCandle>();
        for (int i = 0; i < 21; i++) bars.add(candle(i, 100, 101, 100));
        bars.add(candle(21, 102, 111, 110));
        bars.add(candle(22, 120, 122, 120));
        bars.add(candle(23, 125, 132, 130));
        bars.add(candle(24, 126, 132, 128));
        bars.add(candle(25, 126, 132, 128));
        return bars;
    }

    private DailyCandle candle(int i, int open, int high, int close) {
        return new DailyCandle(DATES.get(i), BigDecimal.valueOf(open), BigDecimal.valueOf(high),
                BigDecimal.valueOf(Math.min(open, close) - 1), BigDecimal.valueOf(close), 1000L);
    }

    private Frame frame(List<DailyCandle> bars) {
        return new Frame(Instant.EPOCH, Map.of(STOCK, bars));
    }

    private Map<LocalDate, Frame> historical(Run run, List<DailyCandle> bars) {
        var frames = new HashMap<LocalDate, Frame>();
        run.sessions().forEach(date -> frames.put(date, frame(bars)));
        return frames;
    }

    private Map<LocalDate, Frame> paper(List<DailyCandle> bars) {
        var frames = new HashMap<LocalDate, Frame>();
        for (var date : DATES.subList(21, 26)) {
            frames.put(date, frame(bars.subList(0, DATES.indexOf(date) + 1)));
        }
        return frames;
    }
}
