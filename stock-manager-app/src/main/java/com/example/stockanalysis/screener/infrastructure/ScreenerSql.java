package com.example.stockanalysis.screener.infrastructure;

import com.example.stockanalysis.screener.application.ScreenerQuery;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** All user values are bound parameters; identifiers come exclusively from allowlists. */
final class ScreenerSql {
    private ScreenerSql() {}
    record Filter(String sql, List<Object> args) {}
    static String placeholders(int size) { return String.join(",", Collections.nCopies(size, "?")); }
    static Filter filter(String id, ScreenerQuery q) {
        var sql = new StringBuilder(" WHERE run_id = ?");
        var args = new ArrayList<Object>(); args.add(id);
        if (!q.market().name().equals("ALL")) eq(sql, args, "market", q.market().name());
        if (q.status() != null) eq(sql, args, "item_status", q.status().name());
        if (q.stage() != null) eq(sql, args, "stage", q.stage());
        if (q.quality() != null) eq(sql, args, "quality", q.quality().name());
        if (q.transition() != null) eq(sql, args, "last_transition", q.transition().name());
        if (q.shortMiddleGap() != null) eq(sql, args, "sm_gap_state", q.shortMiddleGap().name());
        if (q.middleLongGap() != null) eq(sql, args, "gap_state", q.middleLongGap().name());
        if (q.maxBarsSinceTransition() != null) { sql.append(" AND bars_since_transition <= ?"); args.add(q.maxBarsSinceTransition()); }
        if (q.minTradingAmountMillionWon() != null) { sql.append(" AND trading_amount >= ?"); args.add(q.minTradingAmountMillionWon()); }
        if (!q.search().isEmpty()) {
            sql.append(" AND (LOWER(stock_name) LIKE ? ESCAPE '!' OR stock_code LIKE ? ESCAPE '!')");
            String pattern = "%" + q.search().toLowerCase(java.util.Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            args.add(pattern); args.add(pattern);
        }
        if (Boolean.TRUE.equals(q.allSlopesUp()) || q.slope().equals("ALL_UP")) sql.append(" AND short_slope > 0 AND middle_slope > 0 AND long_slope > 0");
        else if (q.slope().equals("MID_LONG_UP")) sql.append(" AND middle_slope > 0 AND long_slope > 0");
        sql.append(switch (q.transitionFilter()) {
            case "RECENT" -> " AND bars_since_transition < 3";
            case "REVERSE" -> " AND last_transition = 'REVERSE'";
            case "5_6" -> " AND previous_distinct_stage = 5 AND stage = 6 AND bars_since_transition < 3";
            case "6_1" -> " AND previous_distinct_stage = 6 AND stage = 1 AND bars_since_transition < 3";
            case "HOLD_1" -> " AND stage = 1 AND bars_in_stage >= 4";
            default -> "";
        });
        if (q.watchOnly()) {
            if (q.watchedCodes().isEmpty()) sql.append(" AND 1 = 0");
            else { sql.append(" AND stock_code IN (").append(placeholders(q.watchedCodes().size())).append(")"); args.addAll(q.watchedCodes()); }
        }
        return new Filter(sql.toString(), args);
    }
    private static void eq(StringBuilder sql, List<Object> args, String column, Object value) {
        sql.append(" AND ").append(column).append(" = ?"); args.add(value);
    }
    static String order(ScreenerQuery q) {
        String column = switch (q.sort()) {
            case "stockName" -> "stock_name";
            case "stage" -> "stage";
            case "barsInStage" -> "bars_in_stage";
            case "tradingAmount" -> "trading_amount";
            default -> "stock_code";
        };
        return " ORDER BY " + column + (q.descending() ? " DESC" : " ASC") + " NULLS LAST" + (column.equals("stock_code") ? "" : ", stock_code ASC");
    }
}
