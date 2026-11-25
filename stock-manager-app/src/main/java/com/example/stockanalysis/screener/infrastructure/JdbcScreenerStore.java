package com.example.stockanalysis.screener.infrastructure;

import com.example.stockanalysis.engine.screener.ScreenerInput;
import com.example.stockanalysis.engine.screener.ScreenerTypes.Mode;
import com.example.stockanalysis.screener.application.ScreenerStore;
import com.example.stockanalysis.screener.application.ScreenerQuery;
import com.example.stockanalysis.screener.domain.ScreenerItem;
import com.example.stockanalysis.screener.domain.ScreenerRun;
import com.example.stockanalysis.screener.domain.ScreenerSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

public final class JdbcScreenerStore implements ScreenerStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final TransactionTemplate transaction;
    private final com.example.stockanalysis.storage.SnapshotRepository snapshots;
    public JdbcScreenerStore(DataSource dataSource, ObjectMapper mapper) {
        this(dataSource,mapper,null);
    }
    public JdbcScreenerStore(DataSource dataSource,ObjectMapper mapper,com.example.stockanalysis.storage.SnapshotRepository snapshots){
        this.snapshots=snapshots;
        this.jdbc = new JdbcTemplate(dataSource);
        this.mapper = mapper;
        this.transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }
    @Override public void create(ScreenerRun run) {
        jdbc.update("INSERT INTO sc_screening_run (run_id, request_key, analysis_mode, minute_interval, created_epoch, status, run_json) VALUES (?, ?, ?, ?, ?, ?, ?)",
                run.runId(), run.requestKey(), run.request().mode().name(), run.request().interval(), run.createdAt().toEpochMilli(), run.status().name(), json(run));
    }
    @Override public void saveRun(ScreenerRun run) {
        int count = jdbc.update("UPDATE sc_screening_run SET status = ?, run_json = ? WHERE run_id = ?", run.status().name(), json(run), run.runId());
        if (count != 1) throw new IllegalStateException("run missing");
    }
    @Override public void initialize(ScreenerRun run, List<ScreenerItem> values) {
        transaction.executeWithoutResult(ignored -> {
            values.forEach(item -> saveItem(item, null));
            saveRun(run);
        });
    }
    @Override public Optional<ScreenerRun> find(String id) { return readRun("SELECT run_json FROM sc_screening_run WHERE run_id = ?", id); }
    @Override public Optional<ScreenerRun> findByRequestKey(String key) { return readRun("SELECT run_json FROM sc_screening_run WHERE request_key = ?", key); }
    private Optional<ScreenerRun> readRun(String sql, Object... args) {
        return jdbc.query(sql, (rs, n) -> read(rs.getString(1), ScreenerRun.class), args).stream().findFirst();
    }
    @Override public Optional<ScreenerRun> latest(Mode mode, int interval) {
        return readRun("SELECT run_json FROM sc_screening_run WHERE analysis_mode = ? AND minute_interval = ? AND status IN ('COMPLETED', 'COMPLETED_WITH_ERRORS') ORDER BY created_epoch DESC, run_id DESC FETCH FIRST 1 ROWS ONLY", mode.name(), interval);
    }
    @Override public List<ScreenerItem> items(String id) {
        return jdbc.query("SELECT item_json FROM sc_screening_item WHERE run_id = ? ORDER BY stock_code", (rs, n) -> read(rs.getString(1), ScreenerItem.class), id);
    }
    @Override public Optional<ScreenerItem> item(String id, String code) {
        return jdbc.query("SELECT item_json FROM sc_screening_item WHERE run_id = ? AND stock_code = ?",
                (rs, n) -> read(rs.getString(1), ScreenerItem.class), id, code).stream().findFirst();
    }
    @Override public ResultPage query(String id, ScreenerQuery query) {
        // Aggregate scalars only; neither this query nor the paged select accesses a CLOB.
        var broad = ScreenerSql.filter(id, query.withoutStage());
        var counts = new LinkedHashMap<Integer, Long>();
        for (int stage = 1; stage <= 6; stage++) counts.put(stage, 0L);
        long[] total = {0};
        jdbc.query("SELECT stage, COUNT(*) AS cnt FROM sc_screening_summary" + broad.sql() + " GROUP BY stage", rs -> {
            Integer stage = rs.getObject("stage", Integer.class);
            long count = rs.getLong("cnt");
            if (stage != null) counts.put(stage, count);
            if (query.stage() == null || query.stage().equals(stage)) total[0] += count;
        }, broad.args().toArray());
        var filtered = ScreenerSql.filter(id, query);
        var args = new ArrayList<>(filtered.args());
        args.add((long) query.page() * query.size()); args.add(query.size());
        var content = jdbc.query("SELECT stock_code, stock_name, market, item_status, source_complete, quality, stage, previous_distinct_stage,"
                + " bars_in_stage, bars_since_transition, duration_complete, last_transition, short_slope, middle_slope, long_slope,"
                + " ml_gap, gap_state, close_price, trading_amount FROM sc_screening_summary" + filtered.sql()
                + ScreenerSql.order(query) + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", (rs, n) -> new ScreenerSummary(
                rs.getString("stock_code"), rs.getString("stock_name"), rs.getString("market"), rs.getString("item_status"),
                rs.getInt("source_complete") == 1, rs.getString("quality"), rs.getObject("stage", Integer.class),
                rs.getObject("previous_distinct_stage", Integer.class), rs.getObject("bars_in_stage", Integer.class),
                rs.getObject("bars_since_transition", Integer.class), rs.getInt("duration_complete") == 1, rs.getString("last_transition"),
                rs.getBigDecimal("short_slope"), rs.getBigDecimal("middle_slope"), rs.getBigDecimal("long_slope"),
                rs.getBigDecimal("ml_gap"), rs.getString("gap_state"), rs.getBigDecimal("close_price"), rs.getBigDecimal("trading_amount")), args.toArray());
        // Top counters describe the entire pinned run, independently of the current filters/page.
        long[] topCounts = jdbc.queryForObject("SELECT COUNT(*) AS total, COUNT(CASE WHEN bars_since_transition < 3 THEN 1 END) AS recent FROM sc_screening_summary WHERE run_id = ?",
                (rs, n) -> new long[]{rs.getLong("total"), rs.getLong("recent")}, id);
        long watched = 0;
        if (!query.watchedCodes().isEmpty()) {
            var watchArgs = new ArrayList<Object>(); watchArgs.add(id); watchArgs.addAll(query.watchedCodes());
            watched = jdbc.queryForObject("SELECT COUNT(*) FROM sc_screening_summary WHERE run_id = ? AND stock_code IN ("
                    + ScreenerSql.placeholders(query.watchedCodes().size()) + ")", Long.class, watchArgs.toArray());
        }
        return new ResultPage(total[0], counts, topCounts[1], watched, content, topCounts[0]);
    }
    @Override public void saveItem(ScreenerItem item, ScreenerInput input) {
        transaction.executeWithoutResult(ignored -> {
            String inputJson = input == null ? null : json(input);
            if(input!=null&&snapshots!=null&&snapshots.enabled()){
                String reference=snapshots.put(inputJson,"SCREENER_INPUT",System.currentTimeMillis());
                int count=jdbc.update("UPDATE sc_screening_item SET item_json=?,input_json=NULL,input_snapshot_id=? WHERE run_id=? AND stock_code=?",json(item),reference,item.runId(),item.stockCode());
                if(count==0)jdbc.update("INSERT INTO sc_screening_item(run_id,stock_code,item_json,input_snapshot_id) VALUES(?,?,?,?)",item.runId(),item.stockCode(),json(item),reference);
                saveSummary(item);return;
            }
            int count = input == null
                    ? jdbc.update("UPDATE sc_screening_item SET item_json = ? WHERE run_id = ? AND stock_code = ?", json(item), item.runId(), item.stockCode())
                    : jdbc.update("UPDATE sc_screening_item SET item_json = ?, input_json = ?"+(snapshots==null?"":",input_snapshot_id=NULL")+" WHERE run_id = ? AND stock_code = ?", json(item), inputJson, item.runId(), item.stockCode());
            if (count == 0) jdbc.update("INSERT INTO sc_screening_item (run_id, stock_code, item_json, input_json) VALUES (?, ?, ?, ?)", item.runId(), item.stockCode(), json(item), inputJson);
            saveSummary(item);
        });
    }
    private void saveSummary(ScreenerItem item) {
        var s = ScreenerSummary.from(item);
        var values = new ArrayList<Object>(Arrays.asList(s.stockName(), s.market(), s.status(), s.sourceComplete() ? 1 : 0,
                s.quality(), s.stage(), s.previousDistinctStage(), s.barsInStage(), s.barsSinceTransition(), s.stageDurationComplete() ? 1 : 0,
                s.lastTransition(), s.shortSlope(), s.middleSlope(), s.longSlope(), s.middleLongGap(), s.gapState(),
                item.result() == null || item.result().shortMiddleGap() == null ? null : item.result().shortMiddleGap().state().name(),
                s.close(), s.tradingAmountMillionWon(), item.runId(), item.stockCode()));
        String columns = "stock_name,market,item_status,source_complete,quality,stage,previous_distinct_stage,bars_in_stage,bars_since_transition,"
                + "duration_complete,last_transition,short_slope,middle_slope,long_slope,ml_gap,gap_state,sm_gap_state,close_price,trading_amount";
        String assignments = String.join(",", Arrays.stream(columns.split(",")).map(c -> c + " = ?").toList());
        int count = jdbc.update("UPDATE sc_screening_summary SET " + assignments + " WHERE run_id = ? AND stock_code = ?", values.toArray());
        if (count == 0) jdbc.update("INSERT INTO sc_screening_summary (" + columns + ",run_id,stock_code) VALUES ("
                + ScreenerSql.placeholders(values.size()) + ")", values.toArray());
    }
    @Override public Optional<ScreenerInput> input(String id, String code) {
        var rows=jdbc.query("SELECT input_json"+(snapshots==null?"":",input_snapshot_id")+" FROM sc_screening_item WHERE run_id=? AND stock_code=? AND (input_json IS NOT NULL"+(snapshots==null?"":" OR input_snapshot_id IS NOT NULL")+")",
            (r,n)->new String[]{r.getString(1),snapshots==null?null:r.getString(2)},id,code);
        return rows.stream().findFirst().map(r->read(r[1]==null?r[0]:snapshots.read(r[1]),ScreenerInput.class));
    }
    private String json(Object value) {
        try { return mapper.writeValueAsString(value); } catch (JsonProcessingException e) { throw new IllegalStateException("cannot serialize screener snapshot", e); }
    }
    private <T> T read(String value, Class<T> type) {
        try { return mapper.readValue(value, type); } catch (JsonProcessingException e) { throw new IllegalStateException("cannot read screener snapshot", e); }
    }
    @Override public String storageMode() { return "DATABASE"; }
}
