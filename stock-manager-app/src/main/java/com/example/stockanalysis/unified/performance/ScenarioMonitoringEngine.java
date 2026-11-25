package com.example.stockanalysis.unified.performance;

import static com.example.stockanalysis.unified.performance.ScenarioMonitoringModels.*;
import com.example.stockanalysis.unified.report.ReportModels.StockDecision;
import com.example.stockanalysis.unified.performance.PerformanceModels.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Frozen selection scenario, daily-bar observations. Never implies an actual holding or fill. */
@Component
public class ScenarioMonitoringEngine {
    private final PerformanceProperties costs;
    public ScenarioMonitoringEngine(){this(new PerformanceProperties(false,new BigDecimal("30"),new BigDecimal("5"),new BigDecimal("0.3")));}
    @org.springframework.beans.factory.annotation.Autowired
    public ScenarioMonitoringEngine(PerformanceProperties costs){this.costs=costs;}
    public Stock evaluate(StockDecision candidate,Anchor anchor,List<LocalDate> dates,List<Observation> observations,
                          Instant now,boolean calendarComplete,boolean timelyReport) {
        var today=now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
        var known=new HashMap<LocalDate,Observation>();
        for(var observation:observations) {
            if(!observation.code().equals(candidate.code())||!"STOCK".equals(observation.kind())||observation.collectedAt().isAfter(now))
                throw new IllegalArgumentException("MONITORING_OBSERVATION_IDENTITY");
            if(known.put(observation.date(),observation)!=null)throw new IllegalArgumentException("DUPLICATE_MONITORING_OBSERVATION");
        }
        var due=dates.stream().filter(d->!d.isAfter(today)).toList();
        var scenario=candidate.scenario();var history=new ArrayList<Day>();var notes=new ArrayList<String>();
        boolean conditional=Set.of("ENTRY_CONDITIONAL","RESEARCH_B").contains(candidate.disposition());
        String status=scenario==null?"NO_SCENARIO":anchor==null?"BASELINE_MISSING":!calendarComplete?"CALENDAR_UNAVAILABLE"
                :!timelyReport?"REPORT_LATE":!conditional?"WATCH_ONLY":"WAITING_ENTRY_DAY";
        String dataState=due.isEmpty()?"NOT_DUE":"CURRENT";
        LocalDate entryDate=null,eventDate=null,latestDate=null;
        BigDecimal latestClose=null,entryPrice=null,exitPrice=null;
        String exitReason=null;
        int holdingDays=0;
        boolean ambiguous=false;
        int observed=0;
        boolean active=conditional&&scenario!=null&&anchor!=null&&calendarComplete&&timelyReport;
        boolean terminal=!active,broken=false;
        for(int i=0;i<due.size();i++) {
            var date=due.get(i);var observation=known.get(date);
            String quality=observation==null?"MISSING":observation.quality();
            var candle=observation!=null&&"VALID".equals(quality)?observation.candle():null;
            if(candle!=null&&!date.equals(candle.businessDate()))throw new IllegalArgumentException("MONITORING_DATE_MISMATCH");
            boolean target=false,stop=false;String note=null;
            if(candle==null) {
                boolean waiting=observation==null&&date.equals(today);
                dataState=waiting&&dataState.equals("CURRENT")?"WAITING_DATA":"DATA_GAP";
                if(active&&!terminal){broken=true;status=waiting&&date.equals(due.get(due.size()-1))?"WAITING_DATA":"DATA_GAP";}
                note=waiting?"당일 마감 자료를 기다리고 있습니다.":"자료 누락 또는 가격 기준 불일치로 사건 순서를 확인할 수 없습니다.";
            } else {
                observed++;latestDate=date;latestClose=candle.close();
                boolean tradable=candle.volume()>0&&candle.high().compareTo(candle.low())>0;
                if(scenario!=null&&tradable){target=candle.high().compareTo(scenario.targets().get(0))>=0;stop=candle.low().compareTo(scenario.stopLoss())<=0;}
                if(!tradable) {
                    note="거래량 0 또는 단일 가격 봉으로 체결·도달 여부를 확정하지 않습니다.";
                    if(active&&!terminal){broken=true;status="DATA_GAP";dataState="DATA_GAP";}
                } else if(active&&!terminal&&!broken) {
                    if(i==0) {
                        if(candle.open().compareTo(scenario.entryFrom())<0||TradeOutcomeRules.entry(candle.open(),costs.slippageBps()).compareTo(scenario.entryTo())>0) {
                            status="NO_ENTRY";terminal=true;note="다음 거래일 시가 또는 체결 차이를 반영한 가격이 선정 당시 진입 구간 밖입니다. 이후 진입을 가정하지 않습니다.";
                        } else {entryDate=date;entryPrice=TradeOutcomeRules.entry(candle.open(),costs.slippageBps());status="IN_PROGRESS";note="다음 거래일 시가가 진입 구간에 포함됩니다. 실제 체결을 뜻하지 않습니다.";}
                    }
                    if(!terminal) {
                        holdingDays=i+1;
                        var event=TradeOutcomeRules.exit(candle,scenario,i>0,i==19);
                        if(event!=null){
                            exitReason=event.reason();ambiguous=event.ambiguous();
                            exitPrice=TradeOutcomeRules.exitPrice(event.price(),costs.slippageBps());
                            status=ambiguous?"AMBIGUOUS":event.reason().equals("TARGET")?"TARGET_REACHED":event.reason().equals("TIME_EXIT")?"PERIOD_ENDED":"STOP_REACHED";
                            terminal=true;
                            if(ambiguous)note="같은 일봉에서 목표와 손절 접촉: 선후 미확인. 손절 우선 가정 손익만 별도 제공합니다.";
                        }
                        if(terminal)eventDate=date;
                    }
                } else if(broken)note="앞선 미확인 구간이 있어 이후 가격만으로 시나리오 성패를 확정하지 않습니다.";
                else if(!conditional)note="관찰 대상이므로 모의 진입하지 않습니다. 선정 성공·실패는 별도 선정 평가에서 판정합니다.";
                else if(active&&terminal)note="최초 판정 이후의 참고 관측입니다. 이전 판정을 덮어쓰거나 새로운 진입을 가정하지 않습니다.";
            }
            history.add(new Day(date,quality,candle,target,stop,status,observation==null?null:observation.sourceRunId(),
                    observation==null?null:observation.collectedAt(),note));
        }
        if(broken&&dataState.equals("DATA_GAP")&&active&&!terminal)status="DATA_GAP";
        if(due.isEmpty())notes.add("선정 직후입니다. 다음 거래일부터 저장된 마감 일봉으로 확인합니다.");
        if(anchor==null)notes.add("선정일 원본 일봉을 확인할 수 없어 가격 비교를 보류합니다.");
        if(!calendarComplete)notes.add("20거래일 달력을 확인할 수 없어 시나리오 판정을 보류합니다.");
        if(!timelyReport)notes.add("다음 거래일 장 시작 전 확정된 보고서가 아니므로 당시 진입을 가정하지 않습니다.");
        notes.add("선정 당시 진입 구간·1차 목표·손절을 고정합니다. 이후 재선정 결과로 덮어쓰지 않습니다.");
        if(status.equals("AMBIGUOUS"))notes.add("목표·손절 동시 접촉: 일봉으로 도달 순서를 확인할 수 없습니다.");
        BigDecimal baseline=anchor==null?null:anchor.candle().close();
        BigDecimal stopDistance=scenario==null?null:change(scenario.stopLoss(),latestClose);
        BigDecimal net=entryPrice==null||exitPrice==null?null:TradeOutcomeRules.net(entryPrice,exitPrice,costs.roundTripFeeBps());
        BigDecimal marked=entryPrice!=null&&exitPrice==null&&!broken&&latestClose!=null
                ?TradeOutcomeRules.net(entryPrice,TradeOutcomeRules.exitPrice(latestClose,costs.slippageBps()),costs.roundTripFeeBps()):null;
        String outcomeStatus=status.equals("PERIOD_ENDED")&&net!=null?net.signum()>0?"TIME_PROFIT":net.signum()<0?"TIME_LOSS":"TIME_FLAT":status;
        String label=switch(outcomeStatus){
            case "TARGET_REACHED"->"1차 목표 달성";case "STOP_REACHED"->"손절 종료";
            case "TIME_PROFIT"->"기간 종료 · 수익";case "TIME_LOSS"->"기간 종료 · 손실";case "TIME_FLAT"->"기간 종료 · 보합";
            case "AMBIGUOUS"->"목표·손절 선후 미확인";case "NO_ENTRY"->"진입 구간 미충족 · 미진입";
            case "WATCH_ONLY"->"관찰 대상 · 매매 수익 판정 없음";case "IN_PROGRESS"->"모의 보유 진행 중";
            case "WAITING_ENTRY_DAY"->"다음 거래일 진입 대기";case "WAITING_DATA"->"마감 자료 대기";
            default->"자료 또는 시나리오 확인 필요";
        };
        String summary=label;
        if(entryPrice!=null)summary+=" · "+entryDate+" "+entryPrice.stripTrailingZeros().toPlainString()+"원 모의 진입";
        if(eventDate!=null)summary+=" · "+eventDate+" 판정";
        if(net!=null&&!ambiguous)summary+=" · 비용 반영 모의 수익률 "+net.setScale(2,RoundingMode.HALF_UP)+"%";
        if(marked!=null)summary+=" · "+latestDate+" 기준 평가손익 "+marked.setScale(2,RoundingMode.HALF_UP)+"%";
        var outcome=new Outcome("scenario-outcome-v1",outcomeStatus,summary,entryDate,entryPrice,
                ambiguous?null:eventDate,ambiguous?null:exitPrice,exitReason,
                ambiguous?null:TradeOutcomeRules.change(exitPrice,entryPrice),ambiguous?null:net,marked,ambiguous?net:null,
                holdingDays,costs.roundTripFeeBps(),costs.slippageBps());
        notes.add("모의 진입은 다음 거래일 시가에 체결 차이를 반영하며 진입 상단 초과 시 미진입입니다. 실제 주문·체결 수익이 아닙니다.");
        notes.add("조회 시 비용 가정: 왕복 수수료·세금 "+costs.roundTripFeeBps()+"bp, 편도 체결 차이 "+costs.slippageBps()+"bp. 별도 등록 평가의 고정 정책과 다를 수 있습니다.");
        return new Stock(candidate.code(),candidate.name(),candidate.market(),candidate.disposition(),candidate.strategy(),scenario,
                baseline,status,dataState,entryDate,eventDate,latestDate,latestClose,change(latestClose,baseline),
                scenario==null?null:change(scenario.targets().get(0),latestClose),stopDistance==null?null:stopDistance.negate(),
                observed,due.size(),List.copyOf(history),List.copyOf(notes),outcome,
                new SelectionOutcomeEvaluator().evaluate(scenario,baseline,history,calendarComplete,timelyReport,today));
    }
    private BigDecimal change(BigDecimal end,BigDecimal start) {
        return end==null||start==null||start.signum()<=0?null:end.divide(start,MathContext.DECIMAL128).subtract(BigDecimal.ONE)
                .multiply(new BigDecimal("100")).setScale(4,RoundingMode.HALF_UP);
    }
}
