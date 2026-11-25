package com.example.stockanalysis.unified.portfolio;

import com.example.stockanalysis.shared.exception.*;
import com.example.stockanalysis.unified.eod.TradingCalendar;
import com.example.stockanalysis.unified.performance.PerformanceProperties;
import com.example.stockanalysis.unified.report.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** Explicit operator surface. No credentials, arbitrary destinations, automatic test sends or live quotes. */
@Service
public class OperationsService {
    public record Pipeline(String name,String state,Map<String,Long> counts,List<JobStatus> recent) {}
    public record JobStatus(String id,String status,Instant createdAt,Instant updatedAt,String errorCode) {}
    public record TestResult(String key,String state,String errorCode,String messageId,Instant requestedAt) {}
    public record Status(boolean enabled,Map<String,Boolean> features,Map<String,Boolean> tables,List<Pipeline> pipelines,
                         LocalDate expectedTradeDate,boolean expectedScanPresent,LocalDate calendarThrough,String calendarVersion,
                         String destinationLabel,String destinationFingerprint,boolean credentialsConfigured,boolean testEnabled,
                         List<TestResult> tests,List<String> warnings) {}
    public record Preview(String runId,String text) {}
    public record TestRequest(boolean confirm,String destinationFingerprint) {}
    private static final List<String> TABLES=List.of("ua_scan_run","ua_scan_item","ua_eod_run","ua_eod_item","ua_eod_schedule","ua_eod_lease",
            "ua_report_lock","ua_daily_report","ua_candidate_evidence","ua_report_delivery","ua_eval_lock","ua_eval_run","ua_eval_observation","ua_eval_revision",
            "ua_portfolio_lock","ua_portfolio_run","ua_portfolio_observation","ua_portfolio_revision","ua_ops_delivery_test");
    private final DataSource source;private final JdbcTemplate jdbc;private final TransactionTemplate tx;
    private final PortfolioProperties settings;private final ReportProperties reports;private final PerformanceProperties performance;
    private final TelegramProperties telegram;private final TelegramGateway gateway;private final ReportStore reportStore;
    private final TradingCalendar calendar;private final Clock clock;private final Environment environment;
    public OperationsService(DataSource source,PortfolioProperties settings,ReportProperties reports,PerformanceProperties performance,
                             TelegramProperties telegram,TelegramGateway gateway,ReportStore reportStore,TradingCalendar calendar,Clock clock,Environment environment){
        this.source=source;jdbc=new JdbcTemplate(source);tx=new TransactionTemplate(new DataSourceTransactionManager(source));
        this.settings=settings;this.reports=reports;this.performance=performance;this.telegram=telegram;this.gateway=gateway;
        this.reportStore=reportStore;this.calendar=calendar;this.clock=clock;this.environment=environment;
    }
    public Status status(){
        if(!settings.operationsEnabled())return new Status(false,Map.of(),Map.of(),List.of(),null,false,null,null,null,null,false,false,List.of(),List.of());
        var features=new LinkedHashMap<String,Boolean>();features.put("schedule",environment.getProperty("unified.analysis-schedule.enabled",Boolean.class,true));
        features.put("eodWorker",environment.getProperty("unified.eod.worker-enabled",Boolean.class,true));features.put("report",reports.enabled());
        features.put("telegram",telegram.enabled());features.put("performance",performance.enabled());features.put("portfolio",settings.enabled());
        var tables=new LinkedHashMap<String,Boolean>();var warnings=new ArrayList<String>();
        try(var connection=source.getConnection()){
            for(var table:TABLES)try(var rows=connection.getMetaData().getTables(connection.getCatalog(),connection.getSchema(),table.toUpperCase(Locale.ROOT),new String[]{"TABLE"})){
                boolean present=false;while(rows.next())if(table.equalsIgnoreCase(rows.getString("TABLE_NAME"))){present=true;break;}
                tables.put(table,present);
            }
        }catch(Exception e){warnings.add("DB 메타데이터 확인 실패: 연결과 스키마 권한을 확인하세요.");}
        if(tables.values().stream().anyMatch(v->!v))warnings.add("일부 필수 테이블이 없습니다. 해당 차수 마이그레이션 적용 여부를 확인하세요.");
        var pipelines=new ArrayList<Pipeline>();
        pipelines.add(pipeline("analysis","ua_scan_run","created_epoch","COALESCE(completed_epoch,created_epoch)",tables));
        pipelines.add(pipeline("report","ua_daily_report","created_epoch","updated_epoch",tables));
        pipelines.add(pipeline("performance","ua_eval_run","created_epoch","updated_epoch",tables));
        pipelines.add(pipeline("portfolio","ua_portfolio_run","created_epoch","updated_epoch",tables));
        // Outbox has no sent-at column in phase 2; do not label deadline/next-try as send time.
        pipelines.add(pipeline("delivery","ua_report_delivery",null,null,tables));
        var now=LocalDateTime.ofInstant(clock.instant(),ZoneId.of("Asia/Seoul"));var expected=calendar.latestDue(now);boolean found=false;
        if(expected.status()==TradingCalendar.Status.OPEN&&Boolean.TRUE.equals(tables.get("ua_eod_run"))&&Boolean.TRUE.equals(tables.get("ua_scan_run"))){
            try{found=jdbc.queryForObject("SELECT COUNT(*) FROM ua_scan_run r JOIN ua_eod_run e ON e.run_id=r.run_id WHERE r.base_date=? AND r.market='ALL'",Integer.class,java.sql.Date.valueOf(expected.date()))>0;}
            catch(RuntimeException e){warnings.add("최근 예약 실행 확인 실패");}
            if(!found)warnings.add("최근 예정 거래일의 전체 시장 EOD 실행이 확인되지 않습니다.");
        }
        LocalDate through=null;String version=null;int open=0;
        for(int offset=0;offset<60;offset++){
            var day=calendar.day(now.toLocalDate().plusDays(offset));if(day.status()==TradingCalendar.Status.UNKNOWN)break;
            if(version!=null&&!version.equals(day.version()))break;version=day.version();through=day.date();if(day.status()==TradingCalendar.Status.OPEN)open++;
        }
        if(open<20)warnings.add("향후 20거래일 달력이 확보되지 않았습니다.");
        if(!features.get("eodWorker")&&(performance.enabled()||settings.enabled()))warnings.add("EOD 작업자가 꺼져 있어 후속 가격 축적이 중단될 수 있습니다.");
        if(!reports.enabled()&&(performance.enabled()||settings.enabled()))warnings.add("보고서 생성이 꺼져 있어 신규 보고서와 지수 관측이 쌓이지 않을 수 있습니다.");
        for(var pipeline:pipelines)if(pipeline.counts().getOrDefault("WAITING_DATA",0L)>0)warnings.add(pipeline.name()+": 후속 가격 또는 거래일을 기다리는 평가가 있습니다.");
        return new Status(true,features,tables,pipelines,expected.status()==TradingCalendar.Status.OPEN?expected.date():null,found,through,version,
                maskedDestination(),fingerprint(),!telegram.botToken().isBlank()&&!telegram.chatId().isBlank(),settings.testDeliveryEnabled()&&telegram.enabled(),
                Boolean.TRUE.equals(tables.get("ua_ops_delivery_test"))?tests():List.of(),warnings);
    }
    private Pipeline pipeline(String name,String table,String created,String updated,Map<String,Boolean> tables){
        if(!tables.containsKey(table))return new Pipeline(name,"METADATA_UNAVAILABLE",Map.of(),List.of());
        if(!Boolean.TRUE.equals(tables.get(table)))return new Pipeline(name,"SCHEMA_MISSING",Map.of(),List.of());
        try{
            var counts=new TreeMap<String,Long>();jdbc.query("SELECT status,COUNT(*) total FROM "+table+" GROUP BY status",r->{counts.put(r.getString(1),r.getLong(2));});
            var rows=jdbc.query("SELECT run_id,status,"+(created==null?"NULL,NULL":created+","+updated)+",error_code FROM "+table+" ORDER BY "+(created==null?"run_id":created)+" DESC FETCH FIRST 10 ROWS ONLY",
                    (r,n)->new JobStatus(r.getString(1),r.getString(2),r.getObject(3)==null?null:Instant.ofEpochMilli(r.getLong(3)),r.getObject(4)==null?null:Instant.ofEpochMilli(r.getLong(4)),r.getString(5)));
            return new Pipeline(name,"AVAILABLE",counts,rows);
        }catch(RuntimeException e){return new Pipeline(name,"SCHEMA_OR_QUERY_ERROR",Map.of(),List.of());}
    }
    public Preview preview(String id){requireEnabled();var view=reportStore.view(id);if(view.report()==null)throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,"확정 보고서가 없습니다.");
        return new Preview(id,ReportMessage.format(view.report(),telegram.publicBaseUrl()));}
    public TestResult test(String key,TestRequest request){
        requireEnabled();if(!settings.testDeliveryEnabled()||!telegram.enabled())throw conflict("테스트 발송을 별도 설정으로 활성화해야 합니다.");
        if(key==null||!key.matches("[A-Za-z0-9._:-]{1,100}")||request==null||!request.confirm()||!Objects.equals(request.destinationFingerprint(),fingerprint()))throw conflict("설정된 수신 대상과 명시적인 테스트 발송 확인이 필요합니다.");
        boolean claimed=Boolean.TRUE.equals(tx.execute(s->{
            jdbc.queryForList("SELECT lock_id FROM ua_portfolio_lock WHERE lock_id=1 FOR UPDATE");
            var old=findTest(key);if(old!=null){
                if(!fingerprint().equals(jdbc.queryForObject("SELECT destination_hash FROM ua_ops_delivery_test WHERE request_key=?",String.class,key)))throw conflict("기존 테스트의 수신 대상이 다릅니다.");return false;}
            long now=clock.millis();if(jdbc.queryForObject("SELECT COUNT(*) FROM ua_ops_delivery_test WHERE created_epoch>?",Integer.class,now-60000)>0)throw conflict("테스트는 1분에 한 번만 보낼 수 있습니다.");
            jdbc.update("INSERT INTO ua_ops_delivery_test(request_key,destination_hash,status,created_epoch,updated_epoch) VALUES(?,?,'SENDING',?,?)",key,fingerprint(),now,now);return true;
        }));
        if(!claimed)return findTest(key);
        TelegramGateway.Result sent;
        try{sent=gateway.send(telegram.chatId(),"Stock Manager 텔레그램 연결 테스트\n분석 결과나 주문이 아닌 연결 확인 메시지입니다.\n확인 번호: "+key);}
        catch(RuntimeException e){sent=new TelegramGateway.Result("UNKNOWN",null,"DELIVERY_UNCONFIRMED",0);}
        var state=sent.state().equals("RETRY_WAIT")?"REJECTED":sent.state();
        jdbc.update("UPDATE ua_ops_delivery_test SET status=?,message_id=?,error_code=?,updated_epoch=? WHERE request_key=? AND status='SENDING'",state,sent.messageId(),sent.errorCode(),clock.millis(),key);
        return findTest(key);
    }
    private List<TestResult> tests(){return jdbc.query("SELECT request_key,status,error_code,message_id,created_epoch FROM ua_ops_delivery_test ORDER BY created_epoch DESC FETCH FIRST 10 ROWS ONLY",(r,n)->testRow(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getLong(5)));}
    private TestResult findTest(String key){return jdbc.query("SELECT request_key,status,error_code,message_id,created_epoch FROM ua_ops_delivery_test WHERE request_key=?",(r,n)->testRow(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getLong(5)),key).stream().findFirst().orElse(null);}
    private TestResult testRow(String key,String state,String error,String message,long time){return new TestResult(key,state.equals("SENDING")&&clock.millis()-time>120000?"UNKNOWN":state,error,message,Instant.ofEpochMilli(time));}
    private String maskedDestination(){var id=telegram.chatId();return id.isBlank()?"미설정":"…"+id.substring(Math.max(0,id.length()-4));}
    private String fingerprint(){try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(telegram.chatId().getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private void requireEnabled(){if(!settings.operationsEnabled())throw conflict("운영 확인 기능이 비활성화되어 있습니다.");}
    private BusinessException conflict(String message){return new BusinessException(ErrorCode.UNIFIED_ANALYSIS_CONFLICT,message);}
}
