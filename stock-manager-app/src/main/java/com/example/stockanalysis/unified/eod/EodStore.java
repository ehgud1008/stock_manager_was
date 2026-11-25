package com.example.stockanalysis.unified.eod;

import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

/** Global lease row also serializes registration and fences all result writes. Never hold it across HTTP. */
@Repository
public class EodStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final com.example.stockanalysis.storage.SnapshotRepository snapshots;
    public EodStore(DataSource source) {
        this(source,null);
    }
    @org.springframework.beans.factory.annotation.Autowired
    public EodStore(DataSource source,com.example.stockanalysis.storage.SnapshotRepository snapshots) {
        this.snapshots=snapshots;
        jdbc = new JdbcTemplate(source); tx = new TransactionTemplate(new DataSourceTransactionManager(source));
    }
    public long databaseNow() { return jdbc.queryForObject("SELECT CURRENT_TIMESTAMP FROM dual", (r,n) -> r.getTimestamp(1).getTime()); }
    public <T> T locked(Supplier<T> operation) {
        return tx.execute(status -> {
            jdbc.queryForList("SELECT lease_id FROM ua_eod_lease WHERE lease_id=1 FOR UPDATE");
            return operation.get();
        });
    }
    public boolean acquire(String owner, Duration ttl) {
        return locked(() -> {
            long now = databaseNow();
            return jdbc.update("UPDATE ua_eod_lease SET owner_token=?,expires_epoch=?,heartbeat_epoch=? WHERE lease_id=1 AND (owner_token IS NULL OR expires_epoch<=? OR owner_token=?)",
                    owner, now + ttl.toMillis(), now, now, owner) == 1;
        });
    }
    public boolean heartbeat(String owner, Duration ttl) {
        return locked(() -> {
            long now = databaseNow();
            return jdbc.update("UPDATE ua_eod_lease SET expires_epoch=?,heartbeat_epoch=? WHERE lease_id=1 AND owner_token=? AND expires_epoch>?",
                    now + ttl.toMillis(), now, owner, now) == 1;
        });
    }
    public void release(String owner) {
        locked(() -> jdbc.update("UPDATE ua_eod_lease SET owner_token=NULL,expires_epoch=0 WHERE lease_id=1 AND owner_token=?", owner));
    }
    public void assertOwned(String owner) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM ua_eod_lease WHERE lease_id=1 AND owner_token=? AND expires_epoch>?",
                Integer.class, owner, databaseNow()) != 1) throw new LeaseLost();
    }
    public void owned(String owner, Runnable operation) {
        locked(() -> {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            assertOwned(owner); operation.run(); assertOwned(owner);
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            return null;
        });
    }
    public boolean live() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM ua_eod_lease WHERE lease_id=1 AND owner_token IS NOT NULL AND expires_epoch>?",
                Integer.class, databaseNow()) > 0;
    }
    public List<String> activeRuns() {
        return jdbc.queryForList("SELECT r.run_id FROM ua_scan_run r JOIN ua_eod_run e ON e.run_id=r.run_id "
                + "WHERE r.status IN ('READY','WAITING_DATA','RUNNING','RETRY_WAIT','INTERRUPTED') ORDER BY r.created_epoch,r.run_id", String.class);
    }
    public Optional<String> activePreviousClose() {
        return jdbc.queryForList("SELECT run_id FROM ua_scan_run WHERE engine_version LIKE ? "
                + "AND status IN ('READY','RUNNING') ORDER BY created_epoch,run_id FETCH FIRST 1 ROWS ONLY", String.class,
                com.example.stockanalysis.unified.scan.UnifiedScanService.PREVIOUS_CLOSE_PREFIX + "%")
                .stream().findFirst();
    }
    public Optional<String> sameDay(LocalDate date, String market) {
        return jdbc.queryForList("SELECT r.run_id FROM ua_scan_run r JOIN ua_eod_run e ON e.run_id=r.run_id WHERE r.base_date=? AND r.market=? AND e.price_basis=? ORDER BY r.created_epoch DESC",
                String.class, java.sql.Date.valueOf(date), market, EodProperties.PRICE_BASIS).stream().findFirst();
    }
    public void create(String id, String trigger, long cutoff, long eligible, long deadline, String calendar, String policy, long now) {
        jdbc.update("INSERT INTO ua_eod_run(run_id,trigger_kind,price_basis,cutoff_epoch,eligible_epoch,deadline_epoch,calendar_version,policy_version,updated_epoch) VALUES(?,?,?,?,?,?,?,?,?)",
                id, trigger, EodProperties.PRICE_BASIS, cutoff, eligible, deadline, calendar, policy, now);
    }
    public void initializeItems(String id) {
        jdbc.update("INSERT INTO ua_eod_item(run_id,stock_code) SELECT run_id,stock_code FROM ua_scan_item WHERE run_id=?", id);
    }
    public record Attempt(String code, int attempts, Long firstSample, Long readinessDeadline, String hash, long nextTry) {}
    public Optional<Attempt> nextItem(String id, long now) {
        return jdbc.query("SELECT e.* FROM ua_eod_item e JOIN ua_scan_item i ON i.run_id=e.run_id AND i.stock_code=e.stock_code "
                        + "WHERE e.run_id=? AND i.status IN ('PENDING','RETRY_WAIT') AND e.next_try_epoch<=? "
                        + "ORDER BY CASE WHEN i.status='RETRY_WAIT' THEN 0 ELSE 1 END,e.next_try_epoch,e.stock_code FETCH FIRST 1 ROWS ONLY",
                (r,n) -> new Attempt(r.getString("stock_code"), r.getInt("attempts"), r.getObject("first_sample_epoch", Long.class),
                        r.getObject("readiness_deadline", Long.class), r.getString("sample_hash"), r.getLong("next_try_epoch")), id, now).stream().findFirst();
    }
    public void touch(String id, long now) { jdbc.update("UPDATE ua_eod_run SET updated_epoch=? WHERE run_id=?", now, id); }
    public void start(String id, long now) {
        jdbc.update("UPDATE ua_eod_run SET started_epoch=COALESCE(started_epoch,?),updated_epoch=? WHERE run_id=?", now, now, id);
    }
    public int requeueRetryableFailures(String id, int maxAttempts) {
        return jdbc.update("UPDATE ua_scan_item SET status='RETRY_WAIT' WHERE run_id=? AND status='FAILED' AND EXISTS "
                + "(SELECT 1 FROM ua_eod_item e WHERE e.run_id=ua_scan_item.run_id AND e.stock_code=ua_scan_item.stock_code AND e.retryable=1 AND e.attempts<?)", id, maxAttempts);
    }
    public void recordRecovery(String id) {
        jdbc.update("UPDATE ua_eod_run SET recovery_count=recovery_count+1 WHERE run_id=?", id);
    }
    public void attempt(String id, Attempt item, long now, long deadline) {
        jdbc.update("UPDATE ua_eod_item SET attempts=attempts+1,readiness_deadline=COALESCE(readiness_deadline,?) WHERE run_id=? AND stock_code=?", deadline, id, item.code());
        jdbc.update("UPDATE ua_scan_item SET status='RETRY_WAIT',error_code='COLLECTION_IN_PROGRESS' WHERE run_id=? AND stock_code=?", id, item.code());
        touch(id, now);
    }
    public void waitForData(String id, Attempt item, String hash, long now, long next, String error) {
        // A readiness observation is not a transport failure and does not consume its retry budget.
        jdbc.update("UPDATE ua_eod_item SET attempts=GREATEST(0,attempts-1),sample_hash=?,first_sample_epoch=?,next_try_epoch=? WHERE run_id=? AND stock_code=?",
                hash, hash == null ? null : now, next, id, item.code());
        jdbc.update("UPDATE ua_scan_item SET status='RETRY_WAIT',error_code=? WHERE run_id=? AND stock_code=?", error, id, item.code());
    }
    public void failure(String id, String code, String error, boolean retry, long next, boolean unavailable) {
        jdbc.update("UPDATE ua_eod_item SET retryable=?,next_try_epoch=? WHERE run_id=? AND stock_code=?", retry ? 1 : 0, next, id, code);
        jdbc.update("UPDATE ua_scan_item SET status=?,error_code=? WHERE run_id=? AND stock_code=?",
                unavailable ? "DATA_UNAVAILABLE" : retry ? "RETRY_WAIT" : "FAILED", error, id, code);
    }
    public void snapshot(String id, String code, String input, long collected, long analyzed) {
        if(snapshots!=null&&snapshots.enabled()){
            String reference=snapshots.put(input,"EOD_INPUT",analyzed);
            jdbc.update("UPDATE ua_eod_item SET input_json=NULL,input_gzip=NULL,input_snapshot_id=?,input_sha256=?,input_utf8_bytes=?,collected_epoch=?,analyzed_epoch=?,retryable=0 WHERE run_id=? AND stock_code=?",
                reference,reference,input.getBytes(java.nio.charset.StandardCharsets.UTF_8).length,collected,analyzed,id,code);return;
        }
        var encoded=EodSnapshotCodec.encode(input);
        jdbc.update("UPDATE ua_eod_item SET input_json=NULL,input_gzip=?,input_sha256=?,input_utf8_bytes=?,collected_epoch=?,analyzed_epoch=?,retryable=0"+(snapshots==null?"":",input_snapshot_id=NULL")+" WHERE run_id=? AND stock_code=?",
                new org.springframework.jdbc.core.SqlParameterValue(java.sql.Types.BLOB, new org.springframework.jdbc.core.support.SqlLobValue(encoded.gzip())),encoded.sha256(),encoded.utf8Bytes(),collected,analyzed,id,code);
    }
    public int initAttempts(String id) { return jdbc.queryForObject("SELECT init_attempts FROM ua_eod_run WHERE run_id=?", Integer.class, id); }
    public long nextInit(String id) { return jdbc.queryForObject("SELECT next_init_epoch FROM ua_eod_run WHERE run_id=?", Long.class, id); }
    public void initAttempt(String id, long next) { jdbc.update("UPDATE ua_eod_run SET init_attempts=init_attempts+1,next_init_epoch=? WHERE run_id=?", next, id); }
    public void expireItems(String id) {
        jdbc.update("UPDATE ua_scan_item SET status='DATA_UNAVAILABLE',error_code='RUN_DEADLINE_EXCEEDED' WHERE run_id=? AND status IN ('PENDING','RETRY_WAIT')", id);
        jdbc.update("UPDATE ua_eod_item SET retryable=0 WHERE run_id=?", id);
    }
    public void schedule(LocalDate date, String status, String runId, long now) {
        var day = java.sql.Date.valueOf(date);
        if (jdbc.update("UPDATE ua_eod_schedule SET status=?,run_id=?,checked_epoch=? WHERE trade_date=? AND price_basis=?", status, runId, now, day, EodProperties.PRICE_BASIS) == 0)
            jdbc.update("INSERT INTO ua_eod_schedule(trade_date,price_basis,status,run_id,checked_epoch) VALUES(?,?,?,?,?)", day, EodProperties.PRICE_BASIS, status, runId, now);
    }
    public static final class LeaseLost extends RuntimeException {}
}
