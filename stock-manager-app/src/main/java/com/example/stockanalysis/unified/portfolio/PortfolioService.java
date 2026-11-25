package com.example.stockanalysis.unified.portfolio;

import static com.example.stockanalysis.unified.portfolio.PortfolioModels.*;
import com.example.stockanalysis.unified.performance.*;
import com.example.stockanalysis.unified.eod.TradingCalendar;
import com.example.stockanalysis.shared.exception.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;

@Service
public class PortfolioService {
    private static final ZoneId SEOUL=ZoneId.of("Asia/Seoul");
    private final PortfolioProperties settings;private final PortfolioStore store;private final PortfolioSources sources;
    private final PerformanceSources prices;private final PerformanceProperties costs;private final PortfolioEngine engine;
    private final TradingCalendar calendar;private final Clock clock;private final AtomicBoolean active=new AtomicBoolean();
    public PortfolioService(PortfolioProperties settings,PortfolioStore store,PortfolioSources sources,PerformanceSources prices,
                            PerformanceProperties costs,PortfolioEngine engine,TradingCalendar calendar,Clock clock){
        this.settings=settings;this.store=store;this.sources=sources;this.prices=prices;this.costs=costs;this.engine=engine;this.calendar=calendar;this.clock=clock;
    }
    public boolean enabled(){return settings.enabled();}
    public List<Summary> recent(){return settings.enabled()?store.recent():List.of();}
    public List<Available> available(){return settings.enabled()?sources.available():List.of();}
    public SelectionDiagnostics.Audit audit(String id){requireEnabled();return sources.audit(id);}
    public View get(String id){if(!settings.enabled())return new View(id,"DISABLED",null,0,null,null);var value=store.view(id);if(value==null)throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);return value;}
    public Result revision(String id,int n){requireEnabled();var result=store.revision(id,n);if(result==null)throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);return result;}
    public View create(Request request,String key){
        requireEnabled();if(key==null||!key.matches("[A-Za-z0-9._:-]{1,100}"))throw new IllegalArgumentException("Idempotency-Key가 필요합니다.");
        var fingerprint=hash(store.json(request));var existing=store.byKey(key,fingerprint);if(existing!=null)return get(existing);
        var now=clock.instant();var cohorts=request.reportIds().stream().map(id->sources.cohort(id,now)).sorted(Comparator.comparing(c->c.report().tradeDate())).toList();
        if(cohorts.stream().map(c->c.report().tradeDate()).distinct().count()!=cohorts.size())throw new IllegalArgumentException("같은 거래일 보고서는 하나만 선택하세요.");
        if(cohorts.stream().map(c->c.report().version()).distinct().count()!=1)throw new IllegalArgumentException("같은 선정 규칙 버전의 보고서로 비교하세요.");
        var first=cohorts.get(0).report().forecastDate();var lastEntry=cohorts.get(cohorts.size()-1).report().forecastDate();
        var dates=new ArrayList<LocalDate>();String calendarVersion=null;int tail=0;
        for(var day=first;tail<20;day=day.plusDays(1)){
            if(java.time.temporal.ChronoUnit.DAYS.between(first,day)>730)throw new IllegalArgumentException("평가 기간은 최대 2년입니다.");
            var trading=calendar.day(day);if(trading.status()==TradingCalendar.Status.UNKNOWN)throw new IllegalArgumentException("평가 기간 거래일 달력을 확인할 수 없습니다.");
            if(calendarVersion!=null&&!calendarVersion.equals(trading.version()))throw new IllegalArgumentException("달력 버전이 변경되었습니다.");calendarVersion=trading.version();
            if(trading.status()==TradingCalendar.Status.OPEN){dates.add(day);if(!day.isBefore(lastEntry))tail++;}
        }
        for(var cohort:cohorts){
            var report=cohort.report();LocalDate expected=null;
            for(var d=report.tradeDate().plusDays(1);!d.isAfter(report.forecastDate());d=d.plusDays(1)){
                var trading=calendar.day(d);if(trading.status()==TradingCalendar.Status.UNKNOWN||!Objects.equals(calendarVersion,trading.version()))throw new IllegalArgumentException("보고서 달력 확인 불가");
                if(trading.status()==TradingCalendar.Status.OPEN){expected=d;break;}
            }
            if(!report.forecastDate().equals(expected)||!dates.contains(expected))throw new IllegalArgumentException("보고서의 다음 거래일이 달력과 다릅니다.");
        }
        var policy=new Policy(PortfolioEngine.VERSION,request.initialCash(),request.maximumPositions(),request.positionPercent(),costs.roundTripFeeBps(),costs.slippageBps(),costs.neutralBandPercent(),20,calendarVersion);
        var input=new Input(policy,cohorts,List.copyOf(dates),now,now.isBefore(first.atTime(9,0).atZone(SEOUL).toInstant())?"FORWARD_CAPTURE":"RETROSPECTIVE");
        var id=store.register(key,fingerprint,input,dates.get(dates.size()-1).plusDays(14).atTime(23,59).atZone(SEOUL).toInstant().toEpochMilli());return get(id);
    }
    public void tick(){if(!settings.enabled()||!active.compareAndSet(false,true))return;try{store.claim(clock.millis()).ifPresent(this::process);}finally{active.set(false);}}
    private void process(PortfolioStore.Job job){
        try{
            if(!PortfolioEngine.supported(job.input().policy().version())){store.failure(job,"ENGINE_VERSION_CHANGED",clock.millis());return;}
            if(job.attempts()>3){store.failure(job,"ATTEMPTS_EXHAUSTED",clock.millis());return;}
            var now=clock.instant();var today=now.atZone(SEOUL).toLocalDate();var known=new HashSet<String>();store.observations(job.id()).forEach(o->known.add(o.key()));
            for(var cohort:job.input().cohorts()){
                for(var anchor:cohort.anchors()){
                    store.heartbeat(job);
                    int checked=0;
                    for(var day:job.input().dates()){
                        if(day.isAfter(today))break;if(day.isBefore(cohort.report().forecastDate()))continue;
                        if(++checked%20==0)store.heartbeat(job);
                        var key=cohort.report().runId()+":STOCK:"+anchor.code()+":"+day;
                        if(!known.contains(key))prices.stock(anchor,day,now).ifPresent(o->store.observe(job,new Observed(cohort.report().runId(),o)));
                    }
                }
                store.heartbeat(job);
                for(var market:cohort.report().markets())if(!cohort.report().forecastDate().isAfter(today)){
                    var key=cohort.report().runId()+":INDEX:"+market.market()+":"+cohort.report().forecastDate();
                    if(!known.contains(key))prices.index(market,cohort.report().tradeDate(),cohort.report().forecastDate(),now).ifPresent(o->store.observe(job,new Observed(cohort.report().runId(),o)));
                }
            }
            store.heartbeat(job);var result=engine.evaluate(job.input(),store.observations(job.id()),now,clock.millis()>=job.deadline());
            store.complete(job,result,clock.millis()+900000);
        }catch(PortfolioStore.LostOwnership|java.util.concurrent.CancellationException e){throw e;}
        catch(RuntimeException e){store.failure(job,"PORTFOLIO_EVALUATION_FAILED",clock.millis());}
    }
    private void requireEnabled(){if(!settings.enabled())throw new BusinessException(ErrorCode.UNIFIED_ANALYSIS_CONFLICT,"누적 모의계좌가 비활성화되어 있습니다.");}
    private String hash(String text){try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
