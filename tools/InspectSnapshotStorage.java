import java.sql.*;

/** Read-only schema diagnostics. Never prints credentials, message bodies or exception messages. */
public class InspectSnapshotStorage {
    static void query(Connection c,String label,String sql)throws SQLException{
        System.out.println("CHECK="+label);
        try(var s=c.createStatement()){
            s.setQueryTimeout(20);
            try(var r=s.executeQuery(sql)){
                var meta=r.getMetaData();while(r.next()){
                    var line=new StringBuilder();for(int i=1;i<=meta.getColumnCount();i++){if(i>1)line.append(" | ");line.append(meta.getColumnLabel(i)).append('=').append(r.getString(i));}System.out.println(line);
                }
            }
        }catch(SQLException e){System.out.println("SQL_ERROR vendorCode="+e.getErrorCode()+" state="+e.getSQLState());}
    }
    public static void main(String[] args)throws Exception{
        try(var c=DriverManager.getConnection(System.getenv("TASK_REPORT_JDBC"),System.getenv("TASK_REPORT_USER"),System.getenv("TASK_REPORT_PASSWORD"))){
            c.setAutoCommit(false);try(var s=c.createStatement()){s.execute("SET TRANSACTION READ ONLY");}
            query(c,"STORAGE_COLUMNS","SELECT table_name,column_name FROM user_tab_columns WHERE table_name LIKE 'UA_STORAGE_%' ORDER BY table_name,column_id");
            query(c,"STATE","SELECT state_id,last_level,last_error,last_archive_health,archived_objects FROM ua_storage_state");
            query(c,"INSTANCES","SELECT COUNT(*) instance_count FROM ua_storage_instance");
            query(c,"SAMPLE_TABLE","SELECT COUNT(*) sample_count FROM ua_storage_sample");
            query(c,"NOTICE_TABLE","SELECT status,COUNT(*) notice_count FROM ua_storage_notice GROUP BY status");
            query(c,"LEVEL_IDENTIFIER_PARSE","SELECT 'probe' AS level FROM dual");
            query(c,"REPORT_DELIVERY","SELECT status,error_code,COUNT(*) delivery_count FROM ua_report_delivery GROUP BY status,error_code");
            query(c,"LATEST_REPORTS","SELECT r.base_date,p.status report_status,d.status delivery_status,d.error_code FROM ua_daily_report p JOIN ua_scan_run r ON r.run_id=p.run_id LEFT JOIN ua_report_delivery d ON d.run_id=p.run_id ORDER BY r.created_epoch DESC FETCH FIRST 5 ROWS ONLY");
            c.rollback();
        }
    }
}
