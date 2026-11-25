import java.sql.*;
import java.nio.file.*;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
public class InspectHistoricalPrices {
 static final ObjectMapper J=new ObjectMapper();static final Path OUT=Path.of("tools/swing-expanded-review-20261118");
 static final String CUT="TO_TIMESTAMP_TZ('2026-11-19 00:00:00 Asia/Seoul','YYYY-MM-DD HH24:MI:SS TZR')";
 static ArrayNode query(Connection c,String sql)throws Exception{var a=J.createArrayNode();try(var s=c.createStatement()){s.setQueryTimeout(90);s.setFetchSize(500);try(var r=s.executeQuery(sql)){var m=r.getMetaData();while(r.next()){var x=a.addObject();for(int i=1;i<=m.getColumnCount();i++){Object v=r.getObject(i);String k=m.getColumnLabel(i).toLowerCase();if(v==null)x.putNull(k);else if(v instanceof Number n)x.set(k,J.valueToTree(n));else x.put(k,r.getString(i));}}}}return a;}
 static void record(Connection c,ObjectNode out,String label,String sql)throws Exception{try{var rows=query(c,sql);out.set(label,rows);System.out.println(label+"="+rows);}catch(SQLException e){out.putObject(label).put("sql_error_code",e.getErrorCode());System.out.println(label+" ERROR="+e.getErrorCode());}J.writerWithDefaultPrettyPrinter().writeValue(OUT.resolve("db-history-details.json").toFile(),out);}
 public static void main(String[] args)throws Exception{
  var p=new Properties();p.setProperty("user",System.getenv("TASK_REPORT_USER"));p.setProperty("password",System.getenv("TASK_REPORT_PASSWORD"));p.setProperty("oracle.net.CONNECT_TIMEOUT","15000");p.setProperty("oracle.jdbc.ReadTimeout","120000");
  try(var c=DriverManager.getConnection(System.getenv("TASK_REPORT_JDBC"),p)){c.setAutoCommit(false);try(var s=c.createStatement()){s.execute("SET TRANSACTION READ ONLY");}var out=J.createObjectNode();
   record(c,out,"columns","SELECT table_name,column_name,data_type FROM user_tab_columns WHERE table_name IN ('AN_STOCK_DAILY_CANDLE','AN_STOCK_DAILY_PRICE','UA_SCAN_ITEM','UA_SCAN_RUN','MD_TRADING_CALENDAR') ORDER BY table_name,column_id");
   for(String table:List.of("AN_STOCK_DAILY_CANDLE","AN_STOCK_DAILY_PRICE","MD_STOCK_DAILY_PRICE","AN_STOCK_TIMEFRAME_CANDLE","MD_INDUSTRY_DAILY_PRICE","AN_INDUSTRY_DAILY_CANDLE")){
    record(c,out,table+"_range","SELECT COUNT(*) row_count,COUNT(DISTINCT business_date) dates,TO_CHAR(MIN(business_date),'YYYY-MM-DD') earliest,TO_CHAR(MAX(business_date),'YYYY-MM-DD') latest,TO_CHAR(MIN(observed_at),'YYYY-MM-DD HH24:MI:SS TZH:TZM') earliest_observed,TO_CHAR(MAX(observed_at),'YYYY-MM-DD HH24:MI:SS TZH:TZM') latest_observed FROM "+table+" WHERE business_date<=DATE '2026-11-18' AND observed_at<"+CUT);
   }
   record(c,out,"daily_candle_stock_coverage","SELECT stock_code,price_variant,COUNT(*) rows_total,COUNT(DISTINCT business_date) dates,TO_CHAR(MIN(business_date),'YYYY-MM-DD') earliest,TO_CHAR(MAX(business_date),'YYYY-MM-DD') latest,TO_CHAR(MIN(observed_at),'YYYY-MM-DD HH24:MI:SS TZH:TZM') first_observed,TO_CHAR(MAX(observed_at),'YYYY-MM-DD HH24:MI:SS TZH:TZM') last_observed FROM an_stock_daily_candle WHERE business_date<=DATE '2026-11-18' AND observed_at<"+CUT+" GROUP BY stock_code,price_variant ORDER BY stock_code,price_variant");
   record(c,out,"legacy_scan_price_dates","SELECT TO_CHAR(r.base_date,'YYYY-MM-DD') run_date,TO_CHAR(i.price_date,'YYYY-MM-DD') price_date,COUNT(*) rows_total,SUM(CASE WHEN i.result_json IS NOT NULL THEN 1 ELSE 0 END) inline_results,SUM(CASE WHEN i.result_snapshot_id IS NOT NULL THEN 1 ELSE 0 END) stored_results FROM ua_scan_run r JOIN ua_scan_item i ON i.run_id=r.run_id WHERE r.base_date<DATE '2026-10-16' AND i.status='COMPLETED' GROUP BY r.base_date,i.price_date ORDER BY r.base_date,i.price_date");
   record(c,out,"trading_calendar_count","SELECT COUNT(*) rows_total,TO_CHAR(MIN(calendar_date),'YYYY-MM-DD') earliest,TO_CHAR(MAX(calendar_date),'YYYY-MM-DD') latest FROM md_trading_calendar");
   var samples=out.putArray("legacy_samples");
   try(var s=c.createStatement()){s.setQueryTimeout(90);try(var r=s.executeQuery("SELECT r.run_id,TO_CHAR(r.base_date,'YYYY-MM-DD'),i.stock_code,i.result_json FROM ua_scan_run r JOIN ua_scan_item i ON i.run_id=r.run_id WHERE r.base_date<DATE '2026-10-16' AND i.stock_code='005930' AND i.result_json IS NOT NULL ORDER BY r.base_date FETCH FIRST 24 ROWS ONLY")){
    while(r.next()){var x=samples.addObject();x.put("run",r.getString(1));x.put("date",r.getString(2));x.put("code",r.getString(3));var root=J.readTree(r.getString(4));var keys=x.putArray("keys");root.fieldNames().forEachRemaining(keys::add);x.put("priceHistoryCount",root.path("priceHistory").size());var hist=root.path("priceHistory");if(hist.size()>0){x.set("firstBar",hist.get(0));x.set("lastBar",hist.get(hist.size()-1));}x.set("resultFields",root.path("result").isMissingNode()?root:root.path("result"));}
   }}
   J.writerWithDefaultPrettyPrinter().writeValue(OUT.resolve("db-history-details.json").toFile(),out);System.out.println("HISTORY_DETAILS_COMPLETE legacy_samples="+samples.size());c.rollback();
  }catch(SQLException e){System.out.println("SQL_ERROR_CODE="+e.getErrorCode());System.exit(1);}
 }
}
