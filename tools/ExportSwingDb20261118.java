import java.sql.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.example.stockanalysis.storage.SnapshotChunks;
import com.example.stockanalysis.unified.eod.EodSnapshotCodec;

/** Direct Oracle read-only extraction. The source data cutoff is 2026-11-18 KST. */
public class ExportSwingDb20261118 {
    static final ObjectMapper J=new ObjectMapper();
    static final Path OUT=Path.of("tools/swing-db-review-20261118");
    static final ZoneId KST=ZoneId.of("Asia/Seoul");
    static final long CUTOFF=LocalDate.of(2026,11,19).atStartOfDay(KST).toInstant().toEpochMilli();
    static final LinkedHashMap<String,String> CACHE=new LinkedHashMap<>(16384,.75f,true);
    static long cacheChars=0,readObjects=0;
    static void cache(String id,String value){String old=CACHE.put(id,value);cacheChars+=value.length()-(old==null?0:old.length());}
    static void trimCache(){while(cacheChars>80_000_000){var it=CACHE.entrySet().iterator();var e=it.next();cacheChars-=e.getValue().length();it.remove();}}
    static String marks(int n){return String.join(",",Collections.nCopies(n,"?"));}
    static void loadObjects(Connection c,Map<String,Integer> refs)throws Exception {
        var missing=refs.keySet().stream().filter(k->!CACHE.containsKey(k)).toList();
        for(int at=0;at<missing.size();at+=300){
            var ids=missing.subList(at,Math.min(at+300,missing.size()));
            try(var s=c.prepareStatement("SELECT sha256,utf8_bytes,payload_gzip FROM ua_snapshot_object WHERE sha256 IN ("+marks(ids.size())+")")){
                s.setQueryTimeout(90);s.setFetchSize(100);
                if(s instanceof oracle.jdbc.OracleStatement os)os.setLobPrefetchSize(65536);
                for(int i=0;i<ids.size();i++)s.setString(i+1,ids.get(i));
                try(var r=s.executeQuery()){while(r.next()){
                    String id=r.getString(1);byte[] gzip=r.getBytes(3);
                    if(gzip==null)continue;
                    if(r.getInt(2)!=refs.get(id))throw new IllegalStateException("OBJECT_SIZE_MISMATCH");
                    cache(id,EodSnapshotCodec.decode(gzip,id,r.getInt(2)));readObjects++;
                }}
            }
        }
    }
    static void exportBatch(Connection c,List<ObjectNode> batch,java.io.BufferedWriter writer,ArrayNode failures)throws Exception {
        var ids=batch.stream().map(r->r.path("result_snapshot_id").asText("")).filter(s->!s.isBlank()).distinct().toList();
        var inlineRows=batch.stream().filter(r->r.path("result_snapshot_id").asText("").isBlank()).toList();
        var inlines=new HashMap<String,String>();
        if(!inlineRows.isEmpty())try(var s=c.prepareStatement("SELECT run_id,stock_code,result_json FROM ua_scan_item WHERE (run_id,stock_code) IN ("+String.join(",",Collections.nCopies(inlineRows.size(),"(?,?)"))+")")){
            s.setQueryTimeout(90);s.setFetchSize(100);
            if(s instanceof oracle.jdbc.OracleStatement os)os.setLobPrefetchSize(65536);
            int n=1;for(var row:inlineRows){s.setString(n++,row.path("run_id").asText());s.setString(n++,row.path("stock_code").asText());}
            try(var r=s.executeQuery()){while(r.next())inlines.put(r.getString(1)+"/"+r.getString(2),r.getString(3));}
        }
        var headers=new HashMap<String,JsonNode>();var refs=new LinkedHashMap<String,Integer>();
        if(!ids.isEmpty())for(var h:query(c,"SELECT sha256,manifest_id,manifest_utf8_bytes,utf8_bytes FROM ua_snapshot_document WHERE sha256 IN ("+marks(ids.size())+")",ids.toArray())){
            headers.put(h.path("sha256").asText(),h);refs.put(h.path("manifest_id").asText(),h.path("manifest_utf8_bytes").asInt());
        }
        loadObjects(c,refs);
        var manifests=new HashMap<String,SnapshotChunks.Manifest>();refs.clear();
        for(var entry:headers.entrySet()){
            String raw=CACHE.get(entry.getValue().path("manifest_id").asText());if(raw==null)continue;
            var m=J.readValue(raw,SnapshotChunks.Manifest.class);
            if(!m.sha256().equals(entry.getKey())||m.utf8Bytes()!=entry.getValue().path("utf8_bytes").asInt())throw new IllegalStateException("MANIFEST_MISMATCH");
            manifests.put(entry.getKey(),m);for(var part:m.parts())if(part.objectId()!=null)refs.put(part.objectId(),part.objectBytes());
        }
        loadObjects(c,refs);
        for(var row:batch){
            String id=row.path("result_snapshot_id").asText("");
            try{
                String raw;
                if(!id.isBlank()){
                    var m=manifests.get(id);if(m==null)throw new IllegalStateException("MANIFEST_NOT_IN_DB");
                    raw=new SnapshotChunks(J).restore(m,CACHE);
                }else{
                    raw=inlines.get(row.path("run_id").asText()+"/"+row.path("stock_code").asText());
                    if(raw==null)throw new IllegalStateException("INLINE_MISSING");
                }
                var root=J.readTree(raw);var result=root.path("result");
                if(!row.path("stock_code").asText().equals(result.path("stock").path("stockCode").asText())||!row.path("base_date").asText().equals(result.path("priceDate").asText()))throw new IllegalStateException("IDENTITY_MISMATCH");
                var out=row.deepCopy();out.put("verified",true);out.put("engine",result.path("analysis").path("engineVersion").asText());
                out.set("quality",root.path("collectionQuality"));out.set("warnings",root.path("collectionWarnings"));
                var factors=out.putArray("factors");for(var f:result.path("analysis").path("factors")){
                    var a=factors.addArray();a.add(f.path("factorCode").asText());a.add(f.path("rawValue"));a.add(f.path("score"));
                }
                var history=new ArrayList<JsonNode>();
                for(var b:root.path("priceHistory"))if(b.path("businessDate").asText().compareTo(row.path("base_date").asText())<=0)history.add(b);
                history.sort(Comparator.comparing(b->b.path("businessDate").asText()));
                var bars=out.putArray("bars");
                for(var b:history.subList(Math.max(0,history.size()-66),history.size())){
                    var a=bars.addArray();a.add(b.path("businessDate").asText());for(String field:List.of("open","high","low","close","volume"))a.add(b.path(field));
                }
                if(bars.isEmpty()||!bars.get(bars.size()-1).get(0).asText().equals(row.path("base_date").asText()))throw new IllegalStateException("HISTORY_DATE_MISMATCH");
                if(Math.abs(bars.get(bars.size()-1).get(4).asDouble()-row.path("current_price").asDouble())>.00001||bars.get(bars.size()-1).get(5).asLong()!=row.path("volume").asLong())throw new IllegalStateException("HISTORY_PRICE_MISMATCH");
                writer.write(J.writeValueAsString(out));writer.newLine();
            }catch(Exception ex){var f=failures.addObject();f.put("date",row.path("base_date").asText());f.put("code",row.path("stock_code").asText());f.put("type",ex.getClass().getSimpleName());f.put("reason",ex.getMessage()==null?"":ex.getMessage().substring(0,Math.min(100,ex.getMessage().length())));}
        }
        trimCache();
    }
    static long nextOpen(String date){var d=LocalDate.parse(date).plusDays(1);while(d.getDayOfWeek()==DayOfWeek.SATURDAY||d.getDayOfWeek()==DayOfWeek.SUNDAY)d=d.plusDays(1);return d.atTime(9,0).atZone(KST).toInstant().toEpochMilli();}
    static boolean timely(JsonNode r){long t=r.path("analyzed_epoch").asLong(0);return t>0&&t<nextOpen(r.path("base_date").asText());}
    static void export(Connection c)throws Exception{
        var all=query(c,"SELECT r.run_id,TO_CHAR(r.base_date,'YYYY-MM-DD') base_date,r.created_epoch,r.completed_epoch,i.stock_code,i.stock_name,i.status,TO_CHAR(i.price_date,'YYYY-MM-DD') price_date,i.current_price,i.volume,i.total_score,i.result_snapshot_id,e.analyzed_epoch,e.collected_epoch FROM ua_scan_run r JOIN ua_scan_item i ON i.run_id=r.run_id LEFT JOIN ua_eod_item e ON e.run_id=i.run_id AND e.stock_code=i.stock_code WHERE r.base_date BETWEEN DATE '2026-10-16' AND DATE '2026-11-18' AND r.created_epoch<? ORDER BY r.base_date,i.stock_code,r.created_epoch",CUTOFF);
        save("db-all-items.json",all);
        var chosen=new TreeMap<String,ObjectNode>();var universe=new TreeMap<String,String>();
        for(var row:all){
            universe.put(row.path("stock_code").asText(),row.path("stock_name").asText());
            if(!row.path("status").asText().equals("COMPLETED")||!row.path("base_date").equals(row.path("price_date"))||row.path("current_price").asDouble()<=0||row.path("volume").asLong()<=0||row.path("analyzed_epoch").asLong(0)<=0||row.path("analyzed_epoch").asLong()>=CUTOFF||row.path("collected_epoch").asLong()>=CUTOFF)continue;
            String k=row.path("base_date").asText()+"/"+row.path("stock_code").asText();var old=chosen.get(k);
            if(old==null||(timely(row)&&!timely(old))||(timely(row)==timely(old)&&row.path("analyzed_epoch").asLong()>old.path("analyzed_epoch").asLong()))chosen.put(k,(ObjectNode)row);
        }
        save("universe.json",J.valueToTree(universe));
        var selected=J.createArrayNode();var byDate=new TreeMap<String,List<ObjectNode>>();
        for(var row:chosen.values()){row.put("signal_available",timely(row));selected.add(row);byDate.computeIfAbsent(row.path("base_date").asText(),k->new ArrayList<>()).add(row);}
        save("observations.json",selected);var failures=Files.exists(OUT.resolve("extraction-failures.json"))?(ArrayNode)J.readTree(OUT.resolve("extraction-failures.json").toFile()):J.createArrayNode();
        Files.createDirectories(OUT.resolve("snapshots"));
        for(var entry:byDate.entrySet()){
            Path target=OUT.resolve("snapshots/"+entry.getKey()+".jsonl");
            if(Files.exists(target)&&Files.size(target)>0){System.out.println("RESUME_DAY="+entry.getKey());continue;}
            for(int n=failures.size()-1;n>=0;n--)if(failures.get(n).path("date").asText().equals(entry.getKey()))failures.remove(n);
            Path tmp=target.resolveSibling(target.getFileName()+".partial");
            try(var writer=Files.newBufferedWriter(tmp,java.nio.charset.StandardCharsets.UTF_8)){
                var rows=entry.getValue();for(int at=0;at<rows.size();at+=200)exportBatch(c,rows.subList(at,Math.min(at+200,rows.size())),writer,failures);
            }
            Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING);
            save("extraction-failures.json",failures);
            System.out.println("EXPORTED_DAY="+entry.getKey()+" selected="+entry.getValue().size()+" failures_total="+failures.size()+" objects_read="+readObjects);
        }
        save("extraction-failures.json",failures);
        var metadata=J.createObjectNode();metadata.put("source","Oracle direct JDBC read-only");metadata.put("start","2026-10-16");metadata.put("end","2026-11-18");metadata.put("cutoffEpochMillis",CUTOFF);metadata.put("exportedAt",Instant.now().toString());metadata.put("universe",universe.size());metadata.put("selectedObservations",chosen.size());metadata.put("failures",failures.size());metadata.put("databaseWrites",0);save("extraction-metadata.json",metadata);
        System.out.println("EXPORT_COMPLETE="+metadata);
    }
    static ArrayNode query(Connection c,String sql,Object...params)throws Exception {
        var out=J.createArrayNode();
        try(var s=c.prepareStatement(sql)){
            s.setQueryTimeout(90);s.setFetchSize(500);
            for(int i=0;i<params.length;i++)s.setObject(i+1,params[i]);
            try(var r=s.executeQuery()){
                var m=r.getMetaData();
                while(r.next()){
                    var row=out.addObject();
                    for(int i=1;i<=m.getColumnCount();i++){
                        String k=m.getColumnLabel(i).toLowerCase(Locale.ROOT);
                        Object value=r.getObject(i);
                        if(value==null)row.putNull(k);
                        else if(value instanceof Number n)row.set(k,J.valueToTree(n));
                        else row.put(k,r.getString(i));
                    }
                }
            }
        }
        return out;
    }
    static void save(String file,JsonNode data)throws Exception {J.writerWithDefaultPrettyPrinter().writeValue(OUT.resolve(file).toFile(),data);}
    public static void main(String[] args)throws Exception {
        Files.createDirectories(OUT);
        var p=new Properties();p.setProperty("user",System.getenv("TASK_REPORT_USER"));p.setProperty("password",System.getenv("TASK_REPORT_PASSWORD"));
        p.setProperty("oracle.net.CONNECT_TIMEOUT","15000");p.setProperty("oracle.jdbc.ReadTimeout","120000");
        try(var c=DriverManager.getConnection(System.getenv("TASK_REPORT_JDBC"),p)){
            c.setAutoCommit(false);try(var s=c.createStatement()){s.execute("SET TRANSACTION READ ONLY");}
            System.out.println("READ_ONLY_CONNECTED cutoff_epoch="+CUTOFF);
            var runs=query(c,"SELECT r.run_id,TO_CHAR(r.base_date,'YYYY-MM-DD') base_date,r.status,r.created_epoch,r.completed_epoch,COUNT(i.stock_code) item_count,SUM(CASE WHEN i.status='COMPLETED' THEN 1 ELSE 0 END) completed_count,SUM(CASE WHEN i.status='COMPLETED' AND i.price_date=r.base_date AND i.current_price>0 AND i.volume>0 THEN 1 ELSE 0 END) valid_count,MIN(e.analyzed_epoch) min_analyzed,MAX(e.analyzed_epoch) max_analyzed FROM ua_scan_run r LEFT JOIN ua_scan_item i ON i.run_id=r.run_id LEFT JOIN ua_eod_item e ON e.run_id=i.run_id AND e.stock_code=i.stock_code WHERE r.base_date BETWEEN DATE '2026-10-16' AND DATE '2026-11-18' GROUP BY r.run_id,r.base_date,r.status,r.created_epoch,r.completed_epoch ORDER BY r.base_date,r.created_epoch");
            save("db-runs.json",runs);System.out.println("RUN_COUNT="+runs.size());
            var tables=query(c,"SELECT table_name FROM user_tables WHERE table_name LIKE '%DAILY%' OR table_name LIKE '%CANDLE%' ORDER BY table_name");
            System.out.println("DAILY_TABLES="+tables);
            if(args.length==0||args[0].equals("inventory")){c.rollback();return;}
            export(c);
            c.rollback();
        }catch(SQLException e){System.out.println("SQL_ERROR="+e.getErrorCode()+" STATE="+e.getSQLState());throw new RuntimeException("Database extraction failed (details redacted)");}
    }
}
