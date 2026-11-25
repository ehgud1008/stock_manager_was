package com.example.stockanalysis.unified.report;

import static com.example.stockanalysis.unified.report.ReportFixtures.*;
import static org.assertj.core.api.Assertions.*;
import com.example.stockanalysis.unified.application.UnifiedAnalysisResponse;
import com.example.stockanalysis.judgment.NewEntryEvaluator;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;

class SignalDiscoveryTest {
    @Test void novemberFourthSavedStocksRetainTheirActualSignalsAndDataDeferral() throws Exception {
        for(var code:List.of("009470","207940")) {
            try(var source=getClass().getResourceAsStream("/signal-discovery/"+code+".json")) {
                var data=MAPPER.treeToValue(MAPPER.readTree(source).path("data"),UnifiedAnalysisResponse.class);
                var result=data.result();var assessment=new NewEntryEvaluator().evaluate(result.analysis(),result.stock().currentPrice(),
                        result.priceDate(),result.asOf().atZone(ZoneId.of("Asia/Seoul")).toInstant(),data.collectionQuality().name());
                assertThat(assessment.suitability().tier()).isEqualTo("B");
                assertThat(assessment.selectedSignal()).isNotNull();
                assertThat(assessment.selectedSignal().entryStatus()).isEqualTo("IN_RANGE");
                assertThat(assessment.status().name()).isEqualTo("DEFER");
                assertThat(assessment.reasons()).contains("수집 자료가 완전하지 않아 신규 진입을 보류합니다.");
                assertThat(assessment.signals()).anyMatch(c->c.eligible());
                var complete=new NewEntryEvaluator().evaluate(result.analysis(),result.stock().currentPrice(),result.priceDate(),
                        result.asOf().atZone(ZoneId.of("Asia/Seoul")).toInstant(),"COMPLETE");
                assertThat(complete.timing().status().name()).isEqualTo("ELIGIBLE");
                assertThat(complete.status().name()).isEqualTo("WATCH");
            }
        }
    }
    @Test void secondPrecisionPersistenceRetainsSignalsButDifferentSecondDoesNot() throws Exception {
        var data=bundle("005930",true); var tree=(ObjectNode)MAPPER.valueToTree(data.detail());
        ((ObjectNode)tree.path("result").path("analysis").path("signalReport")).put("evaluatedAt",NOW.plusMillis(89).toString());
        var detail=MAPPER.treeToValue(tree,UnifiedAnalysisResponse.class);
        var found=new CandidateAnalyzer(MAPPER,settings()).analyze("r","KOSPI",detail,data.input(),market().indices().get(0));
        assertThat(found.newEntry().selectedSignal()).isNotNull();
        assertThat(found.disposition()).isEqualTo("ENTRY_CONDITIONAL");
        assertThat(NewEntryEvaluator.validContext(detail.result().analysis(),detail.result().stock().currentPrice(),DAY,NOW.plusSeconds(1),"COMPLETE")).isFalse();
    }
    @Test void currentLowVolumeDoesNotHideValidEventAndPartialDataStillDefers() throws Exception {
        var data=bundle("005930",true);var tree=(ObjectNode)MAPPER.valueToTree(data.detail());
        tree.put("collectionQuality","PARTIAL");
        var candles=(ArrayNode)data.input().path("input").path("dailyCandles");int last=candles.size()-1;
        ((ObjectNode)candles.get(last)).put("volume",500000);
        ((ObjectNode)candles.get(last-1)).put("volume",2000000);
        var analysis=(ObjectNode)tree.path("result").path("analysis");analysis.put("totalScore",77.33);
        var signal=(ObjectNode)analysis.path("signalReport").path("signals").get(0);
        signal.put("occurredOn",candles.get(last-1).path("businessDate").asText());signal.put("ageBars",1);signal.put("state","ACTIVE");
        var detail=MAPPER.treeToValue(tree,UnifiedAnalysisResponse.class);
        var found=new CandidateAnalyzer(MAPPER,settings()).analyze("r","KOSPI",detail,data.input(),market().indices().get(0));
        assertThat(found.newEntry().timing().status().name()).isEqualTo("DEFER");
        var lists=ReportModels.TierLists.from(List.of(found),20);
        assertThat(lists.activeSignalsTotal()).isEqualTo(1);
        assertThat(lists.comparisonBTotal()).isZero();
        tree.put("collectionQuality","COMPLETE");
        var complete=new CandidateAnalyzer(MAPPER,settings()).analyze("r","KOSPI",MAPPER.treeToValue(tree,UnifiedAnalysisResponse.class),data.input(),market().indices().get(0));
        assertThat(complete.disposition()).isEqualTo("RESEARCH_B");
        assertThat(complete.evidence()).filteredOn(e->e.code().equals("PATTERN_VOLUME")).allMatch(e->Boolean.TRUE.equals(e.passed()));
    }
}
