import com.fasterxml.jackson.databind.*;
import java.sql.*;

/** Read-only inspection of a stock's saved selection and subsequent observations. */
public class InspectSelectedStock {
    static final ObjectMapper JSON=new ObjectMapper();
    static void query(Connection c,String label,String sql,String... args)throws Exception{
        System.out.println("CHECK="+label);
        try(var s=c.prepareStatement(sql)){s.setQueryTimeout(30);for(int i=0;i<args.length;i++)s.setString(i+1,args[i]);try(var r=s.executeQuery()){var m=r.getMetaData();while(r.next()){var row=JSON.createObjectNode();for(int i=1;i<=m.getColumnCount();i++)row.put(m.getColumnLabel(i),r.getString(i));System.out.println(row);}}}
        catch(SQLException e){System.out.println("SQL_ERROR="+e.getErrorCode());}
    }
    public static void main(String[] args)throws Exception{
        String code="373220";
        try(var c=DriverManager.getConnection(System.getenv("TASK_REPORT_JDBC"),System.getenv("TASK_REPORT_USER"),System.getenv("TASK_REPORT_PASSWORD"))){
            c.setAutoCommit(false);try(var s=c.createStatement()){s.execute("SET TRANSACTION READ ONLY");}
            try(var s=c.prepareStatement("SELECT r.run_id,r.base_date,p.report_json FROM ua_daily_report p JOIN ua_scan_run r ON r.run_id=p.run_id WHERE r.base_date>=DATE '2026-10-19' AND r.base_date<=DATE '2026-10-22' AND p.status='READY' ORDER BY r.base_date")){
                try(var r=s.executeQuery()){while(r.next()){var report=JSON.readTree(r.getString(3));for(var item:report.path("candidates"))if(code.equals(item.path("code").asText())){var row=JSON.createObjectNode();row.put("runId",r.getString(1));row.put("date",r.getString(2));row.set("finalizedAt",report.path("finalizedAt"));row.set("candidate",item);System.out.println("SELECTION="+row);}}}
            }
            query(c,"EOD_HISTORY","SELECT r.run_id,r.base_date,i.status,i.current_price,i.error_code,e.collected_epoch,CASE WHEN e.input_json IS NOT NULL OR e.input_gzip IS NOT NULL OR e.input_snapshot_id IS NOT NULL THEN 1 ELSE 0 END has_input FROM ua_scan_item i JOIN ua_scan_run r ON r.run_id=i.run_id LEFT JOIN ua_eod_item e ON e.run_id=i.run_id AND e.stock_code=i.stock_code WHERE i.stock_code=? AND r.base_date>=DATE '2026-10-19' AND r.base_date<=DATE '2026-10-22' ORDER BY r.base_date,r.created_epoch",code);
            query(c,"EVALUATION","SELECT r.base_date,e.status,e.revision,e.error_code,e.updated_epoch FROM ua_eval_run e JOIN ua_scan_run r ON r.run_id=e.run_id WHERE r.base_date>=DATE '2026-10-19' ORDER BY r.base_date");
            query(c,"EVALUATION_OBSERVATIONS","SELECT run_id,trade_date,detail_json FROM ua_eval_observation WHERE observation_kind='STOCK' AND asset_code=? ORDER BY trade_date",code);
            c.rollback();
        }
    }
}
