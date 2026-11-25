package com.example.stockanalysis.unified.performance;

import static com.example.stockanalysis.unified.performance.ScenarioMonitoringModels.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Paginates saved selection events, preserving a separate frozen scenario for every report. */
@Service
public class ScenarioMonitoringOverviewService {
    private static final ZoneId SEOUL=ZoneId.of("Asia/Seoul");
    private final JdbcTemplate jdbc;
    private final PerformanceSources sources;
    private final ScenarioMonitoringService monitoring;
    private final Clock clock;
    public ScenarioMonitoringOverviewService(DataSource source,PerformanceSources sources,ScenarioMonitoringService monitoring,Clock clock){
        jdbc=new JdbcTemplate(source);this.sources=sources;this.monitoring=monitoring;this.clock=clock;
    }
    private record Key(String runId,LocalDate date,String code,String name){}
    public Overview list(LocalDate from,LocalDate to,String search,String disposition,int page,int size){
        return list(from,to,search,disposition,page,size,false);
    }
    public Overview list(LocalDate from,LocalDate to,String search,String disposition,int page,int size,boolean deleted){
        boolean deletionEnabled=deletionAvailable();
        Instant now=clock.instant();LocalDate today=now.atZone(SEOUL).toLocalDate();
        from=from==null?today.minusDays(7):from;to=to==null?today:to;
        search=search==null?"":search.trim();disposition=disposition==null?"ALL":disposition;
        if(from.isAfter(to)||to.isAfter(today)||from.plusDays(366).isBefore(to)||search.length()>100||page<0||page>100000||size<1||size>50
                ||!Set.of("ALL","WATCH","ENTRY_CONDITIONAL","RESEARCH_B").contains(disposition))
            throw new IllegalArgumentException("선정 기간은 오늘까지 최대 1년, 페이지 크기는 1~50이며 검색 조건이 올바른지 확인하세요.");
        var args=new ArrayList<Object>();args.add(java.sql.Date.valueOf(from));args.add(java.sql.Date.valueOf(to));args.add(now.toEpochMilli());
        String sql=" FROM ua_candidate_evidence e JOIN ua_daily_report d ON d.run_id=e.run_id JOIN ua_scan_run r ON r.run_id=e.run_id "
                +"JOIN ua_eod_run o ON o.run_id=r.run_id JOIN ua_scan_item i ON i.run_id=e.run_id AND i.stock_code=e.stock_code "
                +"WHERE d.status='READY' AND d.report_json IS NOT NULL AND r.market='ALL' AND r.base_date BETWEEN ? AND ? AND d.updated_epoch<=? "
                +"AND e.disposition IN ('WATCH','ENTRY_CONDITIONAL','RESEARCH_B')";
        if(deletionEnabled)sql+=(deleted?" AND EXISTS":" AND NOT EXISTS")+" (SELECT 1 FROM ua_monitoring_hidden h WHERE h.run_id=e.run_id AND h.stock_code=e.stock_code)";
        else if(deleted)sql+=" AND 1=0";
        if(!search.isEmpty()){
            sql+=" AND (e.stock_code LIKE ? ESCAPE '!' OR i.stock_name LIKE ? ESCAPE '!')";
            String pattern="%"+search.replace("!","!!").replace("%","!%").replace("_","!_")+"%";args.add(pattern);args.add(pattern);
        }
        if(!disposition.equals("ALL")){sql+=" AND e.disposition=?";args.add(disposition);}
        long total=jdbc.queryForObject("SELECT COUNT(*)"+sql,Long.class,args.toArray());
        args.add((long)page*size);args.add(size);
        var keys=jdbc.query("SELECT e.run_id,r.base_date,e.stock_code,i.stock_name"+sql+" ORDER BY r.base_date DESC,r.created_epoch DESC,r.run_id DESC,e.stock_code OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                (r,n)->new Key(r.getString(1),r.getDate(2).toLocalDate(),r.getString(3),r.getString(4)),args.toArray());
        var rows=new ArrayList<Selection>();
        // Read each report only once, then evaluate each selected row independently so a bad source remains visible.
        var reports=new HashMap<String,com.example.stockanalysis.unified.report.ReportModels.DailyReport>();
        var broken=new HashSet<String>();
        for(var key:keys){
            try{
                if(broken.contains(key.runId()))throw new IllegalArgumentException("REPORT_UNAVAILABLE");
                var report=reports.get(key.runId());
                if(report==null){try{report=sources.report(key.runId());}catch(IllegalArgumentException e){broken.add(key.runId());throw e;}
                    if(report==null){broken.add(key.runId());throw new IllegalArgumentException("REPORT_UNAVAILABLE");}reports.put(key.runId(),report);}
                var view=monitoring.selected(report,List.of(key.code()),now);
                rows.add(new Selection(key.runId(),key.date(),view.firstTrackingDate(),view.lastTrackingDate(),key.code(),key.name(),view.stocks().get(0),null));
            }catch(java.util.concurrent.CancellationException e){throw e;}
            catch(org.springframework.dao.EmptyResultDataAccessException|IllegalArgumentException|IllegalStateException e){
                rows.add(new Selection(key.runId(),key.date(),null,null,key.code(),key.name(),null,"SOURCE_UNAVAILABLE"));
            }
        }
        return new Overview(from,to,page,size,total,(long)(page+1)*size<total,now,List.copyOf(rows),List.of(
                "보고서 선택과 독립적으로 기간 내 전체 시장 보고서의 선정 이력을 조회합니다. 같은 종목도 선정일·실행별 목표와 손절을 각각 유지합니다.",
                "매일 저장된 마감 일봉으로 최대 20거래일을 추적합니다. 실시간 시세·실제 보유·체결 상태를 뜻하지 않습니다.",
                "선정일 대비 등락률은 가격 변화이며 모의 매매 수익률과 다릅니다. 목록 삭제는 원본 보고서·성과 통계에서 제외하는 기능이 아닙니다.",
                deletionEnabled?"삭제한 선정 이력은 삭제 이력 보기에서 복원할 수 있습니다.":"목록 삭제 기능이 아직 준비되지 않았습니다. 서버의 저장 기능 설정을 확인해 주세요."),deletionEnabled);
    }

    private boolean deletionAvailable(){
        try{jdbc.queryForObject("SELECT COUNT(*) FROM ua_monitoring_hidden WHERE 1=0",Long.class);return true;}
        catch(org.springframework.jdbc.BadSqlGrammarException e){
            var cause=e.getSQLException();
            if(cause!=null&&(cause.getErrorCode()==942||cause.getErrorCode()==42102||cause.getErrorCode()==42104))return false;
            throw e;
        }
    }
    /** Only a list visibility preference changes; immutable reports, evidence and statistics are untouched. */
    @org.springframework.transaction.annotation.Transactional
    public void visibility(String runId,String code,boolean hidden){
        if(runId==null||!runId.matches("[A-Za-z0-9-]{1,64}")||code==null||!code.matches("[0-9A-Za-z]{6}"))
            throw new IllegalArgumentException("선정 이력 식별자가 올바르지 않습니다.");
        if(!deletionAvailable())throw new IllegalArgumentException("목록 삭제 기능의 DB 설치가 필요합니다.");
        var count=jdbc.queryForObject("SELECT COUNT(*) FROM ua_candidate_evidence e JOIN ua_daily_report d ON d.run_id=e.run_id JOIN ua_scan_run r ON r.run_id=e.run_id WHERE e.run_id=? AND e.stock_code=? AND e.disposition IN ('WATCH','ENTRY_CONDITIONAL','RESEARCH_B') AND d.status='READY' AND r.market='ALL'",Long.class,runId,code);
        if(count==null||count==0)throw new IllegalArgumentException("전체 선정 이력을 찾을 수 없습니다.");
        if(!hidden){jdbc.update("DELETE FROM ua_monitoring_hidden WHERE run_id=? AND stock_code=?",runId,code);return;}
        try{jdbc.update("INSERT INTO ua_monitoring_hidden(run_id,stock_code,hidden_epoch) VALUES(?,?,?)",runId,code,clock.millis());}
        catch(org.springframework.dao.DuplicateKeyException ignored){/* Idempotent repeated delete. */}
    }
}
