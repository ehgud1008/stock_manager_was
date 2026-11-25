import java.sql.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.example.stockanalysis.storage.SnapshotChunks;
import com.example.stockanalysis.unified.eod.EodSnapshotCodec;

/** Read-only provenance-preserving historical price export. No trading/account data. */
public class ExportExtendedHistories {
 static final ObjectMapper J=new ObjectMapper();static final Path OUT=Path.of("tools/swing-expanded-review-20261118");
 static final long CUT=1795014000000L;static final ZoneId KST=ZoneId.of("Asia/Seoul");
 static final String TCUT="TO_TIMESTAMP_TZ('2026-11-19 00:00:00 Asia/Seoul','YYYY-MM-DD HH24:MI:SS TZR')";
 static final LinkedHashMap<String,String> CACHE=new LinkedHashMap<>(16384,.75f,true);static long chars=0,objects=0;
 static String marks(int n){return String.join(",",Collections.nCopies(n,"?"));}
 static void cache(String id,String s){String old=CACHE.put(id,s);chars+=s.length()-(old==null?0:old.length());}
 static void trim(){while(chars>80_000_000){var it=CACHE.entrySet().iterator();var x=it.next();chars-=x.getValue().length();it.remove();}}
 static ArrayNode query(Connection c,String sql,Object...args)throws Exception{var a=J.createArrayNode();try(var s=c.prepareStatement(sql)){s.setQueryTimeout(90);s.setFetchSize(500);for(int i=0;i<args.length;i++)s.setObject(i+1,args[i]);try(var r=s.executeQuery()){var m=r.getMetaData();while(r.next()){var x=a.addObject();for(int i=1;i<=m.getColumnCount();i++){String k=m.getColumnLabel(i).toLowerCase();Object v=r.getObject(i);if(v==null)x.putNull(k);else if(v instanceof Number n)x.set(k,J.valueToTree(n));else x.put(k,r.getString(i));}}}}return a;}
 static void save(String f,JsonNode x)throws Exception{J.writerWithDefaultPrettyPrinter().writeValue(OUT.resolve(f).toFile(),x);}
 static void load(Connection c,Map<String,Integer> refs)throws Exception{
  var missing=refs.keySet().stream().filter(x->!CACHE.containsKey(x)).toList();
  for(int at=0;at<missing.size();at+=200){var ids=missing.subList(at,Math.min(at+200,missing.size()));
   try(var s=c.prepareStatement("SELECT sha256,utf8_bytes,payload_gzip FROM ua_snapshot_object WHERE sha256 IN ("+marks(ids.size())+")")){s.setQueryTimeout(90);s.setFetchSize(100);if(s instanceof oracle.jdbc.OracleStatement os)os.setLobPrefetchSize(65536);for(int i=0;i<ids.size();i++)s.setString(i+1,ids.get(i));try(var r=s.executeQuery()){while(r.next()){String id=r.getString(1);byte[] gzip=r.getBytes(3);if(gzip==null)continue;if(r.getInt(2)!=refs.get(id))throw new IllegalStateException("OBJECT_SIZE_MISMATCH");cache(id,EodSnapshotCodec.decode(gzip,id,r.getInt(2)));objects++;}}}
  }
 }
 static Map<String,String> restore(Connection c,List<ObjectNode> batch)throws Exception{
  var ids=batch.stream().map(x->x.path("input_snapshot_id").asText("")).filter(x->!x.isBlank()).distinct().toList();var refs=new LinkedHashMap<String,Integer>();var headers=new HashMap<String,JsonNode>();
  if(!ids.isEmpty())for(var h:query(c,"SELECT sha256,manifest_id,manifest_utf8_bytes,utf8_bytes FROM ua_snapshot_document WHERE sha256 IN ("+marks(ids.size())+")",ids.toArray())){headers.put(h.path("sha256").asText(),h);refs.put(h.path("manifest_id").asText(),h.path("manifest_utf8_bytes").asInt());}
  load(c,refs);refs.clear();var manifests=new HashMap<String,SnapshotChunks.Manifest>();
  for(var e:headers.entrySet()){String raw=CACHE.get(e.getValue().path("manifest_id").asText());if(raw==null)continue;var m=J.readValue(raw,SnapshotChunks.Manifest.class);if(!m.sha256().equals(e.getKey())||m.utf8Bytes()!=e.getValue().path("utf8_bytes").asInt())throw new IllegalStateException("MANIFEST_MISMATCH");manifests.put(e.getKey(),m);for(var part:m.parts())if(part.objectId()!=null)refs.put(part.objectId(),part.objectBytes());}
  load(c,refs);var output=new HashMap<String,String>();
  for(var row:batch){String id=row.path("input_snapshot_id").asText("");if(!id.isBlank()){var m=manifests.get(id);if(m!=null)output.put(row.path("run_id").asText()+"/"+row.path("stock_code").asText(),new SnapshotChunks(J).restore(m,CACHE));}}
  var inline=batch.stream().filter(x->x.path("input_snapshot_id").asText("").isBlank()).toList();
  if(!inline.isEmpty())try(var s=c.prepareStatement("SELECT run_id,stock_code,input_json,input_gzip,input_sha256,input_utf8_bytes FROM ua_eod_item WHERE (run_id,stock_code) IN ("+String.join(",",Collections.nCopies(inline.size(),"(?,?)"))+")")){s.setQueryTimeout(90);s.setFetchSize(50);if(s instanceof oracle.jdbc.OracleStatement os)os.setLobPrefetchSize(65536);int n=1;for(var x:inline){s.setString(n++,x.path("run_id").asText());s.setString(n++,x.path("stock_code").asText());}try(var r=s.executeQuery()){while(r.next())output.put(r.getString(1)+"/"+r.getString(2),EodSnapshotCodec.read(r));}}
  trim();return output;
 }
 static ArrayNode bars(JsonNode input,long observed){
  var sorted=new TreeMap<String,JsonNode>();for(var b:input){String d=b.path("businessDate").asText();if(d.length()!=10||d.compareTo("2026-11-18")>0)continue;
   // Do not treat an intraday snapshot's current daily candle as a finalized close.
   long close=LocalDate.parse(d).atTime(15,30).atZone(KST).toInstant().toEpochMilli();if(observed<close)continue;sorted.put(d,b);
  }
  var a=J.createArrayNode();for(var e:sorted.entrySet()){var b=e.getValue();var x=a.addArray();x.add(e.getKey());for(String k:List.of("open","high","low","close","volume"))x.add(b.path(k));}return a;
 }
 static List<ObjectNode> eodRows(Connection c,boolean probe)throws Exception{
  String sql="SELECT * FROM (SELECT r.run_id,TO_CHAR(r.base_date,'YYYY-MM-DD') base_date,i.stock_code,s.stock_name,i.input_snapshot_id,i.collected_epoch,i.analyzed_epoch,e.price_basis,ROW_NUMBER() OVER(PARTITION BY i.stock_code ORDER BY r.base_date,i.analyzed_epoch) first_rank,ROW_NUMBER() OVER(PARTITION BY i.stock_code ORDER BY r.base_date DESC,i.analyzed_epoch DESC) last_rank FROM ua_eod_item i JOIN ua_scan_run r ON r.run_id=i.run_id JOIN ua_scan_item s ON s.run_id=i.run_id AND s.stock_code=i.stock_code JOIN ua_eod_run e ON e.run_id=i.run_id WHERE r.base_date<=DATE '2026-11-18' AND s.status='COMPLETED' AND i.collected_epoch>0 AND i.collected_epoch<? AND i.analyzed_epoch>0 AND i.analyzed_epoch<? AND (i.input_snapshot_id IS NOT NULL OR i.input_json IS NOT NULL OR i.input_gzip IS NOT NULL)) WHERE "+(probe?"stock_code='005930' AND last_rank=1":"first_rank=1 OR last_rank=1 OR base_date<'2026-10-16'")+" ORDER BY base_date,stock_code";
  var a=new ArrayList<ObjectNode>();for(var r:query(c,sql,CUT,CUT))a.add((ObjectNode)r);return a;
 }
 static void eod(Connection c,boolean probe)throws Exception{
  var rows=eodRows(c,probe);if(probe){var raw=restore(c,rows);for(var row:rows){var root=J.readTree(raw.get(row.path("run_id").asText()+"/"+row.path("stock_code").asText()));var a=root.path("input").path("dailyCandles");var output=row.deepCopy();output.put("dailyCount",a.size());output.set("firstBar",a.size()>0?a.get(0):J.nullNode());output.set("lastBar",a.size()>0?a.get(a.size()-1):J.nullNode());save("eod-input-probe.json",output);System.out.println("EOD_INPUT_PROBE="+output);}return;}
  save("eod-history-selection.json",J.valueToTree(rows));var failures=J.createArrayNode();Path dest=OUT.resolve("eod-full-histories.jsonl");if(Files.exists(dest)){System.out.println("RESUME_EOD_FILE");return;}
  Path tmp=dest.resolveSibling(dest.getFileName()+".partial");try(var writer=Files.newBufferedWriter(tmp)){
   for(int at=0;at<rows.size();at+=50){var batch=rows.subList(at,Math.min(at+50,rows.size()));var values=restore(c,batch);
    for(var row:batch){String key=row.path("run_id").asText()+"/"+row.path("stock_code").asText();try{
     String raw=values.get(key);if(raw==null)throw new IllegalStateException("SOURCE_UNAVAILABLE");var root=J.readTree(raw);var input=root.path("input");
     if(!input.path("stock").path("stockCode").asText().equals(row.path("stock_code").asText()))throw new IllegalStateException("IDENTITY_MISMATCH");
     var out=row.deepCopy();out.put("id","EOD/"+key);out.put("kind","EOD_INPUT");out.put("available_epoch",Math.max(row.path("collected_epoch").asLong(),row.path("analyzed_epoch").asLong()));out.put("verified",true);out.set("bars",bars(input.path("dailyCandles"),row.path("collected_epoch").asLong()));if(out.path("bars").isEmpty())throw new IllegalStateException("EMPTY_DAILY_HISTORY");writer.write(J.writeValueAsString(out));writer.newLine();
    }catch(Exception ex){var x=failures.addObject();x.put("key",key);x.put("type",ex.getClass().getSimpleName());}}
    if(at%500==0){writer.flush();System.out.println("EOD_PROGRESS="+(at+batch.size())+"/"+rows.size()+" objects="+objects);}
   }
  }Files.move(tmp,dest);save("eod-export-failures.json",failures);System.out.println("EOD_COMPLETE selected="+rows.size()+" failures="+failures.size());
 }
 static void legacy(Connection c)throws Exception{
  Path dest=OUT.resolve("legacy-full-histories.jsonl");if(Files.exists(dest)){System.out.println("RESUME_LEGACY_FILE");return;}Path tmp=dest.resolveSibling(dest.getFileName()+".partial");int count=0;
  String sql="SELECT r.run_id,TO_CHAR(r.base_date,'YYYY-MM-DD') base_date,r.created_epoch,r.completed_epoch,i.stock_code,i.stock_name,TO_CHAR(i.price_date,'YYYY-MM-DD') price_date,i.result_json FROM ua_scan_run r JOIN ua_scan_item i ON i.run_id=r.run_id WHERE r.base_date<DATE '2026-10-14' AND r.completed_epoch>0 AND r.completed_epoch<? AND i.status='COMPLETED' AND i.result_json IS NOT NULL AND JSON_EXISTS(i.result_json,'$.priceHistory[0]') ORDER BY r.base_date,i.stock_code";
  try(var writer=Files.newBufferedWriter(tmp);var s=c.prepareStatement(sql)){s.setQueryTimeout(120);s.setFetchSize(100);if(s instanceof oracle.jdbc.OracleStatement os)os.setLobPrefetchSize(65536);s.setLong(1,CUT);try(var r=s.executeQuery()){while(r.next()){
   var root=J.readTree(r.getString(8));var x=J.createObjectNode();x.put("id","LEGACY/"+r.getString(1)+"/"+r.getString(5));x.put("kind","LEGACY_RESULT");x.put("run_id",r.getString(1));x.put("base_date",r.getString(2));x.put("stock_code",r.getString(5));x.put("stock_name",r.getString(6));x.put("price_date",r.getString(7));x.put("available_epoch",r.getLong(4));x.put("observed_bound","run_completed_epoch");x.put("verified",root.path("result").path("stock").path("stockCode").asText().equals(r.getString(5)));x.set("bars",bars(root.path("priceHistory"),r.getLong(4)));writer.write(J.writeValueAsString(x));writer.newLine();count++;if(count%2000==0){writer.flush();System.out.println("LEGACY_PROGRESS="+count);}
  }}}Files.move(tmp,dest);System.out.println("LEGACY_COMPLETE="+count);
 }
 static void candles(Connection c)throws Exception{
  Path dest=OUT.resolve("table-candle-histories.jsonl");if(Files.exists(dest)){System.out.println("RESUME_CANDLE_FILE");return;}Path tmp=dest.resolveSibling(dest.getFileName()+".partial");long count=0;int groups=0;
  String sql="SELECT tr_response_id,stock_code,price_variant,TO_CHAR(business_date,'YYYY-MM-DD') business_date,open_price_krw,high_price_krw,low_price_krw,close_price_krw,volume,TO_CHAR(observed_at AT TIME ZONE 'UTC','YYYY-MM-DD\"T\"HH24:MI:SS.FF3\"Z\"') observed_utc,TO_CHAR(created_at AT TIME ZONE 'UTC','YYYY-MM-DD\"T\"HH24:MI:SS.FF3\"Z\"') created_utc FROM an_stock_daily_candle WHERE business_date<=DATE '2026-11-18' AND observed_at<"+TCUT+" AND created_at<"+TCUT+" ORDER BY tr_response_id,stock_code,price_variant,business_date";
  try(var writer=Files.newBufferedWriter(tmp);var s=c.createStatement()){s.setQueryTimeout(120);s.setFetchSize(4000);try(var r=s.executeQuery(sql)){ObjectNode current=null;String old="";while(r.next()){
   String key=r.getString(1)+"/"+r.getString(2)+"/"+r.getString(3);long observed=Instant.parse(r.getString(10)).toEpochMilli(),created=Instant.parse(r.getString(11)).toEpochMilli();count++;
   if(!key.equals(old)){if(current!=null){writer.write(J.writeValueAsString(current));writer.newLine();groups++;}current=J.createObjectNode();current.put("id","CANDLE/"+key);current.put("kind","AN_DAILY_CANDLE");current.put("stock_code",r.getString(2));current.put("price_variant",r.getString(3));current.put("available_epoch",Math.max(observed,created));current.put("observed_epoch",observed);current.put("verified",true);current.putArray("bars");old=key;}
   current.put("available_epoch",Math.max(current.path("available_epoch").asLong(),Math.max(observed,created)));
   String date=r.getString(4);if(observed<LocalDate.parse(date).atTime(15,30).atZone(KST).toInstant().toEpochMilli())continue;
   var b=((ArrayNode)current.path("bars")).addArray();b.add(date);for(int k=5;k<=9;k++){Object v=r.getObject(k);b.add(v==null?J.nullNode():J.valueToTree(v));}
  }if(current!=null){writer.write(J.writeValueAsString(current));writer.newLine();groups++;}}}
  Files.move(tmp,dest);System.out.println("CANDLE_COMPLETE rows="+count+" sources="+groups);
 }
 public static void main(String[] args)throws Exception{
  Files.createDirectories(OUT);var p=new Properties();p.setProperty("user",System.getenv("TASK_REPORT_USER"));p.setProperty("password",System.getenv("TASK_REPORT_PASSWORD"));p.setProperty("oracle.net.CONNECT_TIMEOUT","15000");p.setProperty("oracle.jdbc.ReadTimeout","180000");
  try(var c=DriverManager.getConnection(System.getenv("TASK_REPORT_JDBC"),p)){c.setAutoCommit(false);try(var s=c.createStatement()){s.execute("SET TRANSACTION READ ONLY");}
   if(args.length>0&&args[0].equals("probe"))eod(c,true);else {candles(c);legacy(c);eod(c,false);}c.rollback();System.out.println("EXPORT_FINISHED DB_WRITES=0");
  }catch(SQLException e){System.out.println("SQL_ERROR_CODE="+e.getErrorCode());System.exit(1);}
 }
}
