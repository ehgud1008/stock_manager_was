package com.example.stockanalysis.decision;

import static com.example.stockanalysis.decision.DecisionModels.*;
import com.example.stockanalysis.engine.model.*;
import com.example.stockanalysis.engine.factor.internal.RiskStabilityFactorCalculator;
import com.example.stockanalysis.unified.application.UnifiedAnalysisResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Observed price structure only. The volatility screen is a heuristic, never a probability or target generator. */
public final class HorizonPlanCalculator {
    public static final String VERSION="horizon-structure-v3-profitability";
    private final DecisionJson json;
    public HorizonPlanCalculator(DecisionJson json) {this.json=json;}
    record Level(BigDecimal price,LocalDate date,String basis) {}

    public ObjectNode calculate(UnifiedAnalysisResponse data,ReviewRequest context) {
        int horizon=context.horizonTradingDays(),lookback=Math.min(252,Math.max(20,horizon*4));
        var current=data.result().stock().currentPrice();var date=data.result().priceDate();
        var out=json.tree(Map.of("version",VERSION,"horizonTradingDays",horizon,"lookbackBars",lookback,
                "focus",horizon<=5?"단기 가격 구조":horizon<=20?"일봉 추세와 가격 구조":"중기 가격 구조와 완료 주봉",
                "method","관측된 미이탈 피벗과 돌파 가격 사용. ATR 참고 범위는 가격 예측이나 도달 확률이 아닙니다."));
        ObjectNode p=(ObjectNode)out,metrics=json.tree(Map.of()).deepCopy();p.set("metrics",metrics);p.putNull("plan");
        metric(metrics,"CURRENT","분석 기준 종가",current,"원","저장 분석 가격일 "+date);
        metric(metrics,"HORIZON",context.positionStatus()==Position.HELD?"잔여 보유기간":"예정 보유기간",horizon,"거래일","사용자 입력");
        if(context.quantity()!=null && context.averageBuyPrice()!=null && context.positionStatus()==Position.HELD) {
            var quantity=context.quantity();var cost=context.averageBuyPrice().multiply(quantity);
            metric(metrics,"QUANTITY","보유수량",quantity,"주","분석 요청에 고정된 잔고");
            metric(metrics,"COST_AMOUNT","매입금액",cost,"원","비용 제외");
            metric(metrics,"VALUE_AMOUNT","평가금액",current.multiply(quantity),"원","저장 분석 가격 기준");
            metric(metrics,"PNL_AMOUNT","평가손익금액",current.multiply(quantity).subtract(cost),"원","비용 제외");
        }
        var bars=data.priceHistory().stream().filter(b->!b.businessDate().isAfter(date)).sorted(Comparator.comparing(DailyCandle::businessDate)).toList();
        if(bars.isEmpty()) return unavailable(p,"HISTORY_REQUIRED","가격 이력이 없는 저장 결과입니다. 종합분석을 다시 실행하세요.");
        if(bars.stream().map(DailyCandle::businessDate).distinct().count()!=bars.size()
                || !bars.get(bars.size()-1).businessDate().equals(date) || bars.get(bars.size()-1).close().compareTo(current)!=0)
            return unavailable(p,"HISTORY_INVALID","저장 가격 이력과 분석 가격일·종가가 일치하지 않습니다. 재분석이 필요합니다.");
        p.put("historyFrom",bars.get(0).businessDate().toString());p.put("historyTo",date.toString());p.put("historyBars",bars.size());
        if(bars.size()<lookback+1) return unavailable(p,"HISTORY_INSUFFICIENT","선택 기간의 가격 구조를 검토할 확정 일봉이 부족합니다.");
        int end=bars.size()-1,start=Math.max(2,end-lookback);
        BigDecimal atr=RiskStabilityFactorCalculator.averageTrueRange(bars,14);
        if(atr.signum()<=0) return unavailable(p,"VOLATILITY_UNAVAILABLE","변동폭이 없어 유효한 가격 계획을 산출하지 않습니다.");
        metric(metrics,"ATR","평균 일봉 변동폭",round(atr),"원","확정 일봉 ATR, 최근 14개 구간");
        List<Level> supports=new ArrayList<>(),resistances=new ArrayList<>();
        for(int i=start;i<=end-2;i++) {
            var pivot=bars.get(i);boolean low=true,high=true,lowLeft=false,lowRight=false,highLeft=false,highRight=false;
            for(int j=i-2;j<=i+2;j++) {low &= pivot.low().compareTo(bars.get(j).low())<=0;high &= pivot.high().compareTo(bars.get(j).high())>=0;}
            for(int j=i-2;j<i;j++) {lowLeft |= pivot.low().compareTo(bars.get(j).low())<0;highLeft |= pivot.high().compareTo(bars.get(j).high())>0;}
            for(int j=i+1;j<=i+2;j++) {lowRight |= pivot.low().compareTo(bars.get(j).low())<0;highRight |= pivot.high().compareTo(bars.get(j).high())>0;}
            low &= lowLeft && lowRight;high &= highLeft && highRight;
            for(int j=i+1;j<=end;j++) {low &= bars.get(j).close().compareTo(pivot.low())>=0;high &= bars.get(j).close().compareTo(pivot.high())<=0;}
            if(low && pivot.low().compareTo(current)<0) supports.add(new Level(pivot.low(),pivot.businessDate(),"좌우 확정 일봉으로 확인되고 이후 종가가 이탈하지 않은 피벗 저점"));
            if(high && pivot.high().compareTo(current)>0) resistances.add(new Level(pivot.high(),pivot.businessDate(),"좌우 확정 일봉으로 확인되고 이후 종가가 돌파하지 않은 피벗 고점"));
        }
        var priorHigh=bars.subList(end-lookback,end).stream().max(Comparator.comparing(DailyCandle::high)).orElseThrow();
        boolean breakout=current.compareTo(priorHigh.high())>0;
        Level support=breakout?new Level(priorHigh.high(),priorHigh.businessDate(),"선택 관측 구간의 직전 고점 돌파선")
                :supports.isEmpty()?null:supports.get(supports.size()-1);
        var targets=resistances.stream().map(l->new Level(round(l.price()),l.date(),l.basis())).filter(l->l.price().compareTo(current)>0)
                .sorted(Comparator.comparing(Level::price).thenComparing(Level::date,Comparator.reverseOrder()))
                .collect(java.util.stream.Collectors.toMap(Level::price,l->l,(a,b)->a,LinkedHashMap::new)).values().stream().limit(2).toList();
        p.set("support",json.tree(support));p.set("targetLevels",json.tree(targets));p.put("breakout",breakout);
        p.set("weeklyTrend",weekly(bars,date));
        p.put("periodReturnPct",percent(current.subtract(bars.get(end-horizon).close()),bars.get(end-horizon).close()));
        var report=data.result().analysis().signalReport();
        int signalRemaining=report==null?horizon:report.signals().stream()
                .filter(s->s.state()==TradeSignal.State.ACTIVE||s.state()==TradeSignal.State.CONFIRMED)
                .mapToInt(s->Math.max(1,s.validForBars()-s.ageBars())).min().orElse(horizon);
        int reviewAfter=Math.min(horizon,signalRemaining);
        metric(metrics,"REVIEW_AFTER","늦어도 재검토할 시점",reviewAfter,"거래일 후","저장 가격일 이후. 신호 유효기간과 잔여 기간 중 먼저 도래하는 시점");
        p.put("timeRule","목표 미도달 상태에서 검토 시점이 오면 신호·추세를 재평가합니다. 기간 만료는 자동 매도 지시가 아닙니다.");
        p.put("signalExpiresBeforeHorizon",signalRemaining<horizon);
        if(context.averageBuyPrice()!=null) {
            metric(metrics,"AVG_BUY","평균 매수가",context.averageBuyPrice(),"원","사용자 입력, 수수료 제외");
            metric(metrics,"PNL_PCT","매수가 대비 평가손익",percent(current.subtract(context.averageBuyPrice()),context.averageBuyPrice()),"%","저장 종가 기준, 수수료·세금 제외");
        }
        if(context.purchasedOn()!=null) {
            boolean covered=!context.purchasedOn().isBefore(bars.get(0).businessDate());p.put("holdingAgeAvailable",covered);
            if(covered) metric(metrics,"HELD_BARS","매수일 이후 경과 거래일",bars.stream().filter(b->b.businessDate().isAfter(context.purchasedOn())).count(),"거래일","저장 일봉에서 집계. 잔여 보유기간과 별개");
        }
        if(support==null) return unavailable(p,"NO_SUPPORT","선택 관측 구간에 확인된 미이탈 지지가 없습니다.");
        if(targets.isEmpty()) return unavailable(p,"NO_RESISTANCE","선택 관측 구간에 확인된 상단 저항이 없어 목표가를 만들지 않습니다.");
        BigDecimal entryTo=com.example.stockanalysis.engine.scenario.ProfitabilityEntryPolicy.upperEntry(current,atr),entryFrom=round(current.subtract(atr.multiply(new BigDecimal("0.25"))).max(support.price())).min(current);
        if(targets.get(0).price().compareTo(entryTo)<=0)
            return unavailable(p,"INVALID_PRICE_ORDER","진입 상단 버퍼가 첫 관측 저항에 도달하여 가격 계획을 보류합니다.");
        BigDecimal stop=round(support.price().subtract(atr.multiply(new BigDecimal(breakout?"0.50":"0.25"))));
        var prices=targets.stream().map(l->round(l.price())).distinct().filter(v->v.compareTo(entryTo)>0).toList();
        if(stop.signum()<=0 || stop.compareTo(entryFrom)>=0 || entryFrom.compareTo(entryTo)>0 || prices.isEmpty())
            return unavailable(p,"INVALID_PRICE_ORDER","반올림 후 진입·손절·목표 가격 순서가 성립하지 않습니다.");
        p.put("status","AVAILABLE");p.put("reason","기간별 관측 구간에서 확인한 가격 구조입니다. 같은 지지가 유효하면 기간을 바꿔도 손절선은 같을 수 있습니다.");
        p.set("plan",json.tree(new TradeScenario(entryFrom,entryTo,prices,stop,TradeScenario.TargetBasis.OBSERVED_RESISTANCE)));
        metric(metrics,"ENTRY_LOW","진입 하단",entryFrom,"원","현재 종가에서 ATR 일부를 차감하되 확인된 지지보다 낮추지 않음");
        metric(metrics,"ENTRY_HIGH","진입 상단",entryTo,"원","분석 기준 종가 + min(0.20 ATR, 종가의 1.5%). 손익비는 이 상단 기준");
        metric(metrics,"STOP","구조적 손절 참고선",stop,"원",support.basis()+" ("+support.date()+"), ATR 완충 적용");
        if(context.quantity()!=null && context.positionStatus()==Position.HELD) {
            metric(metrics,"ADDITIONAL_RISK_AMOUNT","현재부터 손절선까지 추가 하락금액",current.subtract(stop).max(BigDecimal.ZERO).multiply(context.quantity()),"원","체결 보장 아님. 비용 제외");
            if(context.averageBuyPrice()!=null) metric(metrics,"STOP_PNL_AMOUNT","손절선 기준 손익금액",stop.subtract(context.averageBuyPrice()).multiply(context.quantity()),"원","평균 매입단가 대비, 비용 제외");
        }
        BigDecimal reference=context.positionStatus()==Position.HELD?current:entryTo;
        p.put("riskReference",context.positionStatus()==Position.HELD?"CURRENT_PRICE":"ENTRY_UPPER");
        metric(metrics,"LOSS_PCT","손절선까지 하락 폭",percent(reference.subtract(stop),reference),"%","보유 중은 현재 종가, 미보유는 진입 상단 기준. 갭·비용 미반영");
        for(int i=0;i<prices.size();i++) {
            String suffix=i==0?"ONE":"TWO";BigDecimal target=prices.get(i);
            if(context.quantity()!=null && context.averageBuyPrice()!=null && context.positionStatus()==Position.HELD)
                metric(metrics,"TARGET_PNL_"+suffix,(i+1)+"차 목표 기준 손익금액",target.subtract(context.averageBuyPrice()).multiply(context.quantity()),"원","목표 도달 가정, 비용 제외");
            metric(metrics,"TARGET_"+suffix,(i+1)+"차 목표",target,"원",targets.get(i).basis()+" ("+targets.get(i).date()+")");
            metric(metrics,"UPSIDE_"+suffix,(i+1)+"차 목표까지 상승 여력",percent(target.subtract(reference),reference),"%","목표 도달을 가정한 거리이며 기대수익률이 아님");
            metric(metrics,"RR_"+suffix,(i+1)+"차 목표 손익비",target.subtract(reference).divide(reference.subtract(stop),2,RoundingMode.HALF_UP),"배","현재 시점의 상승 여력 / 손절 거리, 비용 제외");
        }
        BigDecimal screen=atr.multiply(BigDecimal.valueOf(StrictMath.sqrt(horizon)));
        metric(metrics,"VOLATILITY_SCREEN","기간별 변동폭 참고치",round(screen),"원","ATR × 기간의 제곱근. 검증되지 않은 거리 비교용이며 확률 구간이 아님");
        p.put("distanceAssessment",prices.get(0).subtract(reference).compareTo(screen)>0?"STRETCHED":"WITHIN_REFERENCE");
        p.put("distanceExplanation","목표까지 거리를 변동폭 참고치와 비교합니다. 도달 가능성이나 승률을 의미하지 않습니다.");
        return p;
    }
    private JsonNode weekly(List<DailyCandle> bars,LocalDate date) {
        Map<LocalDate,BigDecimal> weeks=new TreeMap<>();
        for(var b:bars) {var friday=b.businessDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));if(!friday.isAfter(date)) weeks.put(friday,b.close());}
        var closes=new ArrayList<>(weeks.values());if(closes.size()<13) return json.tree(Map.of("status","INSUFFICIENT","completedWeeks",closes.size()));
        BigDecimal shortMean=mean(closes.subList(closes.size()-5,closes.size())),longMean=mean(closes.subList(closes.size()-13,closes.size()));
        return json.tree(Map.of("status",shortMean.compareTo(longMean)>=0?"SUPPORTIVE":"WEAK","completedWeeks",closes.size(),"mean5",round(shortMean),"mean13",round(longMean),
                "basis","금요일이 분석 기준일을 넘지 않는 완료 주의 마지막 저장 종가"));
    }
    private BigDecimal mean(List<BigDecimal> values) {return values.stream().reduce(BigDecimal.ZERO,BigDecimal::add).divide(BigDecimal.valueOf(values.size()),8,RoundingMode.HALF_UP);}
    private ObjectNode unavailable(ObjectNode p,String status,String reason) {p.put("status",status);p.put("reason",reason);return p;}
    private BigDecimal round(BigDecimal value) {return value.setScale(0,RoundingMode.HALF_UP);}
    private BigDecimal percent(BigDecimal change,BigDecimal base) {return change.multiply(BigDecimal.valueOf(100)).divide(base,2,RoundingMode.HALF_UP);}
    private void metric(ObjectNode metrics,String id,String label,Object value,String unit,String basis) {metrics.set(id,json.tree(Map.of("label",label,"value",value,"unit",unit,"basis",basis)));}
}
