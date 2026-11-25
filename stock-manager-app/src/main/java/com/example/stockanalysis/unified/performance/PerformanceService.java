package com.example.stockanalysis.unified.performance;

import static com.example.stockanalysis.unified.performance.PerformanceModels.*;
import com.example.stockanalysis.shared.exception.*;
import com.example.stockanalysis.unified.eod.TradingCalendar;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;

@Service
public class PerformanceService {
    private static final ZoneId SEOUL=ZoneId.of("Asia/Seoul");
    private final PerformanceProperties settings;
    private final PerformanceStore store;
    private final PerformanceSources sources;
    private final PerformanceEngine engine;
    private final TradingCalendar calendar;
    private final Clock clock;
    private final AtomicBoolean active=new AtomicBoolean();
    public PerformanceService(PerformanceProperties settings,PerformanceStore store,PerformanceSources sources,
                              PerformanceEngine engine,TradingCalendar calendar,Clock clock) {
        this.settings=settings;this.store=store;this.sources=sources;this.engine=engine;this.calendar=calendar;this.clock=clock;
    }
    public View get(String id){return settings.enabled()?store.view(id):new View("DISABLED",null,0,null);}
    public View request(String id) {
        if(!settings.enabled())throw conflict("사후 성과 검증이 비활성화되어 있습니다.");
        var existing=store.view(id);if(!existing.state().equals("NOT_CREATED") && !"REGISTRATION_DATA_OR_CALENDAR_INVALID".equals(existing.errorCode()))return existing;
        var report=sources.report(id);
        if(report==null)throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,"확정된 정상 보고서가 없습니다.");
        if(report.finalizedAt().isAfter(clock.instant()) || report.informationAsOf().isAfter(clock.instant()))throw conflict("현재 시각 이후의 보고서를 평가할 수 없습니다.");
        var dates=new ArrayList<LocalDate>();String version=null;
        for(int offset=1;dates.size()<20 && offset<=60;offset++) {
            var day=calendar.day(report.tradeDate().plusDays(offset));
            if(day.status()==TradingCalendar.Status.UNKNOWN)throw conflict("20거래일 평가 달력을 확인할 수 없습니다.");
            if(version!=null && !version.equals(day.version()))throw conflict("등록 중 달력 버전이 변경되었습니다.");
            version=day.version();if(day.status()==TradingCalendar.Status.OPEN)dates.add(day.date());
        }
        if(dates.size()!=20 || !dates.get(0).equals(report.forecastDate()))throw conflict("보고서와 평가 거래일이 일치하지 않습니다.");
        var firstOpen=dates.get(0).atTime(9,0).atZone(SEOUL).toInstant();
        if(!report.finalizedAt().isBefore(firstOpen) || !report.informationAsOf().isBefore(firstOpen))throw conflict("진입 시각 이후 확정된 보고서는 평가에 사용할 수 없습니다.");
        var anchors=new ArrayList<Anchor>();
        for(var stock:report.candidates())anchors.add(sources.anchor(id,stock.code(),stock.market(),report.tradeDate()));
        var policy=new Policy(PerformanceEngine.VERSION,settings.roundTripFeeBps(),settings.slippageBps(),settings.neutralBandPercent(),20,version);
        var input=new Input(report,policy,List.copyOf(dates),List.copyOf(anchors),clock.instant().isBefore(firstOpen)?"FORWARD_CAPTURE":"RETROSPECTIVE",clock.instant());
        store.register(input,dates.get(19).plusDays(14).atTime(23,59).atZone(SEOUL).toInstant().toEpochMilli());
        return store.view(id);
    }
    public void tick() {
        if(!settings.enabled() || !active.compareAndSet(false,true))return;
        try {
            for(var id:store.discover(clock.millis())) {
                try{request(id);}
                catch(BusinessException|IllegalArgumentException e){store.reject(id,"REGISTRATION_DATA_OR_CALENDAR_INVALID",clock.millis());}
            }
            store.claim(clock.millis()).ifPresent(this::process);
        }finally{active.set(false);}
    }
    private void process(PerformanceStore.Job job) {
        try {
            if(!PerformanceEngine.supported(job.version())){store.failure(job,"ENGINE_VERSION_CHANGED",clock.millis());return;}
            if(job.attempts()>3){store.failure(job,"ATTEMPTS_EXHAUSTED",clock.millis());return;}
            var input=job.input();var now=clock.instant();var today=now.atZone(SEOUL).toLocalDate();
            var keys=new HashSet<String>();store.observations(job.id()).forEach(o->keys.add(o.key()));
            for(var anchor:input.anchors()) {
                store.heartbeat(job);
                for(var day:input.dates()) {
                    if(day.isAfter(today))break;
                    if(!keys.contains("STOCK:"+anchor.code()+":"+day))sources.stock(anchor,day,now).ifPresent(o->store.observe(job,o));
                }
            }
            for(var market:input.report().markets()) {
                store.heartbeat(job);
                var horizons=PerformanceEngine.VERSION.equals(input.policy().version())?PerformanceEngine.horizons(input.policy().version()):List.of(1);
                for(int horizon:horizons){
                    var day=input.dates().get(horizon-1);
                    if(!day.isAfter(today) && !keys.contains("INDEX:"+market.market()+":"+day))
                        sources.index(market,input.report().tradeDate(),day,now).ifPresent(o->store.observe(job,o));
                }
            }
            var result=engine.evaluate(input,store.observations(job.id()),now,clock.millis()>=job.deadline());
            store.complete(job,result,clock.millis()+Duration.ofMinutes(15).toMillis());
        }catch(PerformanceStore.LostOwnership|java.util.concurrent.CancellationException e){throw e;}
        catch(RuntimeException e){store.failure(job,"EVALUATION_DATA_OR_STORAGE_FAILED",clock.millis());}
    }
    private BusinessException conflict(String message){return new BusinessException(ErrorCode.UNIFIED_ANALYSIS_CONFLICT,message);}
}
