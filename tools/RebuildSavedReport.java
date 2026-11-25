import com.example.stockanalysis.unified.report.*;
import static com.example.stockanalysis.unified.report.ReportModels.*;
import com.example.stockanalysis.unified.scan.*;
import com.example.stockanalysis.unified.application.UnifiedAnalysisResponse;
import com.example.stockanalysis.unified.eod.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.core.io.DefaultResourceLoader;
import java.sql.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Explicit one-off recovery: no scheduler, market collection or Telegram calls. */
public class RebuildSavedReport {
  static final String ID="a87b0eec-5af9-434c-ac9d-b75eb0fd32c6";
  static final ObjectMapper JSON=new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  static class Capture extends ReportStore {
    List<Map<String,Object>> rows=new ArrayList<>();
    Map<String,Path> inputs=new HashMap<>(),details=new HashMap<>();
    DailyReport report;List<StockDecision> evidence;
    Capture(SingleConnectionDataSource ds){super(ds,JSON);}
    public List<Map<String,Object>> stocks(String id){return rows;}
    public JsonNode input(String id,String code){try{return JSON.readTree(inputs.get(code).toFile());}catch(Exception e){throw new IllegalArgumentException("INPUT_INVALID");}}
    public void heartbeat(Job job){}
    public void complete(Job job,DailyReport r,List<StockDecision> e,String error,String message,TelegramProperties t,long deadline){
      if(!"READY".equals(r.status()))throw new IllegalStateException("REBUILD_FAILED:"+error);
      report=r;evidence=e;
    }
  }
  public static void main(String[] args)throws Exception {
    boolean cached=args.length==1&&args[0].equals("--commit-cached");
    boolean commit=cached||(args.length==1&&args[0].equals("--commit"));
    try(var c=DriverManager.getConnection(System.getenv("TASK_REPORT_JDBC"),System.getenv("TASK_REPORT_USER"),System.getenv("TASK_REPORT_PASSWORD"))){
      var ds=new SingleConnectionDataSource(c,true);var store=new Capture(ds);
      String old,policy,market,oldVersion;long updated;
      try(var s=c.prepareStatement("SELECT status,report_json,policy_json,market_json,policy_version,updated_epoch FROM ua_daily_report WHERE run_id=?")){
        s.setString(1,ID);try(var r=s.executeQuery()){if(!r.next()||!"FAILED".equals(r.getString(1)))throw new IllegalStateException("NOT_FAILED");old=r.getString(2);policy=r.getString(3);market=r.getString(4);oldVersion=r.getString(5);updated=r.getLong(6);}
      }
      if(cached){
        store.report=JSON.readValue(Path.of("report-recovery",ID,"rebuilt-report.json").toFile(),DailyReport.class);
        store.evidence=JSON.readValue(Path.of("report-recovery",ID,"rebuilt-evidence.json").toFile(),JSON.getTypeFactory().constructCollectionType(List.class,StockDecision.class));
        if(!ID.equals(store.report.runId())||!"READY".equals(store.report.status())||!"RETROSPECTIVE_REBUILD".equals(store.report.validationStatus())||store.report.finalizedAt().isAfter(Instant.now())
          ||!store.report.version().startsWith(CandidateAnalyzer.VERSION+"/partial-coverage-v1/"))throw new IllegalStateException("CACHED_REPORT_INVALID");
        if(store.evidence.size()!=store.report.assessedStocks()||store.evidence.stream().map(StockDecision::code).distinct().count()!=store.evidence.size())throw new IllegalStateException("CACHED_EVIDENCE_INVALID");
      }else{
      var cache=Path.of("report-recovery",ID,"inputs");Files.createDirectories(cache);
      try(var s=c.prepareStatement("SELECT s.stock_code,s.market,s.status,s.result_json,i.input_json,i.input_gzip,i.input_sha256,i.input_utf8_bytes FROM ua_scan_item s LEFT JOIN ua_eod_item i ON i.run_id=s.run_id AND i.stock_code=s.stock_code WHERE s.run_id=? ORDER BY s.stock_code")){
        s.setString(1,ID);s.setFetchSize(10);
        try(var r=s.executeQuery()){while(r.next()){String code=r.getString(1);store.rows.add(Map.of("STOCK_CODE",code,"MARKET",r.getString(2),"STATUS",r.getString(3)));
          if("COMPLETED".equals(r.getString(3))){
            if(!code.matches("[A-Za-z0-9]+"))throw new IllegalStateException("INVALID_CODE");
            for(int col=4;col<=5;col++){
              var path=cache.resolve(code+(col==4?"-detail.json":"-input.json"));
              try(var reader=col==4?r.getCharacterStream(col):new java.io.StringReader(EodSnapshotCodec.read(r));var writer=Files.newBufferedWriter(path)){reader.transferTo(writer);}
              (col==4?store.details:store.inputs).put(code,path);
            }
            if(store.details.size()%200==0)System.out.println("LOADING "+store.details.size());
          }
        }}
      }
      System.out.println("LOADED completed="+store.details.size()+" total="+store.rows.size());
      var scans=new UnifiedScanStore(ds,JSON){
        public Optional<UnifiedAnalysisResponse> detail(String id,String code){try{return Optional.of(JSON.readValue(store.details.get(code).toFile(),UnifiedAnalysisResponse.class));}catch(Exception e){throw new IllegalArgumentException("DETAIL_INVALID");}}
      };
      var run=scans.find(ID).orElseThrow();var settings=JSON.readValue(policy,ReportProperties.class);
      var calendar=new TradingCalendar(new EodProperties("classpath:unified-calendar-2026.csv",Duration.ofHours(8),Duration.ofMinutes(10),Duration.ofSeconds(30),Duration.ofMinutes(90),Duration.ofHours(12),Duration.ofMinutes(1),3,Duration.ofMinutes(2)),new DefaultResourceLoader());
      var service=new DailyReportService(store,scans,date->{throw new IllegalStateException("NO_COLLECTION_ALLOWED");},new CandidateAnalyzer(JSON,settings),calendar,settings,new TelegramProperties(false,"","",""),Clock.systemUTC());
      var build=DailyReportService.class.getDeclaredMethod("build",ReportStore.Job.class,UnifiedScanModels.Run.class);build.setAccessible(true);
      try{build.invoke(service,new ReportStore.Job(ID,"manual",oldVersion,0,System.currentTimeMillis()+3600000,JSON.readValue(market,MarketSnapshot.class)),run);}
      catch(java.lang.reflect.InvocationTargetException e){System.out.println("BUILD_ERROR "+e.getCause().getClass().getSimpleName()+":"+e.getCause().getMessage());return;}
      }
      var warnings=new ArrayList<>(store.report.limitations());String retrospective="실패한 보고서를 저장 입력으로 사후 재생성했습니다. 실제 생성 시각 이전에 알 수 있었던 매매 신호로 간주하지 않습니다.";if(!warnings.contains(retrospective))warnings.add(retrospective);
      var r=store.report;var report=new DailyReport(r.runId(),r.status(),r.tradeDate(),r.forecastDate(),r.informationAsOf(),r.finalizedAt(),r.version(),"RETROSPECTIVE_REBUILD",r.scope(),r.markets(),r.candidates(),r.assessedStocks(),r.eligibleStocks(),warnings);
      var dir=Path.of("report-recovery",ID);Files.createDirectories(dir);
      var backup=dir.resolve("failed-report.json");if(!Files.exists(backup))Files.writeString(backup,old);
      Files.writeString(dir.resolve("rebuilt-report.json"),JSON.writeValueAsString(report));
      Files.writeString(dir.resolve("rebuilt-evidence.json"),JSON.writeValueAsString(store.evidence));
      System.out.println("VALIDATED assessed="+report.assessedStocks()+" eligible="+report.eligibleStocks()+" candidates="+report.candidates().size()+" version="+report.version());
      if(!commit)return;
      c.setAutoCommit(false);
      try {
        try(var s=c.prepareStatement("SELECT status,updated_epoch FROM ua_daily_report WHERE run_id=? FOR UPDATE")){s.setString(1,ID);try(var rs=s.executeQuery()){if(!rs.next()||!"FAILED".equals(rs.getString(1))||updated!=rs.getLong(2))throw new IllegalStateException("REPORT_CHANGED");}}
        try(var s=c.prepareStatement("SELECT COUNT(*) FROM ua_candidate_evidence WHERE run_id=?")){s.setString(1,ID);try(var rs=s.executeQuery()){rs.next();if(rs.getInt(1)!=0)throw new IllegalStateException("EXISTING_EVIDENCE");}}
        try(var s=c.prepareStatement("INSERT INTO ua_candidate_evidence(run_id,stock_code,disposition,strategy,detail_json) VALUES(?,?,?,?,?)")){
          int n=0;for(var e:store.evidence){s.setString(1,ID);s.setString(2,e.code());s.setString(3,e.disposition());s.setString(4,e.strategy());s.setClob(5,new java.io.StringReader(JSON.writeValueAsString(e)));s.addBatch();if(++n%100==0)s.executeBatch();}s.executeBatch();
        }
        try(var s=c.prepareStatement("UPDATE ua_daily_report SET status='READY',report_json=?,policy_version=?,error_code=NULL,updated_epoch=?,owner_token=NULL,lease_epoch=0 WHERE run_id=?")){
          s.setClob(1,new java.io.StringReader(JSON.writeValueAsString(report)));s.setString(2,report.version());s.setLong(3,report.finalizedAt().toEpochMilli());s.setString(4,ID);if(s.executeUpdate()!=1)throw new IllegalStateException("UPDATE_FAILED");
        }
        c.commit();System.out.println("COMMITTED_NO_NOTIFICATION");
      }catch(Exception e){c.rollback();System.out.println("ROLLED_BACK "+e.getClass().getSimpleName()+(e instanceof SQLException sql?" code="+sql.getErrorCode():""));throw e;}
    }
  }
}
