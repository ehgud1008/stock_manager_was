package com.example.stockanalysis.screener.application;

import com.example.stockanalysis.engine.screener.ScreenerInput;
import com.example.stockanalysis.screener.domain.ScreenerItem.Stock;
import com.example.stockanalysis.screener.domain.ScreenerRun;
import com.example.stockanalysis.screener.domain.ScreenerRunRequest.Market;
import java.util.List;

public interface ScreenerDataProvider {
    void validateStockType(String stockCode);
    List<Stock> universe(Market market);
    Collected collect(String stockCode, ScreenerRun run);
    record Collected(ScreenerInput input, boolean sourceComplete, List<String> warnings) {
        public Collected { warnings = List.copyOf(warnings); }
    }
}
