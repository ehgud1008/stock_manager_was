package com.example.stockanalysis.screener.application;

import com.example.stockanalysis.engine.screener.ScreenerInput;
import com.example.stockanalysis.engine.screener.ScreenerTypes.Mode;
import com.example.stockanalysis.screener.domain.ScreenerItem;
import com.example.stockanalysis.screener.domain.ScreenerRun;
import com.example.stockanalysis.screener.domain.ScreenerSummary;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ScreenerStore {
    void create(ScreenerRun run);
    void saveRun(ScreenerRun run);
    void initialize(ScreenerRun run, List<ScreenerItem> items);
    Optional<ScreenerRun> find(String id);
    Optional<ScreenerRun> findByRequestKey(String requestKey);
    Optional<ScreenerRun> latest(Mode mode, int interval);
    List<ScreenerItem> items(String id);
    Optional<ScreenerItem> item(String id, String stockCode);
    ResultPage query(String id, ScreenerQuery query);
    record ResultPage(long totalElements, Map<Integer, Long> stageCounts, long recentCount, long watchCount, List<ScreenerSummary> content, long universeCount) {}
    void saveItem(ScreenerItem item, ScreenerInput input);
    Optional<ScreenerInput> input(String id, String stockCode);
    String storageMode();
}
