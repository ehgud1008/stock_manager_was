package com.example.stockanalysis.unified.portfolio;

import static com.example.stockanalysis.unified.portfolio.PortfolioModels.*;
import com.example.stockanalysis.unified.performance.*;
import com.example.stockanalysis.unified.report.*;
import com.example.stockanalysis.unified.report.ReportModels.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Reads frozen evidence, including rank-limited entries. Never reruns analysis on current prices. */
@Repository
public class PortfolioSources {
    private static final Set<String> ENTRY_STRATEGIES=Set.of("BREAKOUT","PULLBACK_RECOVERY","BREAKOUT_RETEST");
    private final JdbcTemplate jdbc;private final ObjectMapper mapper;private final PerformanceSources prices;
    private final SelectionResearchReplay research;
    public PortfolioSources(DataSource source,ObjectMapper mapper,PerformanceSources prices){this(source,mapper,prices,null);}
    @org.springframework.beans.factory.annotation.Autowired
    public PortfolioSources(DataSource source,ObjectMapper mapper,PerformanceSources prices,SelectionResearchReplay research){jdbc=new JdbcTemplate(source);this.mapper=mapper;this.prices=prices;this.research=research;}
    public List<Available> available(){return jdbc.query("SELECT d.report_json FROM ua_daily_report d WHERE d.status='READY' ORDER BY d.created_epoch DESC FETCH FIRST 200 ROWS ONLY",
            (r,n)->read(r.getString(1),DailyReport.class)).stream().filter(r->r.scope().equals("ALL")).map(r->new Available(r.runId(),r.tradeDate(),r.scope(),r.version(),r.candidates().size())).toList();}
    public Cohort cohort(String id,Instant now){
        var rows=jdbc.query("SELECT report_json,policy_json,market_json FROM ua_daily_report WHERE run_id=? AND status='READY'",(r,n)->new String[]{r.getString(1),r.getString(2),r.getString(3)},id);
        if(rows.size()!=1)throw new IllegalArgumentException("확정 보고서를 찾을 수 없습니다: "+id);
        var report=read(rows.get(0)[0],DailyReport.class);var settings=read(rows.get(0)[1],ReportProperties.class);
        var open=report.forecastDate().atTime(9,0).atZone(ZoneId.of("Asia/Seoul")).toInstant();
        if(!report.scope().equals("ALL") || !(report.version().startsWith("candidate-market-v1/")||report.version().startsWith("candidate-market-v2/")||report.version().startsWith("candidate-market-v3-open-price/")||SelectionDiagnostics.supported(report)) || report.finalizedAt().isAfter(now)
                || report.informationAsOf().isAfter(now) || !report.finalizedAt().isBefore(open) || !report.informationAsOf().isBefore(open))
            throw new IllegalArgumentException("지원 버전의 장 시작 전 전체 시장 보고서만 비교할 수 있습니다.");
        var all=jdbc.query("SELECT detail_json FROM ua_candidate_evidence WHERE run_id=? ORDER BY stock_code",(r,n)->read(r.getString(1),StockDecision.class),id);
        if(all.stream().filter(s->s.dailyReturn()!=null).count()!=report.assessedStocks() || all.stream().map(StockDecision::code).distinct().count()!=all.size())
            throw new IllegalArgumentException("선정·제외 근거가 불완전하여 시장 필터 비교를 재현할 수 없습니다.");
        var baseline=select(all,report,true,settings.maximumCandidates());var alternate=select(all,report,false,settings.maximumCandidates());
        if(!signatures(baseline).equals(signatures(report.candidates())))throw new IllegalArgumentException("저장 근거에서 원래 후보 목록을 재현할 수 없습니다.");
        var comparisons=new TreeMap<String,List<StockDecision>>();var unavailable=new TreeMap<String,String>();
        if(SelectionDiagnostics.tiered(report)) {
            comparisons.put("TIER_A_B",ResearchSelections.withTierB(all,report,settings.maximumCandidates()));
            comparisons.put("TIER_B_ONLY",ResearchSelections.rank(ResearchSelections.withTierB(all,report,Integer.MAX_VALUE).stream()
                    .filter(s->s.newEntry()!=null&&s.newEntry().suitability()!=null&&"B".equals(s.newEntry().suitability().tier())).toList(),report,settings.maximumCandidates()));
        } else for(var key:List.of("TIER_A_B","TIER_B_ONLY"))unavailable.put(key,"티어와 독립 진입 판단을 저장한 v5 보고서부터 지원합니다.");
        if(SelectionDiagnostics.supported(report)) {
            if(SelectionDiagnostics.modern(report))unavailable.put("NO_RELATIVE_STRENGTH","현재 보고서는 상대강도를 선정 제외 조건으로 사용하지 않습니다.");
            else comparisons.put("NO_RELATIVE_STRENGTH",ResearchSelections.withoutRelativeStrength(all,report,settings.maximumCandidates()));
            for(var mode:CandidateAnalyzer.ResearchMode.values()) {
                if(SelectionDiagnostics.modern(report)&&mode==CandidateAnalyzer.ResearchMode.PATTERN_VOLUME){unavailable.put(mode.name(),"패턴별 발생일 거래량 조건이 이미 적용된 보고서입니다.");continue;}
                if(research==null || rows.get(0)[2]==null){unavailable.put(mode.name(),"원본 분석·지수 자료 재생 기능을 사용할 수 없습니다.");continue;}
                try {comparisons.put(mode.name(),research.replay(report,settings,all,read(rows.get(0)[2],MarketSnapshot.class),mode));}
                catch(java.util.concurrent.CancellationException e){throw e;}
                catch(RuntimeException e){unavailable.put(mode.name(),"일부 원본 자료가 없거나 기존 판단을 재현하지 못해 이 비교안 전체를 보류합니다.");}
            }
        } else ResearchSelections.VARIANTS.forEach(key->unavailable.put(key,"공통 진입 판단을 저장한 v4 보고서부터 지원합니다."));
        var candidates=new LinkedHashMap<String,StockDecision>();baseline.forEach(s->candidates.put(s.code(),s));alternate.forEach(s->candidates.put(s.code(),s));
        comparisons.values().forEach(list->list.forEach(s->candidates.put(s.code(),s)));
        var anchors=candidates.values().stream().filter(s->ENTRY_STRATEGIES.contains(s.strategy())).map(s->prices.anchor(id,s.code(),s.market(),report.tradeDate())).toList();
        return new Cohort(report,baseline,alternate,anchors,comparisons,unavailable,SelectionDiagnostics.audit(report,all));
    }
    public SelectionDiagnostics.Audit audit(String id) {
        var rows=jdbc.query("SELECT report_json FROM ua_daily_report WHERE run_id=? AND status='READY'",(r,n)->read(r.getString(1),DailyReport.class),id);
        if(rows.size()!=1)throw new IllegalArgumentException("확정 보고서를 찾을 수 없습니다.");
        var all=jdbc.query("SELECT detail_json FROM ua_candidate_evidence WHERE run_id=? ORDER BY stock_code",(r,n)->read(r.getString(1),StockDecision.class),id);
        if(all.stream().filter(s->s.dailyReturn()!=null).count()!=rows.get(0).assessedStocks()
                || all.stream().map(StockDecision::code).distinct().count()!=all.size())throw new IllegalArgumentException("선정 근거가 불완전하여 탈락 원인을 집계할 수 없습니다.");
        return SelectionDiagnostics.audit(rows.get(0),all);
    }
    static List<StockDecision> select(List<StockDecision> all,DailyReport report,boolean filter,int maximum){
        var result=new ArrayList<StockDecision>();
        for(var s:all){
            if(!Set.of("ENTRY_CONDITIONAL","WATCH","RANK_LIMIT").contains(s.disposition()))continue;
            boolean entry=ENTRY_STRATEGIES.contains(s.strategy());
            if(!entry && !s.strategy().equals("TRANSITION_6_TO_1"))throw new IllegalArgumentException("UNKNOWN_RECORDED_STRATEGY");
            if(entry){
                var required=new HashSet<>(Set.of("PRICE_EMA20_DISTANCE","AMOUNT_TODAY","AMOUNT_AVERAGE20","RELATIVE_RETURN20","NO_SELL_CONFLICT","EMA5_SLOPE_3","EMA20_SLOPE_3","VOLUME_RATIO20","ENTRY_RANGE","BUY_PATTERN","NET_REWARD_RISK"));
                if(!report.version().startsWith("candidate-market-v1/"))required.addAll(Set.of("BULLISH_STRUCTURE","SWING_SCORE","SWING_ACTION","POSITIVE_RETURN20"));
                boolean common=SelectionDiagnostics.supported(report);
                if(SelectionDiagnostics.tiered(report)) {required.remove("SWING_ACTION");required.add("OPERATING_TIER");required.add("ENTRY_TIMING");}
                if(report.version().startsWith("candidate-market-v3-open-price/")||common)required.add("OPEN_PRICE");
                if(common) {
                    required.add("COMMON_ENTRY");
                    if(s.newEntry()==null || s.newEntry().status()!=com.example.stockanalysis.judgment.NewEntryAssessment.Status.ELIGIBLE
                            || s.newEntry().selectedSignal()==null || !s.strategy().equals(s.newEntry().selectedSignal().type())
                            || s.scenario()==null || !s.scenario().equals(s.newEntry().effectiveScenario()))
                        throw new IllegalArgumentException("COMMON_ENTRY_EVIDENCE_INCOMPLETE");
                }
                if(SelectionDiagnostics.modern(report)){required.remove("RELATIVE_RETURN20");required.remove("VOLUME_RATIO20");required.add("PATTERN_VOLUME");}
                if(SelectionDiagnostics.profitability(report)){required.remove("SWING_SCORE");required.add("BREAKOUT_CLOSE");}
                if(!required.stream().allMatch(code->s.evidence().stream().filter(e->e.code().equals(code)&&Boolean.TRUE.equals(e.passed())).count()==1))
                    throw new IllegalArgumentException("ENTRY_EVIDENCE_INCOMPLETE");
            }
            var market=report.markets().stream().filter(m->m.market().equals(s.market())).findFirst().orElseThrow();
            boolean blocked=!SelectionDiagnostics.modern(report) && (!report.version().startsWith("candidate-market-v1/")?!market.bias().equals("상승 우세"):market.bias().equals("하락 우세"));
            var disposition=entry&&!(filter&&blocked)?"ENTRY_CONDITIONAL":"WATCH";
            result.add(new StockDecision(s.code(),s.name(),s.market(),disposition,s.strategy(),s.evidence(),s.cautions(),s.scenario(),s.netRewardRisk(),s.relativeReturn20(),s.liquidity(),s.dailyReturn(),s.aboveEma20(),s.stage(),s.transition61(),s.newEntry()));
        }
        return result.stream().sorted(SelectionDiagnostics.profitability(report)?CandidateRanking.comparator():Comparator.comparingInt((StockDecision s)->s.disposition().equals("ENTRY_CONDITIONAL")?0:1)
                .thenComparing((StockDecision s)->SelectionDiagnostics.modern(report)?s.newEntry().suitability().score():s.relativeReturn20(),Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(StockDecision::liquidity,Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(StockDecision::code)).limit(maximum).toList();
    }
    private List<String> signatures(List<StockDecision> stocks){return stocks.stream().map(s->s.code()+":"+s.strategy()+":"+s.disposition()+":"+s.scenario()).toList();}
    private <T>T read(String value,Class<T> type){try{return mapper.readValue(value,type);}catch(Exception e){throw new IllegalArgumentException("SAVED_REPORT_INVALID");}}
}
