import java.sql.*;
import java.time.*;

/** Read-only scheduling audit; epochs are displayed in Korea time. */
public class InspectAnalysisSchedule {
    static void query(Connection c,String label,String sql)throws SQLException{
        System.out.println("CHECK="+label);
        try(var s=c.createStatement()){s.setQueryTimeout(25);try(var r=s.executeQuery(sql)){var m=r.getMetaData();while(r.next()){var line=new StringBuilder();for(int i=1;i<=m.getColumnCount();i++){if(i>1)line.append(" | ");String name=m.getColumnLabel(i),value=r.getString(i);if(value!=null&&name.endsWith("_EPOCH"))value=Instant.ofEpochMilli(r.getLong(i)).atZone(ZoneId.of("Asia/Seoul")).toString();line.append(name).append('=').append(value);}System.out.println(line);}}}
    }
    public static void main(String[] args)throws Exception{
        try(var c=DriverManager.getConnection(System.getenv("TASK_REPORT_JDBC"),System.getenv("TASK_REPORT_USER"),System.getenv("TASK_REPORT_PASSWORD"))){
            c.setAutoCommit(false);try(var s=c.createStatement()){s.execute("SET TRANSACTION READ ONLY");}
            query(c,"RECENT_RUNS","SELECT * FROM (SELECT r.run_id,r.base_date,r.market,r.status,r.error_code,r.created_epoch,r.completed_epoch,e.trigger_kind,e.started_epoch,e.updated_epoch,e.deadline_epoch FROM ua_scan_run r LEFT JOIN ua_eod_run e ON e.run_id=r.run_id WHERE r.base_date>=DATE '2026-10-22' ORDER BY r.created_epoch DESC) WHERE ROWNUM<=12");
            query(c,"RECENT_SCHEDULE","SELECT trade_date,status,run_id,checked_epoch FROM ua_eod_schedule WHERE trade_date>=DATE '2026-10-23' ORDER BY trade_date");
            query(c,"ACTIVE_ITEMS","SELECT r.base_date,r.market,r.status run_status,i.status item_status,COUNT(*) items FROM ua_scan_run r JOIN ua_scan_item i ON i.run_id=r.run_id WHERE r.base_date>=DATE '2026-10-23' GROUP BY r.base_date,r.market,r.status,i.status ORDER BY r.base_date,i.status");
            query(c,"WORKER_LEASE","SELECT CASE WHEN owner_token IS NULL THEN 'FREE' ELSE 'HELD' END lease_status,expires_epoch,heartbeat_epoch FROM ua_eod_lease");
            c.rollback();
        }
    }
}
