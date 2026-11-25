package com.example.stockanalysis.unified.portfolio;

import com.example.stockanalysis.unified.report.ReportModels.*;
import com.example.stockanalysis.judgment.NewEntryAssessment;
import java.util.*;

/** Explicit single-rule alternatives. Unknown evidence never becomes a passing observation. */
public final class ResearchSelections {
    public static final List<String> VARIANTS=List.of("NO_RELATIVE_STRENGTH","SIGNAL_AGE4","PATTERN_VOLUME","TIER_A_B","TIER_B_ONLY");
    public static List<StockDecision> withTierB(List<StockDecision> all,DailyReport report,int maximum) {
        var stocks=new ArrayList<StockDecision>();
        for(var s:all) {
            if(Set.of("ENTRY_CONDITIONAL","WATCH","RANK_LIMIT").contains(s.disposition())){stocks.add(s);continue;}
            if(!"RESEARCH_B".equals(s.disposition()))continue;
            var common=s.newEntry();
            if(common==null||common.suitability()==null||!"B".equals(common.suitability().tier())||common.timing()==null
                    ||common.timing().status()!=NewEntryAssessment.Status.ELIGIBLE||common.selectedSignal()==null
                    ||!Objects.equals(common.effectiveScenario(),s.scenario())||s.netRewardRisk()==null
                    ||!Boolean.TRUE.equals(SelectionDiagnostics.passed(s,"ENTRY_TIMING"))
                    ||(SelectionDiagnostics.profitability(report)&&!Boolean.TRUE.equals(SelectionDiagnostics.passed(s,"BREAKOUT_CLOSE")))
                    ||(!SelectionDiagnostics.modern(report)&&!Boolean.TRUE.equals(SelectionDiagnostics.passed(s,"RELATIVE_RETURN20")))
                    ||!REQUIRED.stream().filter(k->!k.equals("COMMON_ENTRY")).allMatch(k->Boolean.TRUE.equals(SelectionDiagnostics.passed(s,SelectionDiagnostics.modern(report)&&k.equals("VOLUME_RATIO20")?"PATTERN_VOLUME":k))))
                throw new IllegalArgumentException("TIER_B_EVIDENCE_INCOMPLETE");
            stocks.add(new StockDecision(s.code(),s.name(),s.market(),"ENTRY_CONDITIONAL",s.strategy(),s.evidence(),s.cautions(),s.scenario(),s.netRewardRisk(),s.relativeReturn20(),s.liquidity(),s.dailyReturn(),s.aboveEma20(),s.stage(),s.transition61(),common));
        }
        return rank(stocks,report,maximum);
    }
    private static final List<String> REQUIRED=List.of("COMMON_ENTRY","OPEN_PRICE","BULLISH_STRUCTURE","POSITIVE_RETURN20",
            "PRICE_EMA20_DISTANCE","AMOUNT_TODAY","AMOUNT_AVERAGE20","NO_SELL_CONFLICT","EMA5_SLOPE_3","EMA20_SLOPE_3",
            "VOLUME_RATIO20","ENTRY_RANGE","BUY_PATTERN","NET_REWARD_RISK");
    public static List<StockDecision> withoutRelativeStrength(List<StockDecision> all,DailyReport report,int maximum) {
        var restored=new ArrayList<StockDecision>();
        for(var s:all) {
            var common=s.newEntry();
            if(s.dailyReturn()==null || common==null || common.status()!=NewEntryAssessment.Status.ELIGIBLE || common.selectedSignal()==null
                    || common.effectiveScenario()==null || !common.effectiveScenario().equals(s.scenario())
                    || s.netRewardRisk()==null || s.liquidity()==null || s.relativeReturn20()==null
                    || SelectionDiagnostics.passed(s,"RELATIVE_RETURN20")==null
                    || (SelectionDiagnostics.profitability(report)&&!Boolean.TRUE.equals(SelectionDiagnostics.passed(s,"BREAKOUT_CLOSE")))
                    || !REQUIRED.stream().allMatch(code->Boolean.TRUE.equals(SelectionDiagnostics.passed(s,SelectionDiagnostics.modern(report)&&code.equals("VOLUME_RATIO20")?"PATTERN_VOLUME":code))))continue;
            restored.add(new StockDecision(s.code(),s.name(),s.market(),"ENTRY_CONDITIONAL",common.selectedSignal().type(),s.evidence(),
                    List.of("연구 비교: 시장 대비 강도 제외 조건만 해제했습니다. 운영 선정 결과는 변경하지 않습니다."),
                    common.effectiveScenario(),s.netRewardRisk(),s.relativeReturn20(),s.liquidity(),s.dailyReturn(),s.aboveEma20(),s.stage(),s.transition61(),common));
        }
        return rank(restored,report,maximum);
    }
    public static List<StockDecision> rank(List<StockDecision> all,DailyReport report,int maximum) {
        return all.stream().filter(s->Set.of("ENTRY_CONDITIONAL","WATCH","RANK_LIMIT").contains(s.disposition())).map(s->{
            boolean bullish=SelectionDiagnostics.modern(report)||report.markets().stream().anyMatch(m->m.market().equals(s.market())&&"상승 우세".equals(m.bias()));
            return new StockDecision(s.code(),s.name(),s.market(),bullish?"ENTRY_CONDITIONAL":"WATCH",s.strategy(),s.evidence(),s.cautions(),s.scenario(),
                    s.netRewardRisk(),s.relativeReturn20(),s.liquidity(),s.dailyReturn(),s.aboveEma20(),s.stage(),s.transition61(),s.newEntry());
        }).sorted(SelectionDiagnostics.profitability(report)?com.example.stockanalysis.unified.report.CandidateRanking.comparator():Comparator.comparingInt((StockDecision s)->"ENTRY_CONDITIONAL".equals(s.disposition())?0:1)
                .thenComparing((StockDecision s)->SelectionDiagnostics.modern(report)?s.newEntry().suitability().score():s.relativeReturn20(),Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(StockDecision::liquidity,Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(StockDecision::code))
                .limit(maximum).toList();
    }
}
