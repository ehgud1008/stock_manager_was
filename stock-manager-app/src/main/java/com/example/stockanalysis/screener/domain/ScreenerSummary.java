package com.example.stockanalysis.screener.domain;

import java.math.BigDecimal;

/** Bounded list payload. Full precision snapshots and history live in ScreenerItem only. */
public record ScreenerSummary(String stockCode, String stockName, String market, String status,
        boolean sourceComplete, String quality, Integer stage, Integer previousDistinctStage,
        Integer barsInStage, Integer barsSinceTransition, boolean stageDurationComplete, String lastTransition,
        BigDecimal shortSlope, BigDecimal middleSlope, BigDecimal longSlope,
        BigDecimal middleLongGap, String gapState, BigDecimal close, BigDecimal tradingAmountMillionWon) {
    public static ScreenerSummary from(ScreenerItem item) {
        var r = item.result();
        return new ScreenerSummary(item.stockCode(), item.stockName(), item.market(), item.status().name(),
                item.sourceComplete(), r == null ? null : r.quality().name(), r == null ? null : r.stage(),
                r == null ? null : r.previousDistinctStage(), r == null ? null : r.barsInStage(),
                r == null ? null : r.barsSinceTransition(), r != null && r.stageDurationComplete(),
                r == null ? null : r.lastTransition().name(),
                r == null || r.slopes() == null ? null : r.slopes().shortSlope(),
                r == null || r.slopes() == null ? null : r.slopes().middleSlope(),
                r == null || r.slopes() == null ? null : r.slopes().longSlope(),
                r == null || r.middleLongGap() == null ? null : r.middleLongGap().signedPercent(),
                r == null || r.middleLongGap() == null ? null : r.middleLongGap().state().name(),
                r == null || r.features() == null ? null : r.features().close(),
                r == null || r.features() == null ? null : r.features().tradingAmountMillionWon());
    }
}
