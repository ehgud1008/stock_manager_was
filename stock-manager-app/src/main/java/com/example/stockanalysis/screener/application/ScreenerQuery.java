package com.example.stockanalysis.screener.application;

import com.example.stockanalysis.engine.screener.ScreenerTimeframe;
import com.example.stockanalysis.engine.screener.ScreenerTypes.*;
import com.example.stockanalysis.screener.domain.ScreenerItem;
import com.example.stockanalysis.screener.domain.ScreenerItem.ItemStatus;
import com.example.stockanalysis.screener.domain.ScreenerRunRequest.Market;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record ScreenerQuery(String runId, Mode mode, Integer interval, Market market, Integer stage, Quality quality,
        Transition transition, Integer maxBarsSinceTransition, Boolean allSlopesUp, GapState shortMiddleGap,
        BigDecimal minTradingAmountMillionWon, String search, ItemStatus status, String sort, Boolean descending, Integer page, Integer size,
        String slope, GapState middleLongGap, String transitionFilter, Boolean watchOnly, List<String> watchedCodes) {
    public ScreenerQuery {
        mode = mode == null ? Mode.SWING : mode;
        interval = interval == null ? (mode == Mode.SWING ? 0 : 5) : interval;
        new ScreenerTimeframe(mode, interval);
        market = market == null ? Market.ALL : market;
        page = page == null ? 0 : page; size = size == null ? 50 : size;
        sort = sort == null ? "stockCode" : sort; descending = Boolean.TRUE.equals(descending);
        search = search == null ? "" : search.trim();
        slope = slope == null ? "ALL" : slope;
        transitionFilter = transitionFilter == null ? "ALL" : transitionFilter;
        watchOnly = Boolean.TRUE.equals(watchOnly);
        if (watchedCodes != null && (watchedCodes.size() > 1000 || watchedCodes.stream().anyMatch(Objects::isNull)))
            throw new IllegalArgumentException("invalid watchlist: at most 1000 non-null stock codes");
        watchedCodes = watchedCodes == null ? List.of() : watchedCodes.stream().distinct().sorted().toList();
        if (page < 0 || size < 1 || size > 100 || stage != null && (stage < 1 || stage > 6)
                || maxBarsSinceTransition != null && maxBarsSinceTransition < 0 || minTradingAmountMillionWon != null && minTradingAmountMillionWon.signum() < 0
                || search.length() > 100 || !List.of("stockCode", "stockName", "stage", "barsInStage", "tradingAmount").contains(sort)
                || !List.of("ALL", "ALL_UP", "MID_LONG_UP").contains(slope)
                || !List.of("ALL", "RECENT", "REVERSE", "5_6", "6_1", "HOLD_1").contains(transitionFilter)
                || watchedCodes.size() > 1000 || watchedCodes.stream().anyMatch(code -> !code.matches("[A-Za-z0-9]{1,20}")))
            throw new IllegalArgumentException("invalid screener filter or pagination");
    }
    public ScreenerQuery withoutStage() {
        return new ScreenerQuery(runId, mode, interval, market, null, quality, transition, maxBarsSinceTransition, allSlopesUp,
                shortMiddleGap, minTradingAmountMillionWon, search, status, sort, descending, page, size, slope, middleLongGap, transitionFilter, watchOnly, watchedCodes);
    }
    public boolean matches(ScreenerItem item) {
        var result = item.result();
        if (watchOnly && !watchedCodes.contains(item.stockCode())) return false;
        if (!slope.equals("ALL") && (result == null || result.slopes() == null || !positive(result.slopes().middleSlope()) || !positive(result.slopes().longSlope())
                || slope.equals("ALL_UP") && !positive(result.slopes().shortSlope()))) return false;
        if (middleLongGap != null && (result == null || result.middleLongGap() == null || middleLongGap != result.middleLongGap().state())) return false;
        if (!transitionFilter.equals("ALL")) {
            if (result == null) return false;
            boolean recent = result.barsSinceTransition() != null && result.barsSinceTransition() < 3;
            if (switch (transitionFilter) {
                case "RECENT" -> !recent;
                case "REVERSE" -> result.lastTransition() != Transition.REVERSE;
                case "5_6" -> !recent || !Objects.equals(result.previousDistinctStage(), 5) || !Objects.equals(result.stage(), 6);
                case "6_1" -> !recent || !Objects.equals(result.previousDistinctStage(), 6) || !Objects.equals(result.stage(), 1);
                case "HOLD_1" -> !Objects.equals(result.stage(), 1) || result.barsInStage() == null || result.barsInStage() < 4;
                default -> false;
            }) return false;
        }
        if (market != Market.ALL && !market.name().equals(item.market()) || status != null && status != item.status()) return false;
        if (!item.stockCode().contains(search) && !item.stockName().toLowerCase(java.util.Locale.ROOT).contains(search.toLowerCase(java.util.Locale.ROOT))) return false;
        if (stage != null && (result == null || !Objects.equals(stage, result.stage()))) return false;
        if (quality != null && (result == null || quality != result.quality())) return false;
        if (transition != null && (result == null || transition != result.lastTransition())) return false;
        if (maxBarsSinceTransition != null && (result == null || result.barsSinceTransition() == null || result.barsSinceTransition() > maxBarsSinceTransition)) return false;
        if (Boolean.TRUE.equals(allSlopesUp) && (result == null || !positive(result.slopes().shortSlope()) || !positive(result.slopes().middleSlope()) || !positive(result.slopes().longSlope()))) return false;
        if (shortMiddleGap != null && (result == null || shortMiddleGap != result.shortMiddleGap().state())) return false;
        return minTradingAmountMillionWon == null || result != null && result.features().tradingAmountMillionWon() != null && result.features().tradingAmountMillionWon().compareTo(minTradingAmountMillionWon) >= 0;
    }
    private boolean positive(BigDecimal value) { return value != null && value.signum() > 0; }
    public Comparator<ScreenerItem> comparator() {
        Comparator<ScreenerItem> comparator = switch (sort) {
            case "stockName" -> Comparator.comparing(ScreenerItem::stockName, order());
            case "stage" -> Comparator.comparing(i -> i.result() == null ? null : i.result().stage(), this.<Integer>order());
            case "barsInStage" -> Comparator.comparing(i -> i.result() == null ? null : i.result().barsInStage(), this.<Integer>order());
            case "tradingAmount" -> Comparator.comparing(i -> i.result() == null ? null : i.result().features().tradingAmountMillionWon(), this.<BigDecimal>order());
            default -> Comparator.comparing(ScreenerItem::stockCode, order());
        };
        return comparator.thenComparing(ScreenerItem::stockCode);
    }
    private <T extends Comparable<? super T>> Comparator<T> order() { return Comparator.nullsLast(descending ? Comparator.<T>reverseOrder() : Comparator.<T>naturalOrder()); }
}
