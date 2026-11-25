import java.sql.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.stockanalysis.storage.SnapshotChunks;
import com.example.stockanalysis.unified.eod.EodSnapshotCodec;

/** Bounded connectivity and saved-analysis verification; no database writes. */
public class VerifyAnalysisReadOnly {
    static String object(Connection c, String id, int bytes) throws SQLException {
        try (var s = c.prepareStatement("SELECT payload_gzip FROM ua_snapshot_object WHERE sha256=?")) {
            s.setQueryTimeout(25); s.setString(1, id);
            try (var r = s.executeQuery()) {
                if (!r.next()) throw new IllegalStateException("SNAPSHOT_OBJECT_NOT_IN_DATABASE");
                byte[] gzip = r.getBytes(1);
                if (gzip == null) throw new IllegalStateException("SNAPSHOT_OBJECT_ARCHIVED");
                return EodSnapshotCodec.decode(gzip, id, bytes);
            }
        }
    }
    static void verifyDetail(Connection c, String run, String code) throws Exception {
        var mapper = new ObjectMapper();
        try (var s = c.prepareStatement("SELECT i.stock_name,i.result_snapshot_id,d.manifest_id,d.utf8_bytes,d.manifest_utf8_bytes FROM ua_scan_item i JOIN ua_snapshot_document d ON d.sha256=i.result_snapshot_id WHERE i.run_id=? AND i.stock_code=?")) {
            s.setQueryTimeout(25); s.setString(1, run); s.setString(2, code);
            try (var r = s.executeQuery()) {
                if (!r.next()) throw new IllegalStateException("SAMPLE_DETAIL_NOT_FOUND");
                var manifest = mapper.readValue(object(c, r.getString(3), r.getInt(5)), SnapshotChunks.Manifest.class);
                if (!manifest.sha256().equals(r.getString(2)) || manifest.utf8Bytes()!=r.getInt(4))
                    throw new IllegalStateException("MANIFEST_REFERENCE_MISMATCH");
                var objects = new java.util.HashMap<String,String>();
                for (var part : manifest.parts()) if (part.objectId()!=null && !objects.containsKey(part.objectId()))
                    objects.put(part.objectId(), object(c, part.objectId(), part.objectBytes()));
                String json = new SnapshotChunks(mapper).restore(manifest, objects);
                var root = mapper.readTree(json);
                var fields = new java.util.ArrayList<String>(); root.fieldNames().forEachRemaining(fields::add);
                System.out.println("DETAIL_RESTORED code="+code+" name="+r.getString(1)+" bytes="+r.getInt(4)+" integrity=VERIFIED fields="+fields);
                System.out.println("DETAIL_EXCERPT="+json.substring(0,Math.min(1600,json.length())));
            }
        }
    }
    static void query(Connection c, String label, String sql, String... params) throws SQLException {
        System.out.println("CHECK=" + label);
        try (var s = c.prepareStatement(sql)) {
            s.setQueryTimeout(25);
            for (int i = 0; i < params.length; i++) s.setString(i + 1, params[i]);
            try (var r = s.executeQuery()) {
                var m = r.getMetaData();
                int count = 0;
                while (r.next()) {
                    var out = new StringBuilder();
                    for (int i = 1; i <= m.getColumnCount(); i++) {
                        if (i > 1) out.append(" | ");
                        out.append(m.getColumnLabel(i)).append('=').append(r.getString(i));
                    }
                    System.out.println(out);
                    count++;
                }
                System.out.println("ROWS=" + count);
            }
        }
    }
    public static void main(String[] args) {
        var p = new java.util.Properties();
        p.setProperty("user", System.getenv("TASK_REPORT_USER"));
        p.setProperty("password", System.getenv("TASK_REPORT_PASSWORD"));
        p.setProperty("oracle.net.CONNECT_TIMEOUT", "15000");
        p.setProperty("oracle.jdbc.ReadTimeout", "35000");
        try (var c = DriverManager.getConnection(System.getenv("TASK_REPORT_JDBC"), p)) {
            c.setAutoCommit(false);
            try (var s = c.createStatement()) { s.execute("SET TRANSACTION READ ONLY"); }
            System.out.println("CONNECTION=OK READ_ONLY_TRANSACTION=YES");
            query(c, "RECENT_RUNS", "SELECT run_id,TO_CHAR(base_date,'YYYY-MM-DD') base_date,status FROM ua_scan_run ORDER BY base_date DESC,created_epoch DESC FETCH FIRST 3 ROWS ONLY");
            query(c, "ANALYSIS_STORAGE_COLUMNS", "SELECT table_name,column_name,data_type FROM user_tab_columns WHERE table_name IN ('UA_SCAN_ITEM','UA_EOD_ITEM') AND (column_name LIKE '%JSON%' OR column_name LIKE '%GZIP%' OR column_name LIKE '%SNAPSHOT%') ORDER BY table_name,column_id");
            String run;
            try (var s = c.createStatement()) {
                s.setQueryTimeout(25);
                try (var r = s.executeQuery("SELECT run_id FROM ua_scan_run ORDER BY base_date DESC,created_epoch DESC FETCH FIRST 1 ROW ONLY")) {
                    if (!r.next()) { System.out.println("NO_SAVED_RUNS"); c.rollback(); return; }
                    run = r.getString(1);
                }
            }
            query(c, "LATEST_RUN_COUNTS", "SELECT status,COUNT(*) item_count,SUM(CASE WHEN result_json IS NOT NULL THEN 1 ELSE 0 END) result_json_count,SUM(CASE WHEN result_snapshot_id IS NOT NULL THEN 1 ELSE 0 END) result_snapshot_count FROM ua_scan_item WHERE run_id=? GROUP BY status", run);
            query(c, "SAMPLE_ANALYSES", "SELECT stock_code,stock_name,TO_CHAR(price_date,'YYYY-MM-DD') price_date,status,current_price,volume,total_score,buy_signal,sell_signal,DBMS_LOB.GETLENGTH(result_json) json_chars FROM ua_scan_item WHERE run_id=? AND stock_code IN ('051630','128540','263860','005930') ORDER BY stock_code", run);
            query(c, "RESULT_JSON_SAMPLE", "SELECT stock_code,DBMS_LOB.SUBSTR(result_json,1000,1) result_excerpt FROM ua_scan_item WHERE run_id=? AND result_json IS NOT NULL FETCH FIRST 1 ROW ONLY", run);
            query(c, "SNAPSHOT_SAMPLE", "SELECT i.stock_code,d.kind,d.utf8_bytes,DBMS_LOB.GETLENGTH(o.payload_gzip) manifest_gzip_bytes FROM ua_scan_item i JOIN ua_snapshot_document d ON d.sha256=i.result_snapshot_id LEFT JOIN ua_snapshot_object o ON o.sha256=d.manifest_id WHERE i.run_id=? AND i.stock_code IN ('051630','128540','263860','005930') ORDER BY i.stock_code", run);
            verifyDetail(c, run, "005930");
            verifyDetail(c, run, "051630");
            c.rollback();
            System.out.println("VERIFICATION=COMPLETE DATABASE_WRITES=0");
        } catch (SQLException e) {
            System.out.println("SQL_ERROR_CODE=" + e.getErrorCode() + " SQL_STATE=" + e.getSQLState());
            System.out.println("ERROR_TYPE=" + e.getClass().getSimpleName());
            for (Throwable t = e.getCause(); t != null; t = t.getCause())
                System.out.println("CAUSE_TYPE=" + t.getClass().getSimpleName());
            System.exit(1);
        } catch (Exception e) {
            System.out.println("DETAIL_CHECK_ERROR="+e.getClass().getSimpleName());
            System.exit(1);
        }
    }
}
