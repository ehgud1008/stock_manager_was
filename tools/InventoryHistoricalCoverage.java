import java.sql.*;
import java.nio.file.*;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

public class InventoryHistoricalCoverage {
 static final ObjectMapper J=new ObjectMapper();
 static final Path OUT=Path.of("tools/swing-expanded-review-20261118");
 static final long CUT=1795014000000L;
 static ArrayNode query(Connection c,String sql,Object...args)throws Exception{
  var a=J.createArrayNode();try(var s=c.prepareStatement(sql)){s.setQueryTimeout(90);s.setFetchSize(500);for(int i=0;i<args.length;i++)s.setObject(i+1,args[i]);
   try(var r=s.executeQuery()){var m=r.getMetaData();while(r.next()){var x=a.addObject();for(int i=1;i<=m.getColumnCount();i++){String k=m.getColumnLabel(i).toLowerCase();Object v=r.getObject(i);if(v==null)x.putNull(k);else if(v instanceof Number n)x.set(k,J.valueToTree(n));else x.put(k,r.getString(i));}}}}
  return a;
 }
 static void record(Connection c,ObjectNode output,String name,String sql,Object...args)throws Exception{
  try{var rows=query(c,sql,args);output.set(name,rows);System.out.println(name+"="+J.writeValueAsString(rows));}
  catch(SQLException e){var error=output.putObject(name);error.put("sqlErrorCode",e.getErrorCode());System.out.println(name+"_SQL_ERROR="+e.getErrorCode());}
  J.writerWithDefaultPrettyPrinter().writeValue(OUT.resolve("db-coverage-inventory.json").toFile(),output);
 }
 public static void main(String[] args)throws Exception{
  Files.createDirectories(OUT);var p=new Properties();p.setProperty("user",System.getenv("TASK_REPORT_USER"));p.setProperty("password",System.getenv("TASK_REPORT_PASSWORD"));p.setProperty("oracle.net.CONNECT_TIMEOUT","15000");p.setProperty("oracle.jdbc.ReadTimeout","120000");
  try(var c=DriverManager.getConnection(System.getenv("TASK_REPORT_JDBC"),p)){
   c.setAutoCommit(false);try(var s=c.createStatement()){s.execute("SET TRANSACTION READ ONLY");}
   var out=J.createObjectNode();out.put("cutoff","2026-11-18");out.put("cutoffEpochMillis",CUT);out.put("databaseWrites",0);out.put("queriedAt",java.time.Instant.now().toString());
   record(c,out,"tables","SELECT table_name,num_rows FROM user_tables ORDER BY table_name");
   record(c,out,"time_price_columns","SELECT table_name,column_name,data_type FROM user_tab_columns WHERE data_type LIKE 'TIMESTAMP%' OR data_type='DATE' OR column_name LIKE '%EPOCH%' OR column_name LIKE '%PRICE%' OR column_name LIKE '%CANDLE%' OR column_name LIKE '%HISTORY%' ORDER BY table_name,column_id");
   record(c,out,"scan_run_range","SELECT COUNT(*) run_count,COUNT(DISTINCT base_date) dates,TO_CHAR(MIN(base_date),'YYYY-MM-DD') first_date,TO_CHAR(MAX(base_date),'YYYY-MM-DD') last_date,MIN(created_epoch) earliest_created,MAX(created_epoch) latest_created FROM ua_scan_run WHERE base_date<=DATE '2026-11-18'");
   record(c,out,"scan_by_date","SELECT TO_CHAR(r.base_date,'YYYY-MM-DD') base_date,COUNT(DISTINCT r.run_id) runs,COUNT(i.stock_code) items,COUNT(DISTINCT i.stock_code) stocks,SUM(CASE WHEN i.status='COMPLETED' THEN 1 ELSE 0 END) completed,SUM(CASE WHEN e.analyzed_epoch>0 AND e.analyzed_epoch<? AND e.collected_epoch<? THEN 1 ELSE 0 END) available_before_cutoff,MIN(r.created_epoch) min_created,MIN(e.analyzed_epoch) min_analyzed,MAX(e.analyzed_epoch) max_analyzed FROM ua_scan_run r LEFT JOIN ua_scan_item i ON i.run_id=r.run_id LEFT JOIN ua_eod_item e ON e.run_id=i.run_id AND e.stock_code=i.stock_code WHERE r.base_date<=DATE '2026-11-18' GROUP BY r.base_date ORDER BY r.base_date",CUT,CUT);
   record(c,out,"snapshot_kinds","SELECT kind,COUNT(*) documents,MIN(utf8_bytes) min_bytes,MAX(utf8_bytes) max_bytes FROM ua_snapshot_document GROUP BY kind");
   c.rollback();System.out.println("COVERAGE_INVENTORY_COMPLETE");
  }catch(SQLException e){System.out.println("SQL_ERROR_CODE="+e.getErrorCode());System.exit(1);}
 }
}
