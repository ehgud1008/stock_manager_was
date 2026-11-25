package com.example.stockanalysis.decision;

import static com.example.stockanalysis.decision.DecisionModels.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import org.junit.jupiter.api.*;

class NewsInsightTest {
    DecisionWorkflowTest fixture;
    @BeforeEach void setup() {fixture=new DecisionWorkflowTest();fixture.setup();new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(new org.springframework.core.io.FileSystemResource(java.nio.file.Path.of("..","database","oracle","ddl","13_news_research.sql"))).execute(fixture.ds);}
    JsonNode evidence() {
        return fixture.json.tree(Map.of("collectedAt","2026-10-06T00:00:00Z","windowStart","2026-09-23","windowEnd","2026-10-06","sectorScope","COMPANY_CONTEXT","evidenceHash","frozen-hash","sources",List.of(Map.of(
                "id","S1","title","삼성전자 업황 회복 보도","excerpt","수요가 회복될 수 있지만 계약 확인이 필요하다.","depth","SEARCH_SNIPPET","url","https://example.com/news","dedupKey","key-1")),"coverage",List.of()));
    }
    ObjectNode report() {
        ObjectNode result=(ObjectNode)fixture.json.tree(Map.of("summary","업황 회복 보도가 있으나 실적 연결은 확인이 필요합니다.","summarySourceIds",List.of("S1"),"events",List.of(Map.of(
                "title","수요 회복 보도","sourceIds",List.of("S1"),"quoteSourceId","S1","factQuote","수요가 회복될 수 있지만 계약 확인이 필요하다.","interpretation","엔진 추세와 연결될 수 있습니다.","impactPath","수요 개선이 매출로 연결되는지 확인해야 합니다.","relevance","해당 기업의 업황을 다룬 보도입니다.","counterEvidence","실제 계약 자료는 없습니다.","engineRelation","UNCERTAIN","engineEvidenceIds",List.of("SCREENER"))),
                "nextChecks",List.of(Map.of("what","실적 발표 확인","why","수요의 매출 반영을 확인합니다.","sourceIds",List.of("S1"))),"limitations",List.of("검색 요약만 확보했습니다.")));
        result.set("businessContext",fixture.json.tree(Map.of("status","UNCONFIRMED","description","사업 원문 미확보","sourceIds",List.of())));
        result.set("signalInterpretation",fixture.json.tree(SignalInterpretation.unavailable()));
        var event=(ObjectNode)result.path("events").get(0);
        event.set("importance",fixture.json.tree(Map.of("level","UNDETERMINED","basis","INSUFFICIENT","reason","원문 미확보","metricIds",List.of())));
        event.set("conflict",fixture.json.tree(Map.of("supportingSourceIds",List.of("S1"),"opposingSourceIds",List.of(),"explanation","반대 자료를 확보하지 못했습니다.")));
        event.set("criticalQuestion",fixture.json.tree(Map.of("question","수요가 이익으로 연결되는가?","known","수요 회복 보도","unknown","실제 수익성","nextEvidence","분기 실적 발표","positiveCondition","이익률 개선 확인","negativeCondition","이익률 하락 확인")));
        event.putNull("previousEventId");event.put("changeType","NEW");event.put("changeReason","이전 연결 사건 없음");return result;
    }
    View submit(String snapshot,String key) {return fixture.service.insights(snapshot,new DecisionService.InsightRequest(14,""),key);}
    DecisionWorker worker(AiReviewPort ai,NewsEvidenceCollector collector) {var worker=fixture.worker(ai);worker.newsCollector(collector);return worker;}
    ObjectNode signal(String stance) {
        var check=Map.of("text","엔진 진입 조건과 후속 공시를 함께 확인합니다.","sourceIds",List.of("S1"),"engineEvidenceIds",List.of("ANALYSIS"));
        return (ObjectNode)fixture.json.tree(Map.of("stance",stance,"reason","공시 근거와 엔진 신호를 연결해 해석합니다.","sourceIds",List.of("S1"),"engineEvidenceIds",List.of("ANALYSIS"),
                "conflicts",List.of(),"nonHolderChecks",List.of(check),"holderChecks",List.of(check),"invalidationConditions",List.of(check),"missingInformation",List.of("실제 수익성은 미확인입니다.")));
    }
    ObjectNode signalInput() {
        var source=evidence().path("sources").deepCopy();((ObjectNode)source.get(0)).put("depth","OFFICIAL_DOCUMENT");
        return (ObjectNode)fixture.json.tree(Map.of("policy",Map.of("sources",source,"collectedAt","2026-10-12T00:00:00Z","engine",Map.of("priceDate","2026-10-09","evidence",Map.of("ANALYSIS",Map.of("strategy",Map.of("action","WAIT")),"SCREENER",Map.of(),"QUALITY","COMPLETE")))));
    }
    ObjectNode signalReport(String stance,String relation) {
        var report=report();report.set("signalInterpretation",signal(stance));var event=(ObjectNode)report.path("events").get(0);event.put("engineRelation",relation);event.putArray("engineEvidenceIds").add("ANALYSIS");return report;
    }
    @Test void signalOpinionNeedsDirectOfficialEvidenceAndNeverMeansAutomaticBuy() {
        var contract=new NewsInsightContract(fixture.json);var input=signalInput();var report=signalReport("SUPPORTED","SUPPORTS");contract.validate(report,input);
        var source=(ObjectNode)input.path("policy").path("sources").get(0);source.put("contextOutsideWindow",true);
        assertThatThrownBy(()->contract.validate(report,input)).isInstanceOf(AiFailure.class);source.put("contextOutsideWindow",false).put("depth","SEARCH_SNIPPET");
        assertThatThrownBy(()->contract.validate(report,input)).isInstanceOf(AiFailure.class);((ObjectNode)report.path("signalInterpretation")).put("stance","CHECK_REQUIRED");contract.validate(report,input);
        source.put("depth","TITLE_ONLY");((ObjectNode)report.path("events").get(0)).put("engineRelation","UNCERTAIN");
        assertThatThrownBy(()->contract.validate(report,input)).isInstanceOf(AiFailure.class);((ObjectNode)report.path("signalInterpretation")).put("stance","INSUFFICIENT");contract.validate(report,input);
    }
    @Test void weakenedEngineEvidenceMustAppearInConflictAndCannotSupportOverallSignal() {
        var contract=new NewsInsightContract(fixture.json);var input=signalInput();var report=signalReport("RECONSIDER","WEAKENS");
        assertThatThrownBy(()->contract.validate(report,input)).isInstanceOf(AiFailure.class);
        var signal=(ObjectNode)report.path("signalInterpretation");signal.set("conflicts",signal.path("holderChecks").deepCopy());contract.validate(report,input);
        signal.put("stance","SUPPORTED");assertThatThrownBy(()->contract.validate(report,input)).isInstanceOf(AiFailure.class);
    }
    @Test void oldMissingFutureDatesAndIncompleteEngineCannotProduceStrongOpinion() {
        var contract=new NewsInsightContract(fixture.json);var input=signalInput();var report=signalReport("SUPPORTED","SUPPORTS");
        for(String date:List.of("2026-10-04","2026-10-13","unknown")) {
            ((ObjectNode)input.path("policy").path("engine")).put("priceDate",date);
            assertThatThrownBy(()->contract.validate(report,input)).isInstanceOf(AiFailure.class);
        }
        ((ObjectNode)input.path("policy").path("engine")).put("priceDate","2026-10-05");contract.validate(report,input);
        ((ObjectNode)input.path("policy").path("engine").path("evidence")).put("QUALITY","PARTIAL");assertThatThrownBy(()->contract.validate(report,input)).isInstanceOf(AiFailure.class);
        ((ObjectNode)report.path("signalInterpretation")).put("stance","CHECK_REQUIRED");contract.validate(report,input);
        ((ObjectNode)input.path("policy").path("engine").path("evidence")).remove("ANALYSIS");assertThatThrownBy(()->contract.validate(report,input)).isInstanceOf(AiFailure.class);
        var context=SignalInterpretation.context(fixture.json,fixture.json.tree(Map.of("priceDate","2026-10-04")),fixture.json.tree(Map.of("collectedAt","2026-10-11T15:00:00Z")));
        assertThat(context.path("calendarDayGap").asLong()).isEqualTo(8);assertThat(context.path("status").asText()).isEqualTo("REFRESH_REQUIRED");
    }
    @Test void inventedPricesAndUnreferencedConditionsAreRejected() {
        var contract=new NewsInsightContract(fixture.json);var input=signalInput();var report=signalReport("CHECK_REQUIRED","UNCERTAIN");
        var signal=(ObjectNode)report.path("signalInterpretation");signal.put("reason","목표가 100000원, 승률 70%입니다.");assertThatThrownBy(()->contract.validate(report,input)).isInstanceOf(AiFailure.class);
        signal.put("reason","추가 확인이 필요합니다.");var condition=(ObjectNode)signal.path("nonHolderChecks").get(0);condition.putArray("sourceIds").add("S99");assertThatThrownBy(()->contract.validate(report,input)).isInstanceOf(AiFailure.class);
        condition.putArray("sourceIds");condition.putArray("engineEvidenceIds");assertThatThrownBy(()->contract.validate(report,input)).isInstanceOf(AiFailure.class);
    }
    @Test void sameEvidenceAndEngineCannotUpgradePreviousOverallInterpretation() {
        var contract=new NewsInsightContract(fixture.json);var input=signalInput();var report=signalReport("SUPPORTED","SUPPORTS");
        var policy=(ObjectNode)input.path("policy");var evidence=fixture.json.tree(Map.of("sources",policy.path("sources")));var old=signalReport("CHECK_REQUIRED","SUPPORTS");
        var previous=new NewsResearch(fixture.json).previous(fixture.json.tree(Map.of("engine",policy.path("engine"),"report",old,"evidence",evidence)));policy.set("previousResearch",previous);
        assertThatThrownBy(()->contract.validate(report,input)).isInstanceOf(AiFailure.class);((ObjectNode)report.path("signalInterpretation")).put("stance","CHECK_REQUIRED");contract.validate(report,input);
    }
    @Test void combinedInterpretationUsesOneCallAndSavedReadsNeverCallAi() {
        String id=submit(fixture.snapshot(),"one-combined-call").id();var ai=mock(AiReviewPort.class);var collector=mock(NewsEvidenceCollector.class);
        when(collector.collect(any())).thenReturn(evidence());
        try(var worker=worker(ai,collector)) {
            when(ai.assess(any())).thenReturn(new MarketResponse(signalReport("CHECK_REQUIRED","UNCERTAIN"),"provider",fixture.json.tree(Map.of()),1,1));worker.tick();
        }
        var saved=fixture.service.get(id,"DECISION");assertThat(saved.status()).describedAs("error=%s",saved.errorCode()).isEqualTo("SUCCEEDED");assertThat(saved.result().path("signalContext").path("calendarDayGap").asLong()).isEqualTo(1);
        assertThat(saved.result().path("report").path("signalInterpretation").path("stance").asText()).isEqualTo("CHECK_REQUIRED");
        fixture.service.get(id,"DECISION");verify(ai,times(1)).assess(any());verify(ai,never()).review(any());verify(collector,times(1)).collect(any());
    }
    @Test void idempotencyAndPublicInputsDoNotDependOnHoldingsOrPriceHistory() {
        String snapshot=fixture.snapshot();var first=submit(snapshot,"same-key");assertThat(submit(snapshot,"same-key").id()).isEqualTo(first.id());
        assertThat(submit(snapshot,"explicit-refresh").id()).isNotEqualTo(first.id());
        assertThatThrownBy(()->fixture.service.insights(snapshot,new DecisionService.InsightRequest(7,""),"same-key")).isInstanceOf(RuntimeException.class);
        var input=fixture.store.find(fixture.settings.ownerScope(),first.id()).orElseThrow().input();
        assertThat(input.path("mode").asText()).isEqualTo("NEWS_INSIGHTS");assertThat(input.path("stock").path("stockName").asText()).isEqualTo("삼성전자");
        assertThat(input.toString()).doesNotContain("averageBuyPrice","positionStatus","aggressive");
        assertThatThrownBy(()->fixture.service.insights(snapshot,new DecisionService.InsightRequest(31,""),"bad")).isInstanceOf(RuntimeException.class);
    }
    @Test void frozenEvidenceSurvivesRetryAndReadsNeverRecollect() {
        String id=submit(fixture.snapshot(),"retry-news").id();var ai=mock(AiReviewPort.class);var collector=mock(NewsEvidenceCollector.class);
        when(collector.collect(any())).thenReturn(evidence());
        try(var worker=worker(ai,collector)) {
            when(ai.assess(any())).thenThrow(new AiFailure("AI_HTTP_429",true)).thenReturn(new MarketResponse(report(),"news-provider",fixture.json.tree(Map.of()),11,12));
            worker.tick();assertThat(fixture.service.get(id,"DECISION").status()).isEqualTo("RETRYABLE_FAILED");fixture.clock.advance(20_000);worker.tick();
            var saved=fixture.service.get(id,"DECISION");assertThat(saved.status()).isEqualTo("SUCCEEDED");assertThat(saved.action()).isNull();
            assertThat(saved.result().path("report").path("summary")).isEqualTo(report().path("summary"));assertThat(saved.result().path("evidence").path("sources").get(0).path("excerpt")).isEqualTo(evidence().path("sources").get(0).path("excerpt"));
            assertThat(saved.result().path("report").path("events").get(0).path("evidenceQuality").path("level").asText()).isEqualTo("SNIPPET");
            fixture.service.get(id,"DECISION");verify(collector,times(1)).collect(any());verify(ai,times(2)).assess(any());verify(ai,never()).review(any());
        }
    }
    @Test void noEvidenceSavesCoverageWithoutInventingAnAiReport() {
        String id=submit(fixture.snapshot(),"empty").id();var ai=mock(AiReviewPort.class);var collector=mock(NewsEvidenceCollector.class);
        ObjectNode empty=(ObjectNode)evidence();empty.putArray("sources");when(collector.collect(any())).thenReturn(empty);
        try(var worker=worker(ai,collector)) {worker.tick();}
        var result=fixture.service.get(id,"DECISION").result();assertThat(result.path("report").path("events")).isEmpty();verify(ai,never()).assess(any());
    }
    @Test void fabricatedQuotesUnknownReferencesAndTitleOnlyStrongConclusionsAreRejected() {
        var contract=new NewsInsightContract(fixture.json);ObjectNode input=(ObjectNode)fixture.json.tree(Map.of("policy",Map.of("sources",evidence().path("sources"),"engine",Map.of("evidence",Map.of("SCREENER",Map.of())))));
        contract.validate(report(),input);
        ObjectNode bad=report();((ObjectNode)bad.path("events").get(0)).put("factQuote","제공되지 않은 계약 체결 금액입니다.");assertThatThrownBy(()->contract.validate(bad,input)).isInstanceOf(AiFailure.class);
        ObjectNode unknown=report();unknown.putArray("summarySourceIds").add("S99");assertThatThrownBy(()->contract.validate(unknown,input)).isInstanceOf(AiFailure.class);
        ((ObjectNode)input.path("policy").path("sources").get(0)).put("depth","TITLE_ONLY");ObjectNode confident=report();((ObjectNode)confident.path("events").get(0)).put("engineRelation","SUPPORTS");assertThatThrownBy(()->contract.validate(confident,input)).isInstanceOf(AiFailure.class);
    }
    @Test void staleWorkerCannotChangeCollectedEvidence() {
        String id=submit(fixture.snapshot(),"fence").id();var old=fixture.store.claim(fixture.settings.ownerScope(),"old",fixture.clock.millis(),100).orElseThrow();
        fixture.clock.advance(101);var fresh=fixture.store.claim(fixture.settings.ownerScope(),"new",fixture.clock.millis(),1000).orElseThrow();
        assertThat(fixture.store.recordNews(old,evidence(),null,fixture.clock.millis())).isFalse();assertThat(fixture.store.recordNews(fresh,evidence(),null,fixture.clock.millis())).isTrue();
        assertThat(fixture.store.newsEvidence(fresh)).contains(evidence());assertThat(fresh.id()).isEqualTo(id);
    }
    @Test void previousReportLinksOnlyToNewCollectedDocuments() {
        String snapshot=fixture.snapshot();var collector=mock(NewsEvidenceCollector.class);when(collector.collect(any())).thenReturn(evidence());var ai=mock(AiReviewPort.class);
        try(var worker=worker(ai,collector)) {
            when(ai.assess(any())).thenReturn(new MarketResponse(report(),"provider",fixture.json.tree(Map.of()),1,1));
            String first=submit(snapshot,"first").id();worker.tick();fixture.clock.advance(1000);String next=submit(snapshot,"next").id();worker.tick();
            var changes=fixture.service.get(next,"DECISION").result().path("changes");assertThat(changes.path("previousId").asText()).isEqualTo(first);assertThat(changes.path("newSourceIds")).isEmpty();
        }
    }
    @Test void officialImportanceNeedsAvailableMetricAndPreviousLinksMustExist() {
        var contract=new NewsInsightContract(fixture.json);ObjectNode input=(ObjectNode)fixture.json.tree(Map.of("policy",Map.of("sources",evidence().path("sources"),"engine",Map.of("evidence",Map.of("SCREENER",Map.of())))));
        ObjectNode excessive=report();((ObjectNode)excessive.path("events").get(0).path("importance")).put("level","HIGH").put("basis","QUALITATIVE");
        assertThatThrownBy(()->contract.validate(excessive,input)).isInstanceOf(AiFailure.class);
        var background=(ObjectNode)fixture.json.tree(Map.of("id","S2","depth","OFFICIAL_DOCUMENT","title","사업보고서","contextOutsideWindow",true));
        ((com.fasterxml.jackson.databind.node.ArrayNode)input.path("policy").path("sources")).add(background);
        ((com.fasterxml.jackson.databind.node.ArrayNode)excessive.path("events").get(0).path("sourceIds")).add("S2");
        assertThatThrownBy(()->contract.validate(excessive,input)).isInstanceOf(AiFailure.class);
        ((ObjectNode)input.path("policy").path("sources").get(0)).put("depth","OFFICIAL_DOCUMENT");contract.validate(excessive,input);
        var importance=(ObjectNode)excessive.path("events").get(0).path("importance");importance.put("basis","QUANTITATIVE");importance.putArray("metricIds").add("M99");
        assertThatThrownBy(()->contract.validate(excessive,input)).isInstanceOf(AiFailure.class);
        ObjectNode invented=report();((ObjectNode)invented.path("events").get(0)).put("previousEventId","UNKNOWN").put("changeType","STRENGTHENED");
        assertThatThrownBy(()->contract.validate(invented,input)).isInstanceOf(AiFailure.class);
    }
    @Test void documentAndEventVersionsAreAtomicAndSameDocumentKeepsIdentity() {
        String snapshot=fixture.snapshot();var collector=mock(NewsEvidenceCollector.class);when(collector.collect(any())).thenReturn(evidence());var ai=mock(AiReviewPort.class);
        try(var worker=worker(ai,collector)) {
            when(ai.assess(any())).thenAnswer(invocation->{var request=(JsonNode)invocation.getArgument(0);ObjectNode report=report();var old=request.path("policy").path("previousResearch").path("events");if(!old.isMissingNode() && !old.isEmpty()) ((ObjectNode)report.path("events").get(0)).put("previousEventId",old.get(0).path("eventId").asText()).put("changeType","UNCHANGED");return new MarketResponse(report,"provider",fixture.json.tree(Map.of()),1,1);});
            String first=submit(snapshot,"archive-one").id();worker.tick();fixture.clock.advance(1000);String second=submit(snapshot,"archive-two").id();worker.tick();
            var old=fixture.service.get(first,"DECISION").result().path("report").path("events").get(0);var latest=fixture.service.get(second,"DECISION").result().path("report").path("events").get(0);
            assertThat(latest.path("eventId")).isEqualTo(old.path("eventId"));assertThat(latest.path("linkStatus").asText()).isEqualTo("SAME_DOCUMENT");assertThat(latest.path("documentChange").asText()).isEqualTo("SAME_CONTENT");
            var jdbc=new org.springframework.jdbc.core.JdbcTemplate(fixture.ds);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM td_research_document",Integer.class)).isEqualTo(1);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM td_research_event",Integer.class)).isEqualTo(2);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM td_research_evidence",Integer.class)).isEqualTo(2);
        }
    }
    @Test void identicalDocumentsAndEngineCannotInventStrengthening() {
        var research=new NewsResearch(fixture.json);var evidence=research.prepare(evidence(),"005930");var engine=fixture.json.tree(Map.of("evidence",Map.of("SCREENER",Map.of())));
        var previous=research.previous(fixture.json.tree(Map.of("report",research.enrichReport(report(),evidence,fixture.json.tree(Map.of()),"first",engine),"evidence",evidence,"engine",engine)));
        var input=fixture.json.tree(Map.of("policy",Map.of("sources",research.modelSources(evidence),"engine",engine,"previousResearch",previous)));
        var next=report();var event=(ObjectNode)next.path("events").get(0);event.put("previousEventId",previous.path("events").get(0).path("eventId").asText()).put("changeType","STRENGTHENED");
        var contract=new NewsInsightContract(fixture.json);assertThatThrownBy(()->contract.validate(next,input)).isInstanceOf(AiFailure.class);
        event.put("changeType","UNCHANGED");contract.validate(next,input);
    }
    @Test void archiveFailureRollsBackResultAndDocumentsAndExpiredLeaseCannotArchive() {
        String id=submit(fixture.snapshot(),"archive-rollback").id();var job=fixture.store.claim(fixture.settings.ownerScope(),"worker",fixture.clock.millis(),100).orElseThrow();
        var research=new NewsResearch(fixture.json);var evidence=research.prepare(evidence(),"005930");
        var report=research.enrichReport(report(),evidence,fixture.json.tree(Map.of()),id,fixture.json.tree(Map.of()));
        ((ObjectNode)report.path("events").get(0)).put("eventVersionId","x".repeat(65));
        var result=fixture.json.tree(Map.of("generationMode","NEWS_INSIGHTS","evidence",evidence,"report",report));
        assertThatThrownBy(()->fixture.store.complete(job,result,fixture.clock.millis())).isInstanceOf(org.springframework.dao.DataAccessException.class);
        var jdbc=new org.springframework.jdbc.core.JdbcTemplate(fixture.ds);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM td_research_document",Integer.class)).isZero();
        assertThat(fixture.service.get(id,"DECISION").status()).isEqualTo("RUNNING");
        fixture.clock.advance(101);assertThat(fixture.store.complete(job,result,fixture.clock.millis())).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM td_research_event",Integer.class)).isZero();
    }
    @Test void originalCacheIsOwnerScopedExpiresFromFetchAndFullBodyStaysOffApi() {
        String id=submit(fixture.snapshot(),"cached-original").id();var job=fixture.store.claim(fixture.settings.ownerScope(),"worker",fixture.clock.millis(),1000).orElseThrow();
        var raw=(ObjectNode)evidence();var source=(ObjectNode)raw.path("sources").get(0);long fetched=fixture.clock.millis();source.put("body","원문 전체 서버 저장").put("originalFetchedEpoch",fetched);
        var research=new NewsResearch(fixture.json);var evidence=research.prepare(raw,"005930");
        var result=fixture.json.tree(Map.of("generationMode","NEWS_INSIGHTS","evidence",evidence,"report",research.enrichReport(report(),evidence,fixture.json.tree(Map.of()),id,fixture.json.tree(Map.of()))));
        assertThat(fixture.store.complete(job,result,fixture.clock.millis())).isTrue();
        assertThat(fixture.service.get(id,"DECISION").result().path("evidence").path("sources").get(0).has("body")).isFalse();
        assertThat(fixture.store.cachedResearchDocument(job.owner(),"005930","https://example.com/news",fetched-1)).isPresent();
        assertThat(fixture.store.cachedResearchDocument("another-owner","005930","https://example.com/news",fetched-1)).isEmpty();
        new org.springframework.jdbc.core.JdbcTemplate(fixture.ds).update("UPDATE td_research_document SET last_seen_epoch=?",fetched+1000);
        assertThat(fixture.store.cachedResearchDocument(job.owner(),"005930","https://example.com/news",fetched+1)).isEmpty();
    }
    @Test void missingMigrationIsExplainedBeforeAnyNewJobOrExternalWork() {
        var without=new DecisionWorkflowTest();without.setup();String snapshot=without.snapshot();
        assertThatThrownBy(()->without.service.insights(snapshot,new DecisionService.InsightRequest(14,""),"migration-missing")).hasMessageContaining("20261011_01_news_research.sql");
        assertThat(without.service.history("005930",0,20).items()).isEmpty();
    }
}
