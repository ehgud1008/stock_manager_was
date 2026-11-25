package com.example.stockanalysis.engine.screener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/** Times are Asia/Seoul. closedAt is the earliest time the completed bar is available. */
public record ScreenerBar(LocalDateTime openTime, LocalDateTime closedAt,
                          BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close,
                          long volume, BigDecimal tradingAmountMillionWon) {
    public ScreenerBar {
        Objects.requireNonNull(openTime, "openTime");
        Objects.requireNonNull(closedAt, "closedAt");
        Objects.requireNonNull(open, "open");
        Objects.requireNonNull(high, "high");
        Objects.requireNonNull(low, "low");
        Objects.requireNonNull(close, "close");
        if (!closedAt.isAfter(openTime) || low.signum() <= 0 || high.compareTo(low) < 0
                || open.compareTo(low) < 0 || open.compareTo(high) > 0
                || close.compareTo(low) < 0 || close.compareTo(high) > 0 || volume < 0
                || (tradingAmountMillionWon != null && tradingAmountMillionWon.signum() < 0)) {
            throw new IllegalArgumentException("invalid bar time, OHLC, volume or amount");
        }
    }
}
