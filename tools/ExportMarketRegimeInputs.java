import java.sql.*;
import java.nio.file.*;
import java.util.*;
import java.time.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

public class ExportMarketRegimeInputs {
 static final ObjectMapper J=new ObjectMapper();
 static final Path OUT=Path.of("tools/swing-market-review-20261118");
 static final long CUT=1795014000000L;
 static long epoch(JsonNode n){if(n.isNumber()){double v=n.asDouble();return (long)(v<1e11?v*1000:v);}try{return Instant.parse(n.asText()).toEpochMilli();}catch(Exception e){return -1;}}
 static ArrayNode query(Connection c,String sql)throws Exception{var a=J.createArrayNode();try(var s=c.prepareStatement(sql)){s.setQueryTimeout(120);s.setFetchSize(500);try(var r=s.executeQuery()){var m=r.getMetaData();while(r.next()){var o=a.addObject();for(int i=1;i<=m.getColumnCount();i++){String k=m.getColumnLabel(i).toLowerCase();Object v=r.getObject(i);if(v==null)o.putNull(k);else if(v instanceof Number)o.set(k,J.valueToTree(v));else o.put(k,r.getString(i));}}}}return a;}
 public static void main(String[] args)throws Exception{
  Files.createDirectories(OUT);var props=new Properties();props.setProperty("user",System.getenv("TASK_REPORT_USER"));props.setProperty("password",System.getenv("TASK_REPORT_PASSWORD"));props.setProperty("oracle.net.CONNECT_TIMEOUT","15000");props.setProperty("oracle.jdbc.ReadTimeout","150000");
  try(var c=DriverManager.getConnection(System.getenv("TASK_REPORT_JDBC"),props)){c.setAutoCommit(false);try(var s=c.createStatement()){s.execute("SET TRANSACTION READ ONLY");}
   var meta=J.createObjectNode();meta.put("cutoff","2026-11-18");meta.put("databaseWrites",0);meta.put("queriedAt",Instant.now().toString());
   var membership=query(c,"SELECT i.stock_code,i.market,TO_CHAR(MIN(r.base_date),'YYYY-MM-DD') first_date,TO_CHAR(MAX(r.base_date),'YYYY-MM-DD') last_date,COUNT(*) observations FROM ua_scan_item i JOIN ua_scan_run r ON r.run_id=i.run_id WHERE r.base_date<=DATE '2026-11-18' AND r.completed_epoch>0 AND r.completed_epoch<1795014000000 AND i.status='COMPLETED' GROUP BY i.stock_code,i.market ORDER BY i.stock_code,i.market");
   J.writerWithDefaultPrettyPrinter().writeValue(OUT.resolve("stock-market-membership.json").toFile(),membership);meta.put("membershipRows",membership.size());
   int accepted=0,rejected=0;var summaries=J.createArrayNode();
   try(var writer=Files.newBufferedWriter(OUT.resolve("saved-market-snapshots.jsonl"));var s=c.prepareStatement("SELECT d.run_id,TO_CHAR(r.base_date,'YYYY-MM-DD') base_date,d.created_epoch,d.updated_epoch,d.market_json,d.report_json FROM ua_daily_report d JOIN ua_scan_run r ON r.run_id=d.run_id WHERE r.base_date<=DATE '2026-11-18' AND d.created_epoch<1795014000000 AND d.market_json IS NOT NULL ORDER BY r.base_date,d.created_epoch")){s.setQueryTimeout(120);try(var rs=s.executeQuery()){while(rs.next()){
    var market=J.readTree(rs.getString("market_json"));long available=epoch(market.path("collectedAt"));if(available<=0||available>=CUT){rejected++;continue;}
    var indices=market.path("indices");boolean valid=indices.isArray()&&indices.size()==2;for(var idx:indices){long t=epoch(idx.path("collectedAt"));valid&=t>0&&t<CUT&&idx.path("tradeDate").asText().compareTo("2026-11-18")<=0;}
    if(!valid){rejected++;continue;}var row=J.createObjectNode();row.put("run_id",rs.getString("run_id"));row.put("base_date",rs.getString("base_date"));row.put("created_epoch",rs.getLong("created_epoch"));row.put("updated_epoch",rs.getLong("updated_epoch"));row.put("market_available_epoch",available);row.set("market",market);
    String report=rs.getString("report_json");if(report!=null){var reportNode=J.readTree(report);long finalized=epoch(reportNode.path("finalizedAt"));if(finalized>0&&finalized<CUT)row.set("report",reportNode);}
    writer.write(J.writeValueAsString(row));writer.newLine();accepted++;
    for(var idx:indices){var bars=idx.path("bars");var summary=summaries.addObject();summary.put("base_date",rs.getString("base_date"));summary.put("market",idx.path("market").asText());summary.put("available_epoch",available);summary.put("bars",bars.size());if(bars.size()>0){summary.put("first",bars.get(0).path("date").asText());summary.put("last",bars.get(bars.size()-1).path("date").asText());}}
   }}}
   meta.put("acceptedSnapshots",accepted);meta.put("rejectedSnapshots",rejected);meta.set("indexCoverage",summaries);J.writerWithDefaultPrettyPrinter().writeValue(OUT.resolve("market-extraction-metadata.json").toFile(),meta);c.rollback();System.out.println(J.writerWithDefaultPrettyPrinter().writeValueAsString(meta));
  }catch(SQLException e){System.out.println("SQL_ERROR_CODE="+e.getErrorCode());System.exit(1);}
 }
}
