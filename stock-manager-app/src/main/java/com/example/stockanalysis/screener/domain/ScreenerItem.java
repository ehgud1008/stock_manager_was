package com.example.stockanalysis.screener.domain;

import com.example.stockanalysis.engine.screener.ScreenerResult;
import java.time.LocalDate;
import java.util.List;

public record ScreenerItem(String runId, String stockCode, String stockName, String market,
        ItemStatus status, String observationSource, boolean sourceComplete,
        ScreenerResult result, List<String> warnings, String errorCode) {
    public enum ItemStatus { PENDING, ANALYZED, SKIPPED, FAILED }
    public record Stock(String stockCode, String stockName, String market, String state, LocalDate listedDate, String stockType) {}
    public ScreenerItem { warnings = List.copyOf(warnings); }
    public static ScreenerItem pending(String runId, Stock stock, String source) {
        if (!"ST".equals(stock.stockType())) {
            throw new IllegalArgumentException("Non-ST stocks must be filtered before creating persisted items");
        }
        boolean excluded = stock.state() != null && (stock.state().contains("거래정지") || stock.state().contains("관리종목") || stock.state().contains("정리매매"));
        return new ScreenerItem(runId, stock.stockCode(), stock.stockName(), stock.market(), excluded ? ItemStatus.SKIPPED : ItemStatus.PENDING,
                source, false, null, List.of(), excluded ? "STOCK_STATE_EXCLUDED" : null);
    }
}
