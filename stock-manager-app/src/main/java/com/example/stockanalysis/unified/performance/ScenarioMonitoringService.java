package com.example.stockanalysis.unified.performance;

import static com.example.stockanalysis.unified.performance.ScenarioMonitoringModels.*;
import com.example.stockanalysis.unified.eod.TradingCalendar;
import com.example.stockanalysis.unified.performance.PerformanceModels.Anchor;
import java.time.*;
import java.util.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

/** The report and subsequent EOD inputs already persist the monitoring evidence, even with evaluation disabled. */
@Service
public class ScenarioMonitoringService {
    private final PerformanceSources sources;
    private final ScenarioMonitoringEngine engine;
    private final TradingCalendar calendar;
    private final Clock clock;
    public ScenarioMonitoringService(PerformanceSources sources,ScenarioMonitoringEngine engine,TradingCalendar calendar,Clock clock) {
        this.sources=sources;this.engine=engine;this.calendar=calendar;this.clock=clock;
    }
    public View get(String runId) {
        var now=clock.instant();var report=sources.report(runId);
        if(report==null)return new View("WAITING_REPORT",runId,null,null,null,now,List.of(),List.of("확정된 선정 보고서를 기다리고 있습니다."));
        return evaluate(report,selections(report,null),now);
    }
    /** Evaluate only the requested selections; a paged overview must not expand every report's full list. */
    View selected(com.example.stockanalysis.unified.report.ReportModels.DailyReport report,List<String> codes,Instant now){
        var candidates=selections(report,codes);
        if(candidates.size()!=codes.size())throw new IllegalArgumentException("SELECTION_NOT_IN_REPORT");
        return evaluate(report,candidates,now);
    }
    private List<com.example.stockanalysis.unified.report.ReportModels.StockDecision> selections(
            com.example.stockanalysis.unified.report.ReportModels.DailyReport report,List<String> codes){
        var stocks=new LinkedHashMap<String,com.example.stockanalysis.unified.report.ReportModels.StockDecision>();
        report.candidates().stream().filter(s->codes==null||codes.contains(s.code())).forEach(s->stocks.put(s.code(),s));
        sources.comparisonSelections(report.runId(),codes).forEach(s->stocks.putIfAbsent(s.code(),s));
        return List.copyOf(stocks.values());
    }
    private View evaluate(com.example.stockanalysis.unified.report.ReportModels.DailyReport report,
                          List<com.example.stockanalysis.unified.report.ReportModels.StockDecision> candidates,Instant now){
        String runId=report.runId();
        var dates=new ArrayList<LocalDate>();String version=null;
        for(int offset=1;dates.size()<20&&offset<=60;offset++) {
            var day=calendar.day(report.tradeDate().plusDays(offset));
            if(day.status()==TradingCalendar.Status.UNKNOWN)break;
            if(version!=null&&!version.equals(day.version()))break;
            version=day.version();if(day.status()==TradingCalendar.Status.OPEN)dates.add(day.date());
        }
        boolean complete=dates.size()==20;
        boolean timely=!dates.isEmpty()&&dates.get(0).equals(report.forecastDate())
                && !report.finalizedAt().isAfter(now)&&!report.informationAsOf().isAfter(now)
                && report.finalizedAt().isBefore(dates.get(0).atTime(9,0).atZone(ZoneId.of("Asia/Seoul")).toInstant())
                && report.informationAsOf().isBefore(dates.get(0).atTime(9,0).atZone(ZoneId.of("Asia/Seoul")).toInstant());
        var stocks=new ArrayList<Stock>();
        for(var candidate:candidates) {
            Anchor anchor=null;
            try {anchor=sources.anchor(runId,candidate.code(),candidate.market(),report.tradeDate());}
            catch(IllegalArgumentException|EmptyResultDataAccessException e){/* Keep the selected row visible with unknown data. */}
            var history=anchor==null?List.<PerformanceModels.Observation>of():sources.monitoringHistory(anchor,dates,now);
            stocks.add(engine.evaluate(candidate,anchor,dates,history,now,complete,timely));
        }
        return new View("READY",runId,report.tradeDate(),dates.isEmpty()?null:dates.get(0),complete?dates.get(19):null,now,List.copyOf(stocks),List.of(
                "선정 당시 시나리오를 고정하고 이후 20거래일을 추적합니다. 새 선정 결과로 이전 목표·손절을 변경하지 않습니다.",
                "매일 마감 분석에서 저장한 일봉을 사용합니다. 장중 실시간 감시·주문·알림 발송은 수행하지 않습니다.",
                "시나리오 추적은 성과 평가 활성화·등록과 별개입니다. 보고서와 이후 EOD 자료가 저장되어 있어야 합니다.",
                "조건부 진입은 다음 거래일 시가와 체결 차이를 반영한 가격이 진입 구간 안인지 확인합니다. 종목별 결과에 비용 가정과 모의 손익을 표시합니다. 실제 체결이 아닙니다.",
                "같은 일봉에서 목표와 손절을 모두 접촉하면 시가로 순서를 알 수 있는 경우 외에는 순서 미확인으로 남깁니다. 누락·수정주가 기준 변경은 정상 진행으로 간주하지 않습니다."));
    }
}
