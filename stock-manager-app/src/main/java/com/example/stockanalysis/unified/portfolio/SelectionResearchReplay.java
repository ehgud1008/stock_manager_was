package com.example.stockanalysis.unified.portfolio;

import com.example.stockanalysis.unified.report.*;
import com.example.stockanalysis.unified.report.ReportModels.*;
import com.example.stockanalysis.unified.scan.UnifiedScanStore;
import com.example.stockanalysis.judgment.NewEntryAssessment;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.stereotype.Component;

/** Uses only the original saved analysis and EOD input. Never reruns collection or a current-price engine. */
@Component
public class SelectionResearchReplay {
    private final ReportStore reports;private final UnifiedScanStore scans;private final ObjectMapper mapper;
    public SelectionResearchReplay(ReportStore reports,UnifiedScanStore scans,ObjectMapper mapper){this.reports=reports;this.scans=scans;this.mapper=mapper;}
    public List<StockDecision> replay(DailyReport report,ReportProperties policy,List<StockDecision> all,MarketSnapshot markets,CandidateAnalyzer.ResearchMode mode) {
        if(markets==null || markets.collectedAt().isAfter(report.finalizedAt()))throw new IllegalArgumentException("RESEARCH_INPUT_UNAVAILABLE");
        var analyzer=report.version().startsWith("candidate-market-v4-common-entry/")?CandidateAnalyzer.legacyV4(mapper,policy):report.version().startsWith("candidate-market-v5-tier/")?CandidateAnalyzer.legacyV5(mapper,policy):report.version().startsWith("candidate-v6-signals/")?CandidateAnalyzer.legacyV6(mapper,policy):new CandidateAnalyzer(mapper,policy);var result=new ArrayList<StockDecision>();
        for(var saved:all) {
            var common=saved.newEntry();
            boolean affected=common!=null && (mode==CandidateAnalyzer.ResearchMode.SIGNAL_AGE4
                    ? common.signals().stream().anyMatch(s->s.ageBars()==4)
                    : common.status()==NewEntryAssessment.Status.ELIGIBLE);
            if(!affected){result.add(saved);continue;}
            var detail=scans.detail(report.runId(),saved.code()).orElseThrow(()->new IllegalArgumentException("RESEARCH_INPUT_UNAVAILABLE"));
            if(detail.eod()==null || detail.eod().collectedAt().isAfter(report.finalizedAt()) || !detail.result().priceDate().equals(report.tradeDate()))
                throw new IllegalArgumentException("RESEARCH_INPUT_TIME_MISMATCH");
            var input=reports.input(report.runId(),saved.code());
            var index=markets.indices().stream().filter(i->i.market().equals(saved.market())).findFirst().orElseThrow();
            var reproduced=analyzer.analyze(report.runId(),saved.market(),detail,input,index);
            if(!mapper.valueToTree(reproduced.newEntry()).equals(mapper.valueToTree(saved.newEntry()))
                    || !mapper.valueToTree(reproduced.evidence()).equals(mapper.valueToTree(saved.evidence()))
                    || !mapper.valueToTree(reproduced.scenario()).equals(mapper.valueToTree(saved.scenario())))
                throw new IllegalArgumentException("RESEARCH_BASELINE_MISMATCH");
            result.add(analyzer.analyze(report.runId(),saved.market(),detail,input,index,mode));
        }
        return ResearchSelections.rank(result,report,policy.maximumCandidates());
    }
}
