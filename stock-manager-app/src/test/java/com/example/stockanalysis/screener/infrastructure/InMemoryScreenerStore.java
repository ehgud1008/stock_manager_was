package com.example.stockanalysis.screener.infrastructure;

import com.example.stockanalysis.engine.screener.ScreenerInput;
import com.example.stockanalysis.engine.screener.ScreenerTypes.Mode;
import com.example.stockanalysis.screener.application.ScreenerStore;
import com.example.stockanalysis.screener.application.ScreenerQuery;
import com.example.stockanalysis.screener.domain.ScreenerSummary;
import com.example.stockanalysis.screener.domain.ScreenerItem;
import com.example.stockanalysis.screener.domain.ScreenerRun;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Unit-test fixture only. Application runtime always uses JdbcScreenerStore. */
public final class InMemoryScreenerStore implements ScreenerStore {
    private final Map<String, ScreenerRun> runs = new ConcurrentHashMap<>();
    private final Map<String, ScreenerItem> items = new ConcurrentHashMap<>();
    private final Map<String, ScreenerInput> inputs = new ConcurrentHashMap<>();
    @Override public synchronized void create(ScreenerRun run) {
        if (findByRequestKey(run.requestKey()).isPresent() || runs.containsKey(run.runId())) throw new IllegalArgumentException("duplicate run");
        runs.put(run.runId(), run);
    }
    @Override public void saveRun(ScreenerRun run) { runs.put(run.runId(), run); }
    @Override public synchronized void initialize(ScreenerRun run, List<ScreenerItem> values) {
        values.forEach(item -> saveItem(item, null));
        saveRun(run);
    }
    @Override public Optional<ScreenerRun> find(String id) { return Optional.ofNullable(runs.get(id)); }
    @Override public Optional<ScreenerRun> findByRequestKey(String key) { return runs.values().stream().filter(r -> r.requestKey().equals(key)).findFirst(); }
    @Override public Optional<ScreenerRun> latest(Mode mode, int interval) {
        return runs.values().stream().filter(r -> r.request().mode() == mode && r.request().interval() == interval
                && (r.status() == ScreenerRun.Status.COMPLETED || r.status() == ScreenerRun.Status.COMPLETED_WITH_ERRORS))
                .max(Comparator.comparing(ScreenerRun::createdAt).thenComparing(ScreenerRun::runId));
    }
    @Override public List<ScreenerItem> items(String id) {
        return items.values().stream().filter(i -> i.runId().equals(id)).sorted(Comparator.comparing(ScreenerItem::stockCode)).toList();
    }
    @Override public void saveItem(ScreenerItem item, ScreenerInput input) {
        items.put(item.runId() + ":" + item.stockCode(), item);
        if (input != null) inputs.put(item.runId() + ":" + item.stockCode(), input);
    }
    @Override public Optional<ScreenerItem> item(String id, String code) { return Optional.ofNullable(items.get(id + ":" + code)); }
    @Override public ResultPage query(String id, ScreenerQuery query) {
        var all = items(id);
        var matching = all.stream().filter(query::matches).sorted(query.comparator()).toList();
        var counts = new LinkedHashMap<Integer, Long>();
        for (int stage = 1; stage <= 6; stage++) {
            int s = stage;
            counts.put(stage, all.stream().filter(query.withoutStage()::matches).filter(i -> i.result() != null && Objects.equals(i.result().stage(), s)).count());
        }
        long recent = all.stream().filter(i -> i.result() != null && i.result().barsSinceTransition() != null && i.result().barsSinceTransition() < 3).count();
        long watched = all.stream().filter(i -> query.watchedCodes().contains(i.stockCode())).count();
        int start = (int) Math.min((long) query.page() * query.size(), matching.size());
        return new ResultPage(matching.size(), counts, recent, watched,
                matching.subList(start, Math.min(start + query.size(), matching.size())).stream().map(ScreenerSummary::from).toList(), all.size());
    }
    @Override public Optional<ScreenerInput> input(String id, String code) { return Optional.ofNullable(inputs.get(id + ":" + code)); }
    @Override public String storageMode() { return "MEMORY"; }
}
