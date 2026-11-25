import com.example.stockanalysis.unified.application.UnifiedAnalysisResponse;
import com.example.stockanalysis.unified.report.*;
import com.example.stockanalysis.unified.report.ReportModels.*;
import com.example.stockanalysis.judgment.*;
import com.example.stockanalysis.engine.model.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Read-only replay from saved API responses. Unchanged gates use the original saved evidence.
 * Never infers COMPLETE from PARTIAL, never reconstructs missing raw source observations. */
class ReplayPublicSelection {
  static final ObjectMapper JSON=new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  static final MathContext MC=MathContext.DECIMAL128;
  static final List<String> GATES=List.of("OPEN_PRICE","BULLISH_STRUCTURE","POSITIVE_RETURN20","PRICE_EMA20_DISTANCE","AMOUNT_TODAY","AMOUNT_AVERAGE20","NO_SELL_CONFLICT","EMA5_SLOPE_3","EMA20_SLOPE_3");
  public static void main(String[] args)throws Exception {
    var dir=Path.of("tools/rerun-20261105");var output=JSON.createObjectNode();var rows=output.putArray("stocks");
    var source=JSON.readTree(Path.of("tools/rerun-20261105-tier-ab.json").toFile()).path("data");
    if(source.path("totalElements").asInt()!=source.path("content").size())throw new IllegalStateException("INCOMPLETE_AB_LIST");
    int operating=0,comparison=0,active=0,waiting=0;
    for(var item:source.path("content")) {
      String code=item.path("stockCode").asText();
      var detail=JSON.readValue(dir.resolve(code+"-detail.json").toFile(),UnifiedAnalysisResponse.class);
      var saved=JSON.readValue(dir.resolve(code+"-evidence.json").toFile(),StockDecision.class);
      var r=detail.result();
      if(!r.priceDate().equals(LocalDate.parse("2026-11-05"))||!r.stock().stockCode().equals(code)||!saved.code().equals(code))throw new IllegalStateException("IDENTITY_MISMATCH");
      var common=new NewEntryEvaluator().evaluate(r.analysis(),r.stock().currentPrice(),r.priceDate(),r.asOf().atZone(ZoneId.of("Asia/Seoul")).toInstant(),detail.collectionQuality().name());
      var signal=common.selectedSignal();var bars=detail.priceHistory().stream().sorted(Comparator.comparing(DailyCandle::businessDate)).toList();
      Boolean volumePass=null;BigDecimal ratio=null;
      if(signal!=null){int event=-1;for(int i=0;i<bars.size();i++)if(bars.get(i).businessDate().equals(signal.occurredOn()))event=i;
        if(event>=20){boolean breakout=signal.type().equals("BREAKOUT");BigDecimal den;
          if(breakout){den=BigDecimal.ZERO;for(int i=event-20;i<event;i++)den=den.add(BigDecimal.valueOf(bars.get(i).volume()));den=den.divide(BigDecimal.valueOf(20),MC);}
          else den=BigDecimal.valueOf(bars.get(event-1).volume());
          if(den.signum()>0){ratio=BigDecimal.valueOf(bars.get(event).volume()).divide(den,MC);volumePass=bars.get(bars.size()-1).volume()>0&&ratio.compareTo(breakout?new BigDecimal("1.2"):BigDecimal.ONE)>=0;}
        }
      }
      var failures=new ArrayList<String>();
      for(var key:GATES){var ev=saved.evidence().stream().filter(e->e.code().equals(key)).toList();if(ev.size()!=1||!Boolean.TRUE.equals(ev.get(0).passed()))failures.add(key);}
      var rr=NewEntryEvaluator.rewardRisk(common.effectiveScenario(),new BigDecimal("40"));
      if(!Boolean.TRUE.equals(volumePass))failures.add(volumePass==null?"PATTERN_VOLUME_UNKNOWN":"PATTERN_VOLUME");
      if(rr==null||rr.compareTo(new BigDecimal("1.5"))<0)failures.add(rr==null?"NET_REWARD_RISK_UNKNOWN":"NET_REWARD_RISK");
      boolean eligible=common.timing().status()==NewEntryAssessment.Status.ELIGIBLE&&failures.isEmpty();
      boolean op=eligible&&common.suitability().tier().equals("A"), b=eligible&&common.suitability().tier().equals("B");
      boolean hasSignal=signal!=null&&common.signals().stream().anyMatch(c->c.signalId().equals(signal.id())&&c.eligible());
      if(op)operating++;if(b)comparison++;if(hasSignal)active++;if(common.timing().signalWaiting())waiting++;
      var row=rows.addObject();row.put("code",code);row.put("name",r.stock().stockName());row.put("score",common.suitability().score());
      row.put("activeSignal",hasSignal);row.put("operating",op);row.put("comparisonB",b);row.put("quality",detail.collectionQuality().name());
      row.set("assessment",JSON.valueToTree(common));row.set("otherFailures",JSON.valueToTree(failures));row.put("patternVolumeRatio",ratio);row.put("netRewardRisk",rr);
    }
    output.put("tradeDate","2026-11-05");output.put("runId",source.path("run").path("runId").asText());output.put("policy",CandidateAnalyzer.VERSION);
    output.put("method","Shared NewEntryEvaluator + original unchanged gate evidence + saved daily volume history. Raw database snapshots unavailable (ACL). No collection, report mutation, quality override or notification.");
    output.put("abEvaluated",rows.size());output.put("operating",operating);output.put("comparisonB",comparison);output.put("activeSignals",active);output.put("signalWaiting",waiting);
    JSON.writerWithDefaultPrettyPrinter().writeValue(Path.of("tools/rerun-20261105-result.json").toFile(),output);
    output.remove("stocks");System.out.println(JSON.writerWithDefaultPrettyPrinter().writeValueAsString(output));
  }
}
