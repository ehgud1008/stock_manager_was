package com.example.stockanalysis.unified.performance;

import static com.example.stockanalysis.unified.performance.ScenarioMonitoringModels.*;
import com.example.stockanalysis.engine.model.TradeScenario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Evaluates the frozen price hypothesis independently of entry eligibility or simulated fills. */
public final class SelectionOutcomeEvaluator {
    public static final String VERSION="selection-outcome-v1";
    public SelectionOutcome evaluate(TradeScenario scenario,BigDecimal baseline,List<Day> history,
                                     boolean calendarComplete,boolean timelyReport,LocalDate today) {
        if(scenario==null)return result("DEFERRED",null,null,null,"선정 판정 보류 · 가격 시나리오 없음");
        if(baseline==null||baseline.signum()<=0)return result("DEFERRED",null,null,null,"선정 판정 보류 · 선정일 원본 확인 필요");
        if(!calendarComplete)return result("DEFERRED",null,null,null,"선정 판정 보류 · 20거래일 달력 확인 필요");
        if(!timelyReport)return result("DEFERRED",null,null,null,"선정 판정 보류 · 추적 시작 전 보고서 확정 여부 확인 필요");
        LocalDate through=null;
        for(int i=0;i<Math.min(20,history.size());i++) {
            var day=history.get(i);var bar=day.candle();
            if(!"VALID".equals(day.quality())||bar==null||bar.volume()<=0||bar.high().compareTo(bar.low())<=0){
                boolean waiting=bar==null&&"MISSING".equals(day.quality())&&day.date().equals(today);
                return result(waiting?"WAITING_DATA":"DEFERRED",null,null,through,
                        waiting?"선정 판정 대기 · 당일 마감 자료 대기":"선정 판정 보류 · "+day.date()+" 자료 부족으로 최초 도달 순서 미확인");
            }
            through=day.date();
            // Unlike a trade, the price hypothesis starts at the first following session's open.
            var event=TradeOutcomeRules.exit(bar,scenario,true,i==19);
            if(event==null)continue;
            if(event.ambiguous())return result("AMBIGUOUS",day.date(),null,through,"선정 판정 불가 · "+day.date()+" 목표·손절 동시 접촉, 선후 미확인");
            String state=event.reason().equals("TARGET")?"SUCCESS":event.reason().equals("TIME_EXIT")?"FAILURE_TIMEOUT":"FAILURE_STOP";
            String label=state.equals("SUCCESS")?"선정 성공 · 1차 목표 선행 도달":state.equals("FAILURE_STOP")?"선정 실패 · 손절 기준 선행 도달":"선정 실패 · 20거래일 목표 미달";
            return result(state,day.date(),TradeOutcomeRules.change(bar.close(),baseline),through,label+" · "+day.date());
        }
        return result(history.isEmpty()?"WAITING":"IN_PROGRESS",null,null,through,
                history.isEmpty()?"선정 판정 대기 · 다음 거래일부터 추적":"선정 평가 진행 중 · 목표·손절 미도달 (최대 20거래일)");
    }
    private SelectionOutcome result(String state,LocalDate event,BigDecimal change,LocalDate through,String summary){
        return new SelectionOutcome(VERSION,state,event,through,change,summary);
    }
}
