package com.example.stockanalysis.unified.report;

import static com.example.stockanalysis.unified.report.ReportModels.*;
import static com.example.stockanalysis.unified.report.CandidateAnalyzer.*;
import com.example.stockanalysis.unified.eod.TradingCalendar;
import com.example.stockanalysis.unified.scan.*;
import com.example.stockanalysis.unified.scan.UnifiedScanModels.Run;
import com.example.stockanalysis.shared.exception.*;
import com.example.stockanalysis.engine.screener.EmaCalculator;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;

@Service
public class DailyReportService {
    private static final ZoneId SEOUL=ZoneId.of("Asia/Seoul");
    private final ReportStore store;
    private final UnifiedScanStore scans;
    private final MarketDataProvider markets;
    private final CandidateAnalyzer candidates;
    private final TradingCalendar calendar;
    private final ReportProperties settings;
    private final TelegramProperties telegram;
    private final Clock clock;
    private final String version;
    private final AtomicBoolean active=new AtomicBoolean();
    public DailyReportService(ReportStore store,UnifiedScanStore scans,MarketDataProvider markets,CandidateAnalyzer candidates,
                              TradingCalendar calendar,ReportProperties settings,TelegramProperties telegram,Clock clock){
        this.store=store;this.scans=scans;this.markets=markets;this.candidates=candidates;this.calendar=calendar;this.settings=settings;this.telegram=telegram;this.clock=clock;
        version=VERSION+"/"+com.example.stockanalysis.judgment.NewEntryEvaluator.VERSION+"/"+com.example.stockanalysis.judgment.SuitabilityTier.VERSION+"/coverage-v1/"+hash(store.json(settings));
    }
    public View get(String id){run(id);return settings.enabled()?store.view(id):new View("DISABLED",null,null,"DISABLED");}
    public Library library(int page,int size){
        if(page<0||page>100000||size<1||size>100)throw new IllegalArgumentException("보고서 페이지 범위를 확인하세요.");
        if(!settings.enabled())return new Library(false,null,null,null,List.of(),page,false);
        var expected=calendar.latestDue(LocalDateTime.ofInstant(clock.instant(),SEOUL));
        var rows=store.catalog(page*size,size+1,false);
        var newest=page==0?rows.stream().findFirst().orElse(null):store.catalog(0,1,false).stream().findFirst().orElse(null);
        var ready=store.catalog(0,1,true).stream().findFirst().orElse(null);
        return new Library(true,expected.status()==TradingCalendar.Status.OPEN?expected.date():null,ready,newest,
                rows.stream().limit(size).toList(),page,rows.size()>size);
    }
    public StockDecision evidence(String id,String code){run(id);if(!settings.enabled())throw conflict("일일 보고서가 비활성화되어 있습니다.");return store.evidence(id,code).orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));}
    public View request(String id){
        var run=run(id);
        if(!settings.enabled())throw conflict("일일 보고서가 비활성화되어 있습니다.");
        if(run.eod()==null)throw conflict("1차 EOD 입력이 저장된 실행에서만 보고서를 만들 수 있습니다.");
        if(!store.view(id).state().equals("NOT_CREATED"))return store.view(id);
        // A recovered failed scan still needs its failure notice after the forecast cutoff.
        long deadline=run.status().equals("FAILED")?clock.millis()+3600000:deadline(run);
        if(clock.millis()>=deadline)throw conflict("다음 거래일 장 시작 전 보고서 작성 기한이 지났습니다.");
        store.register(run,version,settings,clock.millis(),deadline);return store.view(id);
    }
    public void tick(){
        if(!settings.enabled()||!active.compareAndSet(false,true))return;
        try {
            for(String id:store.discover(clock.millis()-Duration.ofHours(24).toMillis(),clock.millis())) {
                try {request(id);}catch(BusinessException ignored){ /* Expired/unknown calendars are not guessed. */ }
            }
            store.claim(clock.millis()).ifPresent(this::process);
        } finally {active.set(false);}
    }
    private void process(ReportStore.Job job){
        var run=run(job.id());
        try {
            if(!version.equals(job.version())) {fail(job,run,"POLICY_VERSION_CHANGED");return;}
            if(clock.millis()>=job.deadline()) {fail(job,run,"REPORT_DEADLINE");return;}
            if(run.status().equals("FAILED")) {fail(job,run,"SCAN_FAILED");return;}
            if(job.attempts()>=3) {fail(job,run,"REPORT_ATTEMPTS_EXHAUSTED");return;}
            if(job.market()==null) {
                if(clock.millis()<run.eod().eligibleEpoch()) {store.defer(job,"WAITING_SCAN",null,run.eod().eligibleEpoch());return;}
                store.attempt(job);var snapshot=markets.collect(run.baseDate());validateMarket(snapshot,run.baseDate());
                if(clock.millis()>=job.deadline()) {fail(job,run,"MARKET_SNAPSHOT_TOO_LATE");return;}
                store.market(job,snapshot);store.defer(job,"WAITING_SCAN",null,clock.millis()+1000);return;
            }
            if(!Set.of("COMPLETED","COMPLETED_WITH_ERRORS").contains(run.status())) {
                store.defer(job,"WAITING_SCAN",null,clock.millis()+30000);return;
            }
            store.attempt(job);build(job,run);
        }catch(ReportStore.LostOwnership|java.util.concurrent.CancellationException e){throw e;}
        catch(RuntimeException e){
            if(job.attempts()+1>=3 || clock.millis()>=job.deadline())fail(job,run,"REPORT_DATA_OR_ANALYSIS_FAILED");
            else store.defer(job,"RETRY_WAIT","REPORT_RETRY",clock.millis()+60000L*(1L<<job.attempts()));
        }
    }
    private void build(ReportStore.Job job,Run run){
        var expected=new TreeMap<String,Long>();var all=new ArrayList<StockDecision>();
        Instant information=job.market().collectedAt();
        int index=0;
        for(var row:store.stocks(run.runId())) {
            if(index++%25==0)store.heartbeat(job);
            String code=(String)row.get("STOCK_CODE"),market=(String)row.get("MARKET"),status=(String)row.get("STATUS");
            if("SKIPPED".equals(status))continue;
            expected.merge(market,1L,Long::sum);
            if(!"COMPLETED".equals(status))continue;
            try {
                var detail=scans.detail(run.runId(),code).orElseThrow();
                if(!run.baseDate().equals(detail.result().priceDate()))throw new IllegalArgumentException("PRICE_DATE_MISMATCH");
                var marketIndex=job.market().indices().stream().filter(i->i.market().equals(market)).findFirst().orElseThrow();
                all.add(candidates.analyze(run.runId(),market,detail,store.input(run.runId(),code),marketIndex));
                if(detail.eod().collectedAt().isAfter(information))information=detail.eod().collectedAt();
            }catch(org.springframework.dao.DataAccessException e){throw e;}
            catch(RuntimeException e){
                all.add(new StockDecision(code,code,market,"EXCLUDED","INVALID_INPUT",List.of(),List.of("필수 입력 또는 충분한 가격 이력을 확인하지 못했습니다."),null,null,null,null,null,false,null,false));
            }
        }
        var valid=all.stream().filter(s->s.dailyReturn()!=null).toList();
        Set<String> required=run.market().equals("ALL")?Set.of("KOSPI","KOSDAQ"):Set.of(run.market());
        if(valid.isEmpty()){fail(job,run,"NO_VALID_STOCK_DATA",all);return;}
        var views=job.market().indices().stream().filter(i->required.contains(i.market())).map(i->marketView(i,valid,expected.getOrDefault(i.market(),0L))).toList();
        // Market context is informational; stock eligibility uses its own structure.
        var assessed=new ArrayList<>(all);
        var chosen=assessed.stream().filter(s->Set.of("ENTRY_CONDITIONAL","WATCH").contains(s.disposition())).sorted(
                CandidateRanking.comparator())
                .limit(settings.maximumCandidates()).toList();
        var chosenCodes=chosen.stream().map(StockDecision::code).collect(java.util.stream.Collectors.toSet());
        var recorded=assessed.stream().map(s->{
            if(!Set.of("ENTRY_CONDITIONAL","WATCH").contains(s.disposition())||chosenCodes.contains(s.code()))return s;
            var warnings=new ArrayList<>(s.cautions());warnings.add("표시 상한으로 최종 목록에서 제외했습니다. 조건 충족 자체는 유지됩니다.");
            return new StockDecision(s.code(),s.name(),s.market(),"RANK_LIMIT",s.strategy(),s.evidence(),warnings,s.scenario(),s.netRewardRisk(),s.relativeReturn20(),s.liquidity(),s.dailyReturn(),s.aboveEma20(),s.stage(),s.transition61(),s.newEntry());
        }).toList();
        if(clock.millis()>=job.deadline()) {fail(job,run,"REPORT_DEADLINE");return;}
        var report=new DailyReport(run.runId(),"READY",run.baseDate(),nextDate(run.baseDate()),information,clock.instant(),version,"UNVALIDATED_RULES",
                run.market(),views,chosen,valid.size(),expected.values().stream().mapToLong(Long::longValue).sum(),List.of(
                "가격·거래량·거래대금 및 확보한 수급 기반의 검증 전 관찰 보고서입니다. 승률·수익률·상승 확률을 추정하지 않습니다.",
                "시장 지수는 KRX 기준입니다. 지수·종목 자료의 실제 수집 시각이 20시보다 늦을 수 있으며 이후 정정될 수 있습니다.",
                "업종별 동반성·시장 전체 투자자 수급·뉴스·공시·다음 거래일 중요 일정은 미확인입니다.",
                "후보 순서는 진입 조건 충족 여부, 눌림 회복·돌파 재지지·6→1 전환 우대, 종목 적합도 점수, 당일 거래대금, 종목 코드 순입니다. 별도 합산 점수가 아닙니다.",
                "확보된 유효 자료로 분석합니다. 분석 비율에 따른 보고서 생성 하한은 없습니다. 표본 누락으로 시장 참여도와 선정 결과가 달라질 수 있습니다.",
                "왕복 거래비용 "+settings.roundTripCostBps()+"bp는 설정 가정입니다. 실제 수수료·세금·슬리피지와 다를 수 있습니다. 다음 거래일 갭과 체결 조건을 다시 확인하세요."));
        report=report.withTierLists(TierLists.from(assessed,settings.maximumCandidates()));
        store.complete(job,report,recorded,null,ReportMessage.format(report,telegram.publicBaseUrl()),telegram,deliveryDeadline(run));
    }
    private MarketView marketView(IndexSnapshot index,List<StockDecision> all,long expected){
        var stocks=all.stream().filter(s->s.market().equals(index.market())).toList();
        var bars=index.bars();int last=bars.size()-1;var closes=bars.stream().map(IndexBar::close).toList();
        var ema=new EmaCalculator();var ema20=ema.calculate(closes,20).get(last);var ema60=ema.calculate(closes,60).get(last);
        var close=closes.get(last);var above=stocks.isEmpty()?null:bd(stocks.stream().filter(StockDecision::aboveEma20).count()).divide(bd(stocks.size()),MathContext.DECIMAL128).multiply(bd(100));
        var advancing=stocks.isEmpty()?null:bd(stocks.stream().filter(s->s.dailyReturn().signum()>0).count()).divide(bd(stocks.size()),MathContext.DECIMAL128).multiply(bd(100));
        String bias=stocks.isEmpty()?"판단 보류":close.compareTo(ema20)>0&&ema20.compareTo(ema60)>0&&above.compareTo(bd(55))>=0&&advancing.compareTo(bd(55))>=0?"상승 우세"
                :close.compareTo(ema20)<0&&above.compareTo(bd(40))<0?"하락 우세":"중립";
        var support=bars.subList(last-19,last+1).stream().map(IndexBar::low).min(BigDecimal::compareTo).orElseThrow();
        var resistance=bars.subList(last-19,last+1).stream().map(IndexBar::high).max(BigDecimal::compareTo).orElseThrow();
        var stages=new TreeMap<Integer,Long>();stocks.stream().filter(s->s.stage()!=null).forEach(s->stages.merge(s.stage(),1L,Long::sum));
        var warnings=new ArrayList<>(index.warnings());
        var dayReturn=pct(close,closes.get(last-1));
        if(advancing!=null&&dayReturn.signum()>0&&advancing.compareTo(bd(50))<0)warnings.add("지수 상승과 종목 상승 참여가 엇갈립니다.");
        if(stocks.size()<expected)warnings.add("유효 분석 "+stocks.size()+"/"+expected+"종목 기준입니다. 전체 시장을 대표하지 않을 수 있습니다.");
        if(stocks.isEmpty())warnings.add("유효 종목 자료가 없어 시장 참여도와 추세 종합 판단을 보류합니다. 지수 가격만 참고하세요.");
        warnings.add("예측 확률이 없는 조건부 전망입니다. 다음 거래일 종가 방향 평가는 아직 축적하지 않았습니다.");
        return new MarketView(index.market(),round(dayReturn),round(pct(close,closes.get(last-20))),round(above),round(advancing),stocks.size(),expected,stages,
                stocks.stream().filter(StockDecision::transition61).count(),bias,
                "현재 판단은 "+bias+"입니다. 종가와 EMA20("+ema20.setScale(2,RoundingMode.HALF_UP)+"), 종목 참여도 관계가 유지되는지 확인합니다.",
                "최근 20봉 고점 "+resistance.toPlainString()+" 돌파와 상승 종목 비율 55% 이상이 함께 나타나면 상승 시나리오를 강화합니다.",
                "최근 20봉 저점 "+support.toPlainString()+" 이탈 또는 EMA20 아래에서 참여도 악화 시 기존 상승 판단을 취소하고 진입 검토를 줄입니다.",close,support,resistance,warnings);
    }
    private void validateMarket(MarketSnapshot snapshot,LocalDate date){
        if(snapshot==null||snapshot.indices().size()!=2||!snapshot.indices().stream().map(IndexSnapshot::market).collect(java.util.stream.Collectors.toSet()).equals(Set.of("KOSPI","KOSDAQ")))throw new IllegalArgumentException("MARKET_SNAPSHOT_INVALID");
        for(var index:snapshot.indices())if(!date.equals(index.tradeDate())||index.bars().size()<60||!date.equals(index.bars().get(index.bars().size()-1).date())||index.collectedAt().isAfter(clock.instant()))throw new IllegalArgumentException("MARKET_SNAPSHOT_INVALID");
    }
    private void fail(ReportStore.Job job,Run run,String error){
        fail(job,run,error,List.of());
    }
    private void fail(ReportStore.Job job,Run run,String error,List<StockDecision> evidence){
        var report=new DailyReport(run.runId(),"FAILED",run.baseDate(),null,clock.instant(),clock.instant(),job.version(),"NOT_EVALUATED",run.market(),List.of(),List.of(),evidence.stream().filter(s->s.dailyReturn()!=null).count(),run.total()-run.skipped(),List.of("보고서 생성에 필요한 자료 또는 실행 조건을 충족하지 못했습니다."));
        store.complete(job,report,evidence,error,ReportMessage.format(report,telegram.publicBaseUrl()),telegram,Math.max(clock.millis()+3600000,run.eod().deadlineEpoch()));
    }
    private LocalDate nextDate(LocalDate date){for(int i=1;i<=32;i++){var day=calendar.day(date.plusDays(i));if(day.status()==TradingCalendar.Status.UNKNOWN)throw conflict("다음 거래일 달력을 확인할 수 없습니다.");if(day.status()==TradingCalendar.Status.OPEN)return day.date();}throw conflict("다음 거래일 없음");}
    private long deliveryDeadline(Run run){return nextDate(run.baseDate()).atTime(9,0).atZone(SEOUL).toInstant().toEpochMilli();}
    private long deadline(Run run){return Math.min(run.eod().deadlineEpoch()+1800000,deliveryDeadline(run));}
    private Run run(String id){return scans.find(id).orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));}
    private static BusinessException conflict(String message){return new BusinessException(ErrorCode.UNIFIED_ANALYSIS_CONFLICT,message);}
    private static String hash(String value){try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))).substring(0,16);}catch(Exception e){throw new IllegalStateException(e);}}
}
