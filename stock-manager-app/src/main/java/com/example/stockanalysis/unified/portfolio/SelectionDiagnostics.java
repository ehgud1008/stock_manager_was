package com.example.stockanalysis.unified.portfolio;

import com.example.stockanalysis.unified.report.ReportModels.*;
import com.example.stockanalysis.judgment.NewEntryAssessment;
import java.util.*;

/** Counts saved evidence, never future prices. Dependent entry checks are grouped under common eligibility. */
public final class SelectionDiagnostics {
    public record Rule(String code,String label,int failed,int unknown,int onlyFailure) {}
    public record NearMiss(String code,String name,String rule) {}
    public record Audit(String runId,String state,long target,int recorded,int missingRecords,int commonEligible,
                        int dataUnavailable,int invalidInput,int singleFailure,int multipleFailures,int rankLimited,
                        List<Rule> rules,List<NearMiss> nearMisses,int nearMissTotal,List<String> limitations) {}
    private static final Map<String,String> LABELS;
    static {
        var labels=new LinkedHashMap<String,String>();
        labels.put("COMMON_ENTRY","공통 신규 진입 자격·시가 하한");
        labels.put("STRUCTURE","상승 정배열·이평선 상승");labels.put("POSITIVE_RETURN20","종목 자체 20봉 상승");
        labels.put("AMOUNT_TODAY","당일 거래대금");labels.put("AMOUNT_AVERAGE20","평균 거래대금");
        labels.put("PRICE_EMA20_DISTANCE","과열 제한");labels.put("RELATIVE_RETURN20","시장 대비 강도");
        labels.put("VOLUME_RATIO20","선정일 거래량");labels.put("NET_REWARD_RISK","비용 반영 손익비");
        labels.put("MARKET_FILTER","시장 전체 필터");LABELS=Collections.unmodifiableMap(labels);
    }
    public static boolean profitability(DailyReport report) { return report.version().startsWith("candidate-v7-profitability/"); }
    public static boolean modern(DailyReport report) { return report.version().startsWith("candidate-v6-signals/")||profitability(report); }
    public static boolean tiered(DailyReport report) { return report.version().startsWith("candidate-market-v5-tier/")||modern(report); }
    public static boolean supported(DailyReport report) { return tiered(report)||report.version().startsWith("candidate-market-v4-common-entry/"); }
    public static Boolean passed(StockDecision stock,String code) {
        var found=stock.evidence().stream().filter(e->e.code().equals(code)).toList();
        return found.size()==1?found.get(0).passed():null;
    }
    private static Boolean all(Boolean... checks) {
        if(Arrays.stream(checks).anyMatch(Objects::isNull))return null;
        return Arrays.stream(checks).allMatch(Boolean.TRUE::equals);
    }
    public static Audit audit(DailyReport report,List<StockDecision> stocks) {
        var counters=new LinkedHashMap<String,int[]>();LABELS.keySet().forEach(k->counters.put(k,new int[3]));
        int eligible=0,unavailable=0,invalid=0,single=0,multiple=0,limited=0;var near=new ArrayList<NearMiss>();
        if(!supported(report))return new Audit(report.runId(),"UNSUPPORTED",report.eligibleStocks(),stocks.size(),0,0,0,0,0,0,0,List.of(),List.of(),0,
                List.of("이전 보고서는 공통 판단 자료가 없어 조건별 정밀 집계를 지원하지 않습니다. 기존 성과 조회는 유지됩니다."));
        for(var s:stocks.stream().sorted(Comparator.comparing(StockDecision::code)).toList()) {
            if(s.dailyReturn()==null){invalid++;continue;}
            var common=s.newEntry();var checks=new LinkedHashMap<String,Boolean>();
            if(common!=null&&common.status()==NewEntryAssessment.Status.ELIGIBLE)eligible++;
            checks.put("COMMON_ENTRY",all(common==null||common.status()==NewEntryAssessment.Status.DEFER?null:common.status()==NewEntryAssessment.Status.ELIGIBLE,
                    passed(s,"OPEN_PRICE")));
            checks.put("STRUCTURE",all(passed(s,"BULLISH_STRUCTURE"),passed(s,"EMA5_SLOPE_3"),passed(s,"EMA20_SLOPE_3"),profitability(report)?passed(s,"BREAKOUT_CLOSE"):Boolean.TRUE));
            for(var code:List.of("POSITIVE_RETURN20","AMOUNT_TODAY","AMOUNT_AVERAGE20","PRICE_EMA20_DISTANCE","RELATIVE_RETURN20","VOLUME_RATIO20"))checks.put(code, modern(report)&&code.equals("RELATIVE_RETURN20")?Boolean.TRUE:passed(s,modern(report)&&code.equals("VOLUME_RATIO20")?"PATTERN_VOLUME":code));
            // No eligible signal means range and RR cannot be evaluated independently of the common rejection.
            if(common!=null&&common.selectedSignal()!=null)checks.put("NET_REWARD_RISK",passed(s,"NET_REWARD_RISK"));
            var market=report.markets().stream().filter(m->m.market().equals(s.market())).findFirst().orElse(null);
            checks.put("MARKET_FILTER",modern(report)?Boolean.TRUE:market==null||"판단 보류".equals(market.bias())?null:"상승 우세".equals(market.bias()));
            var failed=checks.entrySet().stream().filter(e->Boolean.FALSE.equals(e.getValue())).map(Map.Entry::getKey).toList();
            boolean missing=checks.containsValue(null);
            checks.forEach((key,value)->{if(value==null)counters.get(key)[1]++;else if(!value)counters.get(key)[0]++;});
            if(missing){unavailable++;continue;}
            if(failed.size()==1){single++;counters.get(failed.get(0))[2]++;if(near.size()<100)near.add(new NearMiss(s.code(),s.name(),LABELS.get(failed.get(0))));}
            else if(failed.size()>1)multiple++;
            if("RANK_LIMIT".equals(s.disposition()))limited++;
        }
        int missing=(int)Math.max(0,report.eligibleStocks()-stocks.size());
        var rules=counters.entrySet().stream().map(e->new Rule(e.getKey(),LABELS.get(e.getKey()),e.getValue()[0],e.getValue()[1],e.getValue()[2])).toList();
        return new Audit(report.runId(),"READY",report.eligibleStocks(),stocks.size(),missing,eligible,unavailable,invalid,single,multiple,limited,rules,near,single,
                List.of("자료 미확인 종목은 한 조건만 미충족·여러 조건 미충족 집계에서 제외합니다. 조건별 미충족 건수는 중복될 수 있습니다.",
                        "공통 자격·시가 하한·진입 구간·매수 패턴 등 종속된 판정은 공통 진입 자격으로 묶습니다. 한 조건은 개별 수치가 아닌 조건 그룹을 뜻합니다. 보유 종목 행동은 집계 대상이 아닙니다.",
                        "한 조건만 미충족한 종목은 종목 코드 순으로 최대 100개를 표시합니다. 상승 확률이나 매수 추천 목록이 아닙니다."));
    }
}
