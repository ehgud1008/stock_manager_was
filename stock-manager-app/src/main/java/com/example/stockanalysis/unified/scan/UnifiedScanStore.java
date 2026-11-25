package com.example.stockanalysis.unified.scan;

import static com.example.stockanalysis.unified.scan.UnifiedScanModels.*;
import com.example.stockanalysis.screener.domain.ScreenerItem.Stock;
import com.example.stockanalysis.unified.application.UnifiedAnalysisResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.stereotype.Repository;

@Repository
public class UnifiedScanStore {
    private static final String COLUMNS = "stock_code,stock_name,market,status,price_date,stage,previous_stage,transition_type,"
            + "bars_since_transition,bars_in_stage,all_slopes_up,total_score,buy_signal,sell_signal,active_signals,entry_status,"
            + "quality,screener_quality,current_price,entry_from,entry_to,target_one,stop_price,reward_risk,error_code,volume,trading_amt_mil_krw,"
            + "rise_score,entry_score,risk_score,buy_state,sell_state,setup_type,target_two";
    private static final String ARCHIVED_ITEMS = "(SELECT i.*, NULL stage, NULL previous_stage, NULL transition_type,"
            + "NULL bars_since_transition, NULL bars_in_stage, NULL all_slopes_up, NULL screener_quality,"
            + "NULL volume, NULL trading_amt_mil_krw FROM an_scan_item i) archived";
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final com.example.stockanalysis.storage.SnapshotRepository snapshots;
    public UnifiedScanStore(DataSource source,ObjectMapper mapper){this(source,mapper,null);}
    @org.springframework.beans.factory.annotation.Autowired
    public UnifiedScanStore(DataSource source,ObjectMapper mapper,com.example.stockanalysis.storage.SnapshotRepository snapshots){jdbc=new JdbcTemplate(source);this.mapper=mapper;this.snapshots=snapshots;}
    public void create(String id, String key, String market, LocalDate date, String version, long now) {
        jdbc.update("INSERT INTO ua_scan_run (run_id,request_key,market,base_date,engine_version,status,created_epoch) VALUES (?,?,?,?,?,'READY',?)",
                id, key, market, java.sql.Date.valueOf(date), version, now);
    }
    public Optional<Run> byKey(String key) { return findWhere("r.request_key=?", key); }
    public Optional<Run> find(String id) { return findWhere("r.run_id=?", id); }
    private Optional<Run> findWhere(String where, String value) { return jdbc.query(runSql() + " WHERE " + where, this::run, value).stream().findFirst(); }
    public List<Run> recent() { return jdbc.query(runSql() + " ORDER BY r.created_epoch DESC,r.run_id DESC FETCH FIRST 50 ROWS ONLY", this::run); }
    public List<Run> recentArchived() {
        return jdbc.query(archivedRunSql() + " ORDER BY r.created_epoch DESC,r.run_id DESC FETCH FIRST 50 ROWS ONLY", this::run);
    }
    public Optional<Run> findArchived(String id) {
        return jdbc.query(archivedRunSql() + " WHERE r.run_id=?", this::run, id).stream().findFirst();
    }
    public Optional<Long> archivedAnalysisId(String id, String code) {
        return jdbc.query("SELECT analysis_run_id FROM an_scan_item WHERE run_id=? AND stock_code=? AND status='COMPLETED' AND analysis_run_id IS NOT NULL",
                (r, n) -> r.getLong(1), id, code).stream().findFirst();
    }
    private String archivedRunSql() {
        return "SELECT r.*, NULL price_basis,"
                + "(SELECT COUNT(*) FROM an_scan_item i WHERE i.run_id=r.run_id) total,"
                + "(SELECT COUNT(*) FROM an_scan_item i WHERE i.run_id=r.run_id AND i.status='COMPLETED') completed,"
                + "(SELECT COUNT(*) FROM an_scan_item i WHERE i.run_id=r.run_id AND i.status='FAILED') failed,"
                + "(SELECT COUNT(*) FROM an_scan_item i WHERE i.run_id=r.run_id AND i.status='SKIPPED') skipped FROM an_scan_run r";
    }
    private String runSql() {
        return "SELECT r.*, e.price_basis,e.cutoff_epoch,e.eligible_epoch,e.deadline_epoch,e.started_epoch,e.updated_epoch,e.recovery_count,e.policy_version,e.calendar_version, "
                + "(SELECT COUNT(*) FROM ua_scan_item i WHERE i.run_id=r.run_id AND i.status='PENDING') pending, "
                + "(SELECT COUNT(*) FROM ua_scan_item i WHERE i.run_id=r.run_id AND i.status='RETRY_WAIT') retrying, "
                + "(SELECT COUNT(*) FROM ua_scan_item i WHERE i.run_id=r.run_id AND i.status='DATA_UNAVAILABLE') unavailable, "
                + "(SELECT COUNT(*) FROM ua_scan_item i WHERE i.run_id=r.run_id) total, "
                + "(SELECT COUNT(*) FROM ua_scan_item i WHERE i.run_id=r.run_id AND i.status='COMPLETED') completed, "
                + "(SELECT COUNT(*) FROM ua_scan_item i WHERE i.run_id=r.run_id AND i.status='FAILED') failed, "
                + "(SELECT COUNT(*) FROM ua_scan_item i WHERE i.run_id=r.run_id AND i.status='SKIPPED') skipped FROM ua_scan_run r LEFT JOIN ua_eod_run e ON e.run_id=r.run_id";
    }
    private Run run(ResultSet r, int n) throws SQLException {
        return new Run(r.getString("run_id"), r.getString("request_key"), r.getString("market"), r.getDate("base_date").toLocalDate(),
                r.getString("engine_version"), r.getString("status"), r.getInt("initialized") == 1, r.getLong("created_epoch"),
                r.getObject("completed_epoch", Long.class), r.getString("error_code"), r.getLong("total"), r.getLong("completed"), r.getLong("failed"), r.getLong("skipped"),
                r.getString("price_basis") == null ? null : new EodRun(r.getString("price_basis"), r.getLong("cutoff_epoch"),
                        r.getLong("eligible_epoch"), r.getLong("deadline_epoch"), r.getObject("started_epoch", Long.class),
                        r.getLong("updated_epoch"), r.getInt("recovery_count"), r.getLong("pending"), r.getLong("retrying"),
                        r.getLong("unavailable"), r.getString("policy_version"), r.getString("calendar_version")));
    }
    public void state(String id, String status, String error, Long completed) {
        jdbc.update("UPDATE ua_scan_run SET status=?,error_code=?,completed_epoch=? WHERE run_id=?", status, error, completed, id);
    }
    public void initialize(String id, List<Stock> stocks) {
        initialize(id, stocks, true);
    }
    public void initialize(String id, List<Stock> stocks, boolean excludeByTradingState) {
        jdbc.batchUpdate("INSERT INTO ua_scan_item (run_id,stock_code,stock_name,market,status,error_code) VALUES (?,?,?,?,?,?)",
                stocks, 250, (statement, s) -> {
            boolean excluded = excludeByTradingState && s.state() != null && (s.state().contains("거래정지") || s.state().contains("관리종목") || s.state().contains("정리매매"));
            statement.setString(1, id); statement.setString(2, s.stockCode());
            statement.setString(3, s.stockName()); statement.setString(4, s.market());
            statement.setString(5, excluded ? "SKIPPED" : "PENDING");
            statement.setString(6, excluded ? "STOCK_STATE_EXCLUDED" : null);
        });
        jdbc.update("UPDATE ua_scan_run SET initialized=1 WHERE run_id=?", id);
    }
    public List<Item> pending(String id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM ua_scan_item WHERE run_id=? AND status IN ('PENDING','FAILED') ORDER BY stock_code", this::item, id);
    }
    public Optional<Item> nextPreviousCloseItem(String id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM ua_scan_item WHERE run_id=? AND status='PENDING' "
                + "ORDER BY stock_code FETCH FIRST 1 ROWS ONLY", this::item, id).stream().findFirst();
    }
    public void requeueFailures(String id) {
        jdbc.update("UPDATE ua_scan_item SET status='PENDING',error_code=NULL WHERE run_id=? AND status='FAILED'", id);
    }
    public Item getItem(String id, String code) {
        return jdbc.queryForObject("SELECT " + COLUMNS + " FROM ua_scan_item WHERE run_id=? AND stock_code=?", this::item, id, code);
    }
    public Optional<UnifiedAnalysisResponse> detail(String id, String code) {
        var rows=jdbc.query("SELECT result_json"+(snapshots==null?"":",result_snapshot_id")+" FROM ua_scan_item WHERE run_id=? AND stock_code=? AND status='COMPLETED'",
                (r,n)->new String[]{r.getString(1),snapshots==null?null:r.getString(2)},id,code);
        return rows.stream().findFirst().map(r->decode(r[1]==null?r[0]:snapshots.read(r[1])));
    }
    public record ScoreHistory(String runId, LocalDate priceDate, java.math.BigDecimal totalScore,
                               String quality, String engineVersion, String priceBasis) {}

    public List<ScoreHistory> scoreHistory(String id, String code) {
        // Read summary columns only. Keep one completed result per price date, including the selected run.
        var rows = jdbc.query("SELECT i.run_id,i.price_date,i.total_score,i.quality,r.engine_version,e.price_basis "
                        + "FROM ua_scan_item i JOIN ua_scan_run r ON r.run_id=i.run_id "
                        + "LEFT JOIN ua_eod_run e ON e.run_id=i.run_id "
                        + "WHERE i.stock_code=? AND i.status='COMPLETED' AND i.price_date IS NOT NULL "
                        + "AND i.price_date<=(SELECT price_date FROM ua_scan_item WHERE run_id=? AND stock_code=?) "
                        + "ORDER BY i.price_date DESC,r.created_epoch DESC,i.run_id DESC FETCH FIRST 200 ROWS ONLY",
                (r,n) -> new ScoreHistory(r.getString(1),r.getDate(2).toLocalDate(),r.getBigDecimal(3),
                        r.getString(4),r.getString(5),r.getString(6)), code,id,code);
        var byDate = new LinkedHashMap<LocalDate, ScoreHistory>();
        for (var row : rows) {
            byDate.putIfAbsent(row.priceDate(), row);
            if (row.runId().equals(id)) byDate.put(row.priceDate(), row);
        }
        return byDate.values().stream().limit(20).toList();
    }
    public void fail(String id, String code, String error) {
        jdbc.update("UPDATE ua_scan_item SET status='FAILED',error_code=? WHERE run_id=? AND stock_code=?", error, id, code);
    }
    public void complete(String id, Item i, UnifiedAnalysisResponse detail) {
        String json;
        try { json = mapper.writeValueAsString(detail); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Cannot serialize unified snapshot", e); }
        String reference=snapshots!=null&&snapshots.enabled()?snapshots.put(json,"UNIFIED_RESULT",System.currentTimeMillis()):null;
        var values=new ArrayList<Object>(Arrays.asList(
                java.sql.Date.valueOf(i.priceDate()), i.stage(), i.previousStage(), i.transition(), i.barsSinceTransition(), i.barsInStage(),
                i.allSlopesUp() == null ? null : i.allSlopesUp() ? 1 : 0, i.totalScore(), i.buySignal(), i.sellSignal(), i.activeSignals(), i.entryStatus(),
                i.quality(), i.screenerQuality(), i.currentPrice(), i.entryFrom(), i.entryTo(), i.targetOne(), i.stopPrice(), i.rewardRisk(),
                i.volume(), i.tradingAmountMillionWon(), i.riseScore(), i.entryScore(), i.riskScore(),
                i.buyState(), i.sellState(), i.setupType(), i.targetTwo(), reference==null?new SqlParameterValue(Types.CLOB,new SqlLobValue(json)):new SqlParameterValue(Types.CLOB,null)));
        if(snapshots!=null)values.add(reference);values.add(id);values.add(i.stockCode());
        // Summary and immutable document reference commit together in a single row update.
        int updated=jdbc.update("UPDATE ua_scan_item SET status='COMPLETED',price_date=?,stage=?,previous_stage=?,transition_type=?,"
            +"bars_since_transition=?,bars_in_stage=?,all_slopes_up=?,total_score=?,buy_signal=?,sell_signal=?,active_signals=?,entry_status=?,"
            +"quality=?,screener_quality=?,current_price=?,entry_from=?,entry_to=?,target_one=?,stop_price=?,reward_risk=?,volume=?,trading_amt_mil_krw=?,rise_score=?,entry_score=?,risk_score=?,buy_state=?,sell_state=?,setup_type=?,target_two=?,error_code=NULL,result_json=?"
            +(snapshots==null?"":",result_snapshot_id=?")+" WHERE run_id=? AND stock_code=?",values.toArray());
        if (updated != 1) throw new IllegalStateException("Unified scan item missing");
    }
    public Page query(Run run, Query q) {
        return query(run, q, false);
    }
    public Page query(Run run, Query q, boolean archived) {
        String from = archived ? ARCHIVED_ITEMS : "ua_scan_item";
        StringBuilder where = new StringBuilder(" WHERE run_id=?"); List<Object> args = new ArrayList<>(List.of(run.runId()));
        if (!q.search().isEmpty()) {
            where.append(" AND (stock_code LIKE ? ESCAPE '!' OR stock_name LIKE ? ESCAPE '!')");
            String text = "%" + q.search().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            args.add(text); args.add(text);
        }
        equal(where, args, "status", q.status()); equal(where, args, "quality", q.quality());
        equal(where, args, "market", q.market());
        bound(where, args, "volume>=?", q.minVolume());
        // UI/API use 100 million KRW (억원); stored ka10081 amount is million KRW.
        bound(where, args, "trading_amt_mil_krw>=?", q.minTradingAmountEok() == null ? null : q.minTradingAmountEok().multiply(java.math.BigDecimal.valueOf(100)));
        equal(where, args, "entry_status", q.entryStatus());
        equal(where, args, "setup_type", q.setup());
        equal(where, args, "transition_type", q.transition()); equal(where, args, "stage", q.stage());
        equal(where, args, "all_slopes_up", q.allSlopesUp() == null ? null : q.allSlopesUp() ? 1 : 0);
        bound(where, args, "bars_since_transition<=?", q.maxBarsSinceTransition());
        bound(where, args, "total_score>=?", q.minScore()); bound(where, args, "total_score<=?", q.maxScore());
        bound(where, args, "reward_risk>=?", q.minRewardRisk());
        if (q.signal() != null && !q.signal().isBlank()) {
            if (q.signal().equals("BUY") || q.signal().equals("SELL")) {
                String side = q.signal().equals("BUY") ? "buy" : "sell";
                where.append(" AND ").append(side).append("_signal IS NOT NULL AND (").append(side)
                        .append("_state IS NULL OR ").append(side).append("_state IN ('CONFIRMED','ACTIVE'))");
            } else if (q.signal().equals("BUY_PRELIMINARY") || q.signal().equals("SELL_PRELIMINARY")) {
                where.append(q.signal().equals("BUY_PRELIMINARY") ? " AND buy_state='PRELIMINARY'" : " AND sell_state='PRELIMINARY'");
            }
            else { where.append(" AND active_signals LIKE ? ESCAPE '!'"); args.add("%|" + q.signal().replace("_", "!_") + "|%"); }
        }
        long count = jdbc.queryForObject("SELECT COUNT(*) FROM " + from + where, Long.class, args.toArray());
        String sort = Map.of("totalScore", "total_score", "entryScore", "entry_score", "stage", "stage", "rewardRisk", "reward_risk", "stockCode", "stock_code", "barsSinceTransition", "bars_since_transition").get(q.sort());
        args.add((long) q.page() * q.size()); args.add(q.size());
        var items = jdbc.query("SELECT " + COLUMNS + " FROM " + from + where + " ORDER BY " + sort + (q.ascending() ? " ASC" : " DESC")
                + " NULLS LAST,stock_code ASC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", this::item, args.toArray());
        return new Page(run, count, q.page(), q.size(), items);
    }
    private void equal(StringBuilder sql, List<Object> args, String column, Object value) {
        if (value != null && (!(value instanceof String s) || !s.isBlank())) bound(sql, args, column + "=?", value);
    }
    private void bound(StringBuilder sql, List<Object> args, String expression, Object value) {
        if (value != null) { sql.append(" AND ").append(expression); args.add(value); }
    }
    private UnifiedAnalysisResponse decode(String value) {
        try { return mapper.readValue(value, UnifiedAnalysisResponse.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Cannot read unified snapshot", e); }
    }
    private Item item(ResultSet r, int n) throws SQLException {
        var date = r.getDate("price_date"); var up = r.getObject("all_slopes_up", Integer.class);
        return new Item(r.getString("stock_code"), r.getString("stock_name"), r.getString("market"), r.getString("status"), date == null ? null : date.toLocalDate(),
                r.getObject("stage", Integer.class), r.getObject("previous_stage", Integer.class), r.getString("transition_type"), r.getObject("bars_since_transition", Integer.class),
                r.getObject("bars_in_stage", Integer.class), up == null ? null : up == 1, r.getBigDecimal("total_score"), r.getString("buy_signal"), r.getString("sell_signal"),
                r.getString("active_signals"), r.getString("entry_status"), r.getString("quality"), r.getString("screener_quality"), r.getBigDecimal("current_price"),
                r.getBigDecimal("entry_from"), r.getBigDecimal("entry_to"), r.getBigDecimal("target_one"), r.getBigDecimal("stop_price"), r.getBigDecimal("reward_risk"), r.getString("error_code"), r.getObject("volume", Long.class), r.getBigDecimal("trading_amt_mil_krw"),
                r.getBigDecimal("rise_score"), r.getBigDecimal("entry_score"), r.getBigDecimal("risk_score"), r.getString("buy_state"),
                r.getString("sell_state"), r.getString("setup_type"), r.getBigDecimal("target_two"));
    }
}
