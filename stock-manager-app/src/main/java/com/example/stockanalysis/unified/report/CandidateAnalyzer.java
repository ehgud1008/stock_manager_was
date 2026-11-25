package com.example.stockanalysis.unified.report;

import static com.example.stockanalysis.unified.report.ReportModels.*;
import com.example.stockanalysis.engine.model.*;
import com.example.stockanalysis.engine.screener.EmaCalculator;
import com.example.stockanalysis.unified.application.UnifiedAnalysisResponse;
import com.example.stockanalysis.judgment.NewEntryEvaluator;
import com.example.stockanalysis.judgment.NewEntryAssessment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Rules are explicit research hypotheses, not calibrated probabilities or verified trading returns. */
@Component
public class CandidateAnalyzer {
    public static final String VERSION="candidate-v7-profitability";
    private static final MathContext MC=MathContext.DECIMAL128;
    private final ObjectMapper mapper;
    private final ReportProperties settings;
    private final boolean legacy;
    private final boolean modern;
    private final boolean profitability;
    private final String evidenceVersion;
    @org.springframework.beans.factory.annotation.Autowired
    public CandidateAnalyzer(ObjectMapper mapper,ReportProperties settings){this(mapper,settings,false,true,true);}
    private CandidateAnalyzer(ObjectMapper mapper,ReportProperties settings,boolean legacy,boolean modern,boolean profitability){this.mapper=mapper;this.settings=settings;this.legacy=legacy;this.modern=modern;this.profitability=profitability;this.evidenceVersion=legacy?"candidate-market-v4-common-entry":profitability?VERSION:modern?"candidate-v6-signals":"candidate-market-v5-tier";}
    public static CandidateAnalyzer legacyV4(ObjectMapper mapper,ReportProperties settings){return new CandidateAnalyzer(mapper,settings,true,false,false);}
    public static CandidateAnalyzer legacyV5(ObjectMapper mapper,ReportProperties settings){return new CandidateAnalyzer(mapper,settings,false,false,false);}
    public static CandidateAnalyzer legacyV6(ObjectMapper mapper,ReportProperties settings){return new CandidateAnalyzer(mapper,settings,false,true,false);}
    public StockDecision analyze(String runId,String market,UnifiedAnalysisResponse detail,JsonNode snapshot,IndexSnapshot index) {
        return analyze(runId,market,detail,snapshot,index,null);
    }
    public enum ResearchMode { SIGNAL_AGE4, PATTERN_VOLUME }
    /** Only called by saved-snapshot research. Scheduled report generation always uses the five-argument method. */
    public StockDecision analyze(String runId,String market,UnifiedAnalysisResponse detail,JsonNode snapshot,IndexSnapshot index,ResearchMode research) {
        AnalysisInput input;
        try {input=mapper.treeToValue(snapshot.path("input"),AnalysisInput.class);}
        catch(Exception e){throw new IllegalArgumentException("INVALID_SAVED_INPUT");}
        var result=detail.result();var date=result.priceDate();var code=result.stock().stockCode();
        if (detail.eod()==null || !date.equals(detail.eod().tradeDate()) || !code.equals(input.stock().stockCode())
                || !date.equals(input.baseDate()) || !date.equals(index.tradeDate())) throw new IllegalArgumentException("SAVED_INPUT_IDENTITY");
        var bars=input.dailyCandles().stream().sorted(Comparator.comparing(DailyCandle::businessDate)).toList();
        if (bars.size()<60 || bars.stream().map(DailyCandle::businessDate).distinct().count()!=bars.size()
                || !bars.get(bars.size()-1).businessDate().equals(date)) throw new IllegalArgumentException("INSUFFICIENT_CANDIDATE_HISTORY");
        var prices=bars.stream().map(DailyCandle::close).toList();int last=prices.size()-1;
        var ema=new EmaCalculator();var fast=ema.calculate(prices,5);var middle=ema.calculate(prices,20);var slow=ema.calculate(prices,40);
        var close=prices.get(last);var shortSlope=pct(fast.get(last),fast.get(last-3));
        if(close.compareTo(result.stock().currentPrice())!=0) throw new IllegalArgumentException("SAVED_PRICE_MISMATCH");
        var middleSlope=pct(middle.get(last),middle.get(last-3));var longSlope=pct(slow.get(last),slow.get(last-3));
        var acceleration=shortSlope.subtract(pct(fast.get(last-1),fast.get(last-4)));
        var gap=pct(fast.get(last),middle.get(last));var gapChange=gap.subtract(pct(fast.get(last-1),middle.get(last-1)));
        var distance=pct(close,middle.get(last));
        var volumeMean=mean(bars.subList(last-20,last).stream().map(b->BigDecimal.valueOf(b.volume())).toList());
        var volumeRatio=volumeMean.signum()>0?BigDecimal.valueOf(bars.get(last).volume()).divide(volumeMean,MC):null;
        var indexByDate=new HashMap<LocalDate,BigDecimal>();index.bars().forEach(b->indexByDate.put(b.date(),b.close()));
        var reference=indexByDate.get(bars.get(last-20).businessDate());
        var relative=reference==null?null:pct(close,prices.get(last-20)).subtract(pct(indexByDate.get(date),reference));
        var amounts=amounts(snapshot,code,date);
        var amountToday=amounts.get(date);
        var priorAmounts=bars.subList(last-20,last).stream().map(b->amounts.get(b.businessDate())).toList();
        var averageAmount=priorAmounts.stream().allMatch(Objects::nonNull)?mean(priorAmounts):null;
        var screen=result.screener();
        boolean transition=Objects.equals(screen.stage(),1) && Objects.equals(screen.previousDistinctStage(),6)
                && screen.barsSinceTransition()!=null && screen.barsSinceTransition()<=3;
        var signals=result.analysis().signalReport()==null?List.<TradeSignal>of():result.analysis().signalReport().signals();
        boolean sell=signals.stream().anyMatch(s->s.side()==TradeSignal.Side.SELL && active(s) && !"POSITION_STOP".equals(s.type()));
        var evaluator=!modern?NewEntryEvaluator.legacyTier(research==ResearchMode.SIGNAL_AGE4?4:3)
                :!profitability?NewEntryEvaluator.legacyV6(research==ResearchMode.SIGNAL_AGE4?4:3)
                :research==ResearchMode.SIGNAL_AGE4?NewEntryEvaluator.researchAge4():new NewEntryEvaluator();
        var common=legacy?evaluator.evaluateLegacy(result.analysis(),close,date,
                result.asOf().atZone(ZoneId.of("Asia/Seoul")).toInstant(),detail.collectionQuality()==null?null:detail.collectionQuality().name())
                :evaluator.evaluate(result.analysis(),close,date,
                result.asOf().atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                detail.collectionQuality()==null?null:detail.collectionQuality().name());
        var buy=Optional.ofNullable(common.selectedSignal());
        var scenario=common.effectiveScenario();
        BigDecimal netRR=NewEntryEvaluator.rewardRisk(scenario,settings.roundTripCostBps());
        boolean inRange=scenario!=null && buy.isPresent();
        Boolean volumePassed=ge(volumeRatio,new BigDecimal("1.2"));
        BigDecimal signalVolumeRatio=null;
        if(modern || research==ResearchMode.PATTERN_VOLUME) {
            volumePassed=null;
            if(buy.isPresent()) {
                var signal=buy.orElseThrow();int event=-1;
                for(int i=0;i<bars.size();i++)if(bars.get(i).businessDate().equals(signal.occurredOn()))event=i;
                if(event>=20) {
                    boolean breakout="BREAKOUT".equals(signal.type());
                    var denominator=breakout?mean(bars.subList(event-20,event).stream().map(b->BigDecimal.valueOf(b.volume())).toList()):BigDecimal.valueOf(bars.get(event-1).volume());
                    signalVolumeRatio=denominator.signum()>0?BigDecimal.valueOf(bars.get(event).volume()).divide(denominator,MC):null;
                    volumePassed=signalVolumeRatio==null?null:bars.get(last).volume()>0&&signalVolumeRatio.compareTo(breakout?new BigDecimal(profitability?"1.5":"1.2"):BigDecimal.ONE)>=0;
                }
            }
        }
        String source="ua_eod_item/"+runId+"/"+code;
        Instant observed=detail.eod().collectedAt();var evidence=new ArrayList<Evidence>();
        var score=result.analysis().totalScore();
        evidence.add(fact("COMMON_ENTRY","공통 신규 진입 판단",null,"",common.policyVersion(),null,
                common.status()==NewEntryAssessment.Status.ELIGIBLE,"GATE",source,date,observed));
        if(!legacy) {
            evidence.add(fact("ENTRY_TIMING","점수 외 공통 진입 조건",null,"",common.policyVersion(),null,
                    common.timing().status()==NewEntryAssessment.Status.ELIGIBLE,"SUPPORT",source,date,observed));
            boolean qualifiedB=profitability && NewEntryEvaluator.qualifiedTierB(result.analysis(),common);
            evidence.add(fact("OPERATING_TIER",profitability?"운영 선정 A등급 또는 조건부 B등급":"운영 선정 A등급",score,"점",profitability?"A>=80 또는 B>=75·눌림 회복/재지지·총손익비>=2·추세>=70":com.example.stockanalysis.judgment.SuitabilityTier.VERSION,
                    qualifiedB?new BigDecimal("75"):NewEntryEvaluator.MIN_SCORE,"A".equals(common.suitability().tier()) || qualifiedB,"SUPPORT",source,date,observed));
        }
        var openingPrice=bars.get(last).open();
        evidence.add(fact("OPEN_PRICE","분석 기준일 시가",openingPrice,"원","시가 > 2,000원 (이하 제외)",
                com.example.stockanalysis.unified.application.UnifiedTradeEligibility.MINIMUM_OPEN_EXCLUSIVE,
                com.example.stockanalysis.unified.application.UnifiedTradeEligibility.eligible(openingPrice),"GATE",source,date,observed));
        boolean bullish=Objects.equals(screen.stage(),1) && close.compareTo(fast.get(last))>=0
                && fast.get(last).compareTo(middle.get(last))>0 && middle.get(last).compareTo(slow.get(last))>0;
        evidence.add(fact("BULLISH_STRUCTURE","1단계·상승 정배열·EMA5 위 종가",close,"원","stage=1, 종가>=EMA5>EMA20>EMA40",null,bullish,"GATE",source,date,observed));
        evidence.add(fact("SWING_SCORE","스윙 적합도 80점 이상",score,"점","공통 신규 진입 판단에 포함; 독립 가산하지 않음",NewEntryEvaluator.MIN_SCORE,ge(score,NewEntryEvaluator.MIN_SCORE),"SUPPORT",source,date,observed));
        boolean buyAction=result.analysis().strategy()!=null && result.analysis().strategy().action()==StrategyAction.BUY_CANDIDATE;
        evidence.add(fact("SWING_ACTION","종합분석 매수 검토 판정",null,"","공통 신규 진입 판단에 포함; 독립 가산하지 않음",null,buyAction,"SUPPORT",source,date,observed));
        var absoluteReturn=pct(close,prices.get(last-20));
        evidence.add(fact("POSITIVE_RETURN20","종목 자체 20봉 상승",absoluteReturn,"%","시장보다 덜 하락한 종목 제외: 20봉 수익률>0",BigDecimal.ZERO,absoluteReturn.signum()>0,"GATE",source,date,observed));
        evidence.add(fact("STAGE_6_TO_1","6→1 전환 후 3봉 이내",bd(screen.barsSinceTransition()),"봉","previousDistinctStage=6, stage=1, age<=3",bd(3),transition,"SELECTION",source,date,observed));
        evidence.add(fact("EMA5_SLOPE_3","단기 EMA5의 3봉 변화율",shortSlope,"%","3봉 전 EMA5 대비",BigDecimal.ZERO,shortSlope.signum()>0,"SELECTION",source,date,observed));
        evidence.add(fact("EMA20_SLOPE_3","중기 EMA20의 3봉 변화율",middleSlope,"%","3봉 전 EMA20 대비",BigDecimal.ZERO,middleSlope.signum()>0,"SELECTION",source,date,observed));
        evidence.add(fact("EMA40_SLOPE_3","장기 EMA40의 3봉 변화율",longSlope,"%","3봉 전 EMA40 대비",BigDecimal.ZERO,longSlope.signum()>0,"SUPPORT",source,date,observed));
        evidence.add(fact("EMA5_ACCELERATION","단기 이평선 기울기 증가",acceleration,"%p","오늘 3봉 변화율 - 전일 3봉 변화율",BigDecimal.ZERO,acceleration.signum()>0,"SUPPORT",source,date,observed));
        evidence.add(fact("EMA5_20_GAP","단기·중기 이격",gap,"%","EMA5 / EMA20 - 1",null,null,"SUPPORT",source,date,observed));
        evidence.add(fact("EMA5_20_EXPANSION","상승 방향 이격 확대",gapChange,"%p","이격>0, 전일 대비 확대, EMA5 기울기>0",BigDecimal.ZERO,gap.signum()>0&&gapChange.signum()>0&&shortSlope.signum()>0,"SUPPORT",source,date,observed));
        evidence.add(fact("PRICE_EMA20_DISTANCE","가격 과열 제한",distance,"%","종가 / EMA20 - 1 <= 상한",settings.maximumEma20Distance(),distance.compareTo(settings.maximumEma20Distance())<=0,"GATE",source,date,observed));
        evidence.add(fact("AMOUNT_TODAY","당일 실제 거래대금",amountToday,"백만원","ka10081 해당 거래일 >= 하한",settings.minimumDailyAmountMillion(),ge(amountToday,settings.minimumDailyAmountMillion()),"GATE",source,date,observed));
        evidence.add(fact("AMOUNT_AVERAGE20","직전 20거래일 평균 거래대금",averageAmount,"백만원","당일 제외, 20일 모두 확보 >= 하한",settings.minimumAverageAmountMillion(),ge(averageAmount,settings.minimumAverageAmountMillion()),"GATE",source,date,observed));
        evidence.add(fact("VOLUME_RATIO20","거래량 동반",volumeRatio,"배","당일 / 직전 20일 평균; 진입 후보 1.2 이상",new BigDecimal("1.2"),ge(volumeRatio,new BigDecimal("1.2")),(modern || research==ResearchMode.PATTERN_VOLUME)?"SUPPORT":"SELECTION",source,date,observed));
        if(modern || research==ResearchMode.PATTERN_VOLUME)evidence.add(fact(modern?"PATTERN_VOLUME":"RESEARCH_VOLUME",modern?"패턴별 발생일 거래량":"연구용 패턴별 발생일 거래량",signalVolumeRatio,"배",
                "돌파: 발생일/직전20일>="+(profitability?"1.5":"1.2")+"; 회복·재지지: 발생일/전일>=1; 선정일 거래량>0",null,volumePassed,"SELECTION",source,date,observed));
        if(profitability) {
            Boolean closePassed=null;
            if(buy.isPresent()) {
                var signal=buy.orElseThrow();
                var event=bars.stream().filter(b->b.businessDate().equals(signal.occurredOn())).findFirst().orElse(null);
                if(!"BREAKOUT".equals(signal.type()))closePassed=true;
                else if(event!=null) {
                    var range=event.high().subtract(event.low());
                    closePassed=range.signum()>0 && event.close().subtract(event.low()).compareTo(range.multiply(new BigDecimal("0.6")))>=0;
                }
            }
            evidence.add(fact("BREAKOUT_CLOSE","돌파 봉 종가 위치",null,"","단순 돌파는 (종가-저가)/(고가-저가)>=0.6; 회복·재지지는 별도 패턴 확인",new BigDecimal("0.6"),closePassed,"GATE",source,date,observed));
        }
        evidence.add(fact("RELATIVE_RETURN20","동일 기간 시장 대비 강도",relative,"%p","종목 20봉 수익률 - 같은 시작·종료일 시장 수익률",BigDecimal.ZERO,ge(relative,BigDecimal.ZERO),modern?"SUPPORT":"GATE",source,date,observed));
        evidence.add(fact("NO_SELL_CONFLICT","유효 매도 신호 없음",sell?BigDecimal.ONE:BigDecimal.ZERO,"여부","확정·유지 중 매도 신호 없어야 함",BigDecimal.ZERO,!sell,"GATE",source,date,observed));
        evidence.add(fact("ENTRY_RANGE","신호·시나리오 교집합 진입 구간",close,"원","공통 판단이 선택한 신호와 시나리오의 교집합 내 종가",null,modern && common.status()==NewEntryAssessment.Status.DEFER && buy.isEmpty()?null:inRange,"SELECTION",source,date,observed));
        evidence.add(fact("BUY_PATTERN","확정·유지 매수 패턴: "+buy.map(s->switch(s.type()){case "BREAKOUT"->"돌파";case "PULLBACK_RECOVERY"->"눌림 회복";default->"돌파 재지지";}).orElse(modern && common.status()==NewEntryAssessment.Status.DEFER?"평가 보류":"없음"),
                buy.map(s->bd(s.ageBars())).orElse(null),"봉","공통 판단: 발생 후 "+(research==ResearchMode.SIGNAL_AGE4?4:NewEntryEvaluator.MAX_SIGNAL_AGE)+"봉 이내, 신호 자체 만료 전",bd(research==ResearchMode.SIGNAL_AGE4?4:NewEntryEvaluator.MAX_SIGNAL_AGE),modern && common.status()==NewEntryAssessment.Status.DEFER && buy.isEmpty()?null:buy.isPresent(),"SELECTION",source,date,observed));
        evidence.add(fact("NET_REWARD_RISK","비용 가정 반영 손익비",netRR,"배","진입 상단·첫 목표·손절, 왕복 비용 "+settings.roundTripCostBps()+"bp; 체결 보장 아님",settings.minimumNetRewardRisk(),ge(netRR,settings.minimumNetRewardRisk()),"SELECTION",source,date,observed));
        var trueRanges=new ArrayList<BigDecimal>();
        for(int i=last-13;i<=last;i++) trueRanges.add(bars.get(i).high().subtract(bars.get(i).low())
                .max(bars.get(i).high().subtract(prices.get(i-1)).abs()).max(bars.get(i).low().subtract(prices.get(i-1)).abs()));
        evidence.add(fact("ATR14_PERCENT","최근 14봉 평균 진폭",mean(trueRanges).divide(close,MC).multiply(bd(100)),"%","단순 평균 TR14 / 종가; 변동 위험 참고",null,null,"RISK",source,date,observed));
        evidence.add(fact("GAP_OPEN","전일 대비 시가 갭",pct(bars.get(last).open(),prices.get(last-1)),"%","당일 시가 / 전일 종가 - 1",null,null,"RISK",source,date,observed));
        addFlow(evidence,snapshot,code,date,observed,source,bars.subList(last-4,last+1));
        var cautions=new ArrayList<String>();
        if(profitability) result.analysis().warnings().stream().filter(w->w.startsWith("SYNTHETIC_TARGET:"))
                .forEach(w->cautions.add(w.substring("SYNTHETIC_TARGET: ".length())));
        if(common.status()!=NewEntryAssessment.Status.ELIGIBLE) common.reasons().forEach(r->cautions.add("선정 제외: "+r));
        cautions.add("검증 전 규칙 기반 후보입니다. 다음 거래일 진입 조건 확인이 필요하며 당일 종가 체결을 가정하지 않습니다.");
        cautions.add("업종 동반성·공시/실적 일정·시장 전체 수급은 이번 보고서에서 확인하지 않았습니다.");
        if (!detail.collectionWarnings().isEmpty()) cautions.add("일부 수집 자료에 경고가 있습니다. 저장된 종합분석 상세를 확인하세요.");
        boolean gates=evidence.stream().filter(e->e.role().equals("GATE")).allMatch(e->Boolean.TRUE.equals(e.passed()));
        boolean rising=shortSlope.signum()>0 && middleSlope.signum()>0;
        boolean entry=gates && rising && inRange && Boolean.TRUE.equals(volumePassed)
                && Boolean.TRUE.equals(ge(netRR,settings.minimumNetRewardRisk()));
        String disposition=entry?"ENTRY_CONDITIONAL":"EXCLUDED";
        String strategy=entry?buy.orElseThrow().type():"NONE";
        if(!legacy && !entry) {
            boolean otherGates=evidence.stream().filter(e->e.role().equals("GATE")&&!e.code().equals("COMMON_ENTRY"))
                    .allMatch(e->Boolean.TRUE.equals(e.passed()));
            boolean comparison="B".equals(common.suitability().tier())&&common.timing().status()==NewEntryAssessment.Status.ELIGIBLE
                    &&otherGates&&rising&&inRange&&Boolean.TRUE.equals(volumePassed)&&Boolean.TRUE.equals(ge(netRR,settings.minimumNetRewardRisk()));
            if(comparison){disposition="RESEARCH_B";strategy=buy.orElseThrow().type();}
            else if(Set.of("A","B").contains(common.suitability().tier())&&common.timing().signalWaiting())disposition="SIGNAL_WAIT";
        }
        if(!entry) evidence.stream().filter(e->!e.role().equals("SUPPORT")&&!e.role().equals("RISK")&&!e.code().equals("STAGE_6_TO_1")&&!Boolean.TRUE.equals(e.passed()))
                .forEach(e->cautions.add("선정 제외: "+e.label()+" ("+(e.passed()==null?"미확인":"미충족")+")"));
        var classified=evidence.stream().map(e->{
            String role=e.role();
            if(e.code().equals("STAGE_6_TO_1"))role="SUPPORT";
            return new Evidence(e.code(),e.label(),e.value(),e.unit(),e.comparison(),e.threshold(),e.passed(),e.direction(),role,e.quality(),e.source(),e.tradeDate(),e.observedAt(),e.version());
        }).toList();
        return new StockDecision(code,result.stock().stockName(),market,disposition,strategy,classified,List.copyOf(cautions),
                scenario,round(netRR),round(relative),amountToday,round(pct(close,prices.get(last-1))),close.compareTo(middle.get(last))>0,screen.stage(),transition,common);
    }
    private void addFlow(List<Evidence> out,JsonNode snapshot,String code,LocalDate date,Instant observed,String source,List<DailyCandle> last5) {
        for(String field:List.of("foreigner","institution")) {
            var values=new HashMap<LocalDate,BigDecimal>();var conflicts=new HashSet<LocalDate>();
            for(var s:snapshot.path("sources")) if("ka10059".equals(s.path("apiId").asText()) && "2".equals(s.path("request").path("amt_qty_tp").asText())
                    && "1".equals(s.path("request").path("unit_tp").asText()) && "0".equals(s.path("request").path("trde_tp").asText()))
                for(var r:s.path("records")) if(code.equals(r.path("stockCode").asText())) {
                    try {var day=mapper.convertValue(r.path("businessDate"),LocalDate.class);var v=decimal(r.path("values").path(field).asText());
                        if(v!=null && values.putIfAbsent(day,v)!=null) conflicts.add(day);
                    } catch(RuntimeException ignored) { /* Missing source values stay unknown. */ }
                }
            BigDecimal sum=BigDecimal.ZERO;long volume=0;
            for(var b:last5) {if(!values.containsKey(b.businessDate())||conflicts.contains(b.businessDate())) {sum=null;break;}sum=sum.add(values.get(b.businessDate()));volume=Math.addExact(volume,b.volume());}
            var ratio=sum==null||volume==0?null:sum.divide(BigDecimal.valueOf(volume),MC).multiply(bd(100));
            out.add(fact(field.equals("foreigner")?"FOREIGN_FLOW5":"INSTITUTION_FLOW5",field.equals("foreigner")?"외국인 5일 순매수/거래량":"기관 5일 순매수/거래량",ratio,"%","ka10059 수량 기준, 동일 5거래일 합계/거래량",BigDecimal.ZERO,ratio==null?null:ratio.signum()>0,"SUPPORT",source,date,observed));
        }
    }
    private Map<LocalDate,BigDecimal> amounts(JsonNode snapshot,String code,LocalDate cutoff) {
        var values=new HashMap<LocalDate,BigDecimal>();var conflicts=new HashSet<LocalDate>();
        for(var source:snapshot.path("sources")) if("ka10081".equals(source.path("apiId").asText()))
            for(var row:source.path("records")) if(code.equals(row.path("stockCode").asText())) {
                try {var date=mapper.convertValue(row.path("businessDate"),LocalDate.class);var value=decimal(row.path("values").path("tradingAmount").asText());
                    if(!date.isAfter(cutoff)) {
                        if(value==null||value.signum()<0) conflicts.add(date);
                        else {var old=values.putIfAbsent(date,value);if(old!=null&&old.compareTo(value)!=0)conflicts.add(date);}
                    }
                }catch(RuntimeException ignored){ /* Invalid source row cannot satisfy liquidity gates. */ }
            }
        conflicts.forEach(values::remove);return values;
    }
    private Evidence fact(String code,String label,BigDecimal value,String unit,String comparison,BigDecimal threshold,Boolean passed,String role,String source,LocalDate date,Instant observed) {
        return new Evidence(code,label,round(value),unit,comparison,threshold,passed,passed==null?"NEUTRAL":passed?"POSITIVE":"NEGATIVE",role,value==null&&passed==null?"UNKNOWN":"OBSERVED",source,date,observed,evidenceVersion);
    }
    static boolean active(TradeSignal signal){return signal.state()==TradeSignal.State.CONFIRMED||signal.state()==TradeSignal.State.ACTIVE;}
    static BigDecimal decimal(String raw){try{return raw==null||raw.isBlank()?null:new BigDecimal(raw.replace(",",""));}catch(NumberFormatException e){return null;}}
    static BigDecimal mean(List<BigDecimal> values){return values.stream().reduce(BigDecimal.ZERO,BigDecimal::add).divide(bd(values.size()),MC);}
    static BigDecimal pct(BigDecimal current,BigDecimal prior){return current.subtract(prior).divide(prior,MC).multiply(bd(100));}
    static BigDecimal bd(Number value){return value==null?null:new BigDecimal(value.toString());}
    static BigDecimal round(BigDecimal value){return value==null?null:value.setScale(6,RoundingMode.HALF_UP);}
    static Boolean ge(BigDecimal value,BigDecimal minimum){return value==null?null:value.compareTo(minimum)>=0;}
}
