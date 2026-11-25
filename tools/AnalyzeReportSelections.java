import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.sql.*;
import java.nio.file.*;
import java.util.*;

/** Audit persisted report decisions only; no regeneration, updates or notifications. */
public class AnalyzeReportSelections {
    static final ObjectMapper JSON=new ObjectMapper();
    static final List<String> REQUIRED=List.of("OPEN_PRICE","AMOUNT_TODAY","AMOUNT_AVERAGE20","BULLISH_STRUCTURE","POSITIVE_RETURN20","SWING_SCORE","SWING_ACTION","NO_SELL_CONFLICT","PRICE_EMA20_DISTANCE","RELATIVE_RETURN20","EMA5_SLOPE_3","EMA20_SLOPE_3","BUY_PATTERN","ENTRY_RANGE","NET_REWARD_RISK","VOLUME_RATIO20");
    static JsonNode evidence(JsonNode row,String code){for(var e:row.path("evidence"))if(code.equals(e.path("code").asText()))return e;return JSON.nullNode();}
    static boolean pass(JsonNode row,String code){return evidence(row,code).path("passed").asBoolean(false);}
    static List<String> failures(JsonNode row){return REQUIRED.stream().filter(code->!pass(row,code)).toList();}
    static ArrayNode query(Connection c,String sql,String id)throws Exception{
        var out=JSON.createArrayNode();try(var s=c.prepareStatement(sql)){s.setQueryTimeout(30);s.setString(1,id);try(var r=s.executeQuery()){var m=r.getMetaData();while(r.next()){var row=out.addObject();for(int i=1;i<=m.getColumnCount();i++)row.put(m.getColumnLabel(i),r.getString(i));}}}return out;
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("Expected trade date YYYY-MM-DD");
        var out=JSON.createObjectNode();var decisions=JSON.createArrayNode();String id;
        try(var c=DriverManager.getConnection(System.getenv("TASK_REPORT_JDBC"),System.getenv("TASK_REPORT_USER"),System.getenv("TASK_REPORT_PASSWORD"))){
            c.setAutoCommit(false);try(var s=c.createStatement()){s.execute("SET TRANSACTION READ ONLY");}
            try(var s=c.prepareStatement("SELECT p.run_id,p.status,p.policy_version,p.policy_json,p.report_json,r.status scan_status,r.error_code FROM ua_daily_report p JOIN ua_scan_run r ON r.run_id=p.run_id WHERE r.base_date=TO_DATE(?,'YYYY-MM-DD') AND r.market='ALL' ORDER BY p.created_epoch DESC FETCH FIRST 1 ROWS ONLY")){
                s.setString(1,args[0]);try(var r=s.executeQuery()){if(!r.next())throw new IllegalStateException("REPORT_NOT_FOUND");id=r.getString(1);out.put("runId",id);out.put("reportStatus",r.getString(2));out.put("policyVersion",r.getString(3));out.set("policy",JSON.readTree(r.getString(4)));out.set("report",JSON.readTree(r.getString(5)));out.put("scanStatus",r.getString(6));out.put("scanError",r.getString(7));}
            }
            out.set("scanCounts",query(c,"SELECT market,status,error_code,COUNT(*) stock_count FROM ua_scan_item WHERE run_id=? GROUP BY market,status,error_code ORDER BY market,status,error_code",id));
            out.set("projectionCounts",query(c,"SELECT stage,quality,screener_quality,entry_status,COUNT(*) stock_count FROM ua_scan_item WHERE run_id=? AND status='COMPLETED' GROUP BY stage,quality,screener_quality,entry_status ORDER BY stock_count DESC",id));
            try(var s=c.prepareStatement("SELECT detail_json FROM ua_candidate_evidence WHERE run_id=? ORDER BY stock_code")){s.setString(1,id);s.setFetchSize(100);try(var r=s.executeQuery()){while(r.next())decisions.add(JSON.readTree(r.getString(1)));}}
            c.rollback();
        }
        var valid=new ArrayList<JsonNode>();var dispositions=new TreeMap<String,Integer>();
        for(var row:decisions){dispositions.merge(row.path("disposition").asText()+"/"+row.path("strategy").asText(),1,Integer::sum);if(!row.path("evidence").isEmpty())valid.add(row);}
        out.set("dispositions",JSON.valueToTree(dispositions));out.put("evidenceRows",decisions.size());out.put("validEvidenceRows",valid.size());
        var conditions=out.putArray("conditions");var funnel=out.putArray("funnel");var remaining=new ArrayList<>(valid);
        for(String code:REQUIRED){var item=conditions.addObject();item.put("code",code);item.put("passed",valid.stream().filter(r->pass(r,code)).count());item.put("unknown",valid.stream().filter(r->evidence(r,code).path("passed").isNull()||evidence(r,code).path("passed").isMissingNode()).count());item.put("soleFailure",valid.stream().filter(r->failures(r).equals(List.of(code))).count());remaining.removeIf(r->!pass(r,code));funnel.addObject().put("through",code).put("remaining",remaining.size());}
        out.put("allGatesPass",valid.stream().filter(r->{for(var e:r.path("evidence"))if(e.path("role").asText().equals("GATE")&&!e.path("passed").asBoolean(false))return false;return true;}).count());
        valid.sort(Comparator.<JsonNode>comparingInt(r->failures(r).size()).thenComparing(Comparator.comparingDouble((JsonNode r)->evidence(r,"SWING_SCORE").path("value").asDouble()).reversed()));
        var nearest=out.putArray("nearest");for(var row:valid.subList(0,Math.min(15,valid.size()))){var item=nearest.addObject();for(String key:List.of("code","name","market","stage","strategy","netRewardRisk","relativeReturn20","liquidity"))item.set(key,row.path(key));item.set("failed",JSON.valueToTree(failures(row)));var values=item.putObject("values");for(String code:REQUIRED)values.set(code,evidence(row,code).path("value"));}
        out.set("decisions",decisions);
        Path file=Path.of("tools","report-selection-"+args[0]+".json");JSON.writerWithDefaultPrettyPrinter().writeValue(file.toFile(),out);
        out.remove("decisions");out.remove("projectionCounts");System.out.println(JSON.writerWithDefaultPrettyPrinter().writeValueAsString(out));
        System.out.println("SAVED="+file);
    }
}
