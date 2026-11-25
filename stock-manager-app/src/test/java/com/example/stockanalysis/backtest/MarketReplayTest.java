package com.example.stockanalysis.backtest;

import static com.example.stockanalysis.backtest.MarketStudy.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MarketReplayTest {
    private final LocalDate start = LocalDate.of(2025, 3, 3);
    private final Spec spec = new Spec(Market.KOSPI, Strategy.BREAKOUT, start, start.plusDays(8),
            10000, 1, 2, 0, 0, 0, MissingSettlement.UNRESOLVED);

    private void session(MarketReplay replay, int offset, long open, long close, long reference) {
        var row = new Quote("000001", "종목", open, Math.max(open, close), Math.min(open, close), close, 1000, 100000, reference);
        replay.session(start.plusDays(offset), Map.of(row.code(), row));
    }

    private MarketReplay entryWithGap() {
        return entryWithGap(spec);
    }

    private MarketReplay entryWithGap(Spec settings) {
        var replay = new MarketReplay(settings);
        for (int i = -21; i < 0; i++) session(replay, i, 100, 100, 100);
        session(replay, 0, 100, 110, 100);
        session(replay, 1, 125, 100, 110);
        return replay;
    }

    @Test void entersAtNextOpenAndMarksEquityBeforeProfitableExit() {
        var replay = entryWithGap();
        session(replay, 2, 101, 150, 100);
        var result = replay.finish(List.of(), "test");
        var trade = result.trades().stream().filter(t -> "CLOSED".equals(t.status())).findFirst().orElseThrow();
        assertThat(trade.quantity()).isEqualTo(80);
        assertThat(trade.entry()).isEqualByComparingTo("125");
        assertThat(trade.net()).isEqualByComparingTo("2000");
        assertThat(result.metrics().returnPct()).isEqualByComparingTo("20");
        assertThat(result.metrics().maxDrawdown()).isEqualByComparingTo("20");
        assertThat(result.metrics().winRate()).isEqualByComparingTo("100");
    }

    @Test void disappearingHoldingRetainsUnresolvedExposureAndWithholdsAccountReturn() {
        var replay = entryWithGap();
        replay.session(start.plusDays(2), Map.of("000002", new Quote("000002","다른종목",100,100,100,100,10,1000,100)));
        var result = replay.finish(List.of(), "test");
        assertThat(result.metrics().closed()).isZero();
        assertThat(result.metrics().open()).isOne();
        assertThat(result.metrics().returnPct()).isNull();
        assertThat(result.metrics().winRate()).isNull();
        assertThat(result.trades()).extracting(Trade::status).contains("UNRESOLVED");
    }

    @Test void priceBasisChangeDoesNotProduceFictitiousLossOrWin() {
        var replay = entryWithGap();
        session(replay, 2, 50, 52, 50);
        var result = replay.finish(List.of(), "test");
        assertThat(result.metrics().maxDrawdown()).isNull();
        assertThat(result.metrics().closed()).isZero();
        assertThat(result.trades()).extracting(Trade::status).contains("UNRESOLVED");
    }

    @Test void explicitZeroRecoveryScenarioRecordsTheLossAndItsAssumption() {
        var scenario = new Spec(Market.KOSPI, Strategy.BREAKOUT, start, start.plusDays(8),
                10000, 1, 2, 0, 0, 0, MissingSettlement.WRITE_OFF);
        var replay = entryWithGap(scenario);
        session(replay, 2, 50, 52, 50);
        var result = replay.finish(List.of(), "test");
        assertThat(result.metrics().returnPct()).isEqualByComparingTo("-100");
        assertThat(result.metrics().writeOffs()).isOne();
        assertThat(result.metrics().winRate()).isEqualByComparingTo("0");
        assertThat(result.trades()).extracting(Trade::status).contains("WRITE_OFF");
    }
}
