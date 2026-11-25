package com.example.stockanalysis.decision;

import static com.example.stockanalysis.decision.DecisionModels.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.stockanalysis.analysis.domain.DataQualityStatus;
import com.example.stockanalysis.engine.model.*;
import com.example.stockanalysis.engine.screener.ScreenerBar;
import com.example.stockanalysis.shared.exception.*;
import com.example.stockanalysis.unified.application.*;
import com.example.stockanalysis.unified.internal.DefaultUnifiedStockAnalysisEngine;
import com.example.stockanalysis.unified.model.UnifiedAnalysisInput;
import com.example.stockanalysis.unified.scan.UnifiedScanStore;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.core.JdbcTemplate;

class DecisionWorkflowTest {
    DecisionJson json; DecisionStore store; DecisionService service; DecisionPolicy policy; AiReviewContract contract;
    DecisionSettings settings; DriverManagerDataSource ds; MutableClock clock; UnifiedAnalysisResponse data;
    @BeforeEach void setup() {
        ds=new DriverManagerDataSource("jdbc:h2:mem:decision_"+UUID.randomUUID()+";MODE=Oracle;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000","sa","");
        new ResourceDatabasePopulator(new FileSystemResource(Path.of("..","database","oracle","ddl","12_trade_decision.sql"))).execute(ds);
        json=new DecisionJson(new ObjectMapper().registerModule(new JavaTimeModule()));
        store=new DecisionStore(ds,json); settings=settings("model-snapshot-test"); clock=new MutableClock();
        policy=new DecisionPolicy(json);contract=new AiReviewContract(json); service=service(settings);
        List<ScreenerBar> bars=new ArrayList<>(); LocalDate end=LocalDate.of(2026,10,5);
        for(int i=0;i<300;i++) {
            var p=BigDecimal.valueOf(1000+100*Math.sin(i*Math.PI/10));var d=end.minusDays(299-i);
            bars.add(new ScreenerBar(d.atStartOfDay(),d.plusDays(1).atStartOfDay(),p,p.add(BigDecimal.TEN),p.subtract(BigDecimal.TEN),p,1000000,null));
        }
        var analyzed=new DefaultUnifiedStockAnalysisEngine().analyze(new UnifiedAnalysisInput("005930","삼성전자","KOSPI",LocalDateTime.of(2026,10,6,4,0),bars,Map.of()));
        ObjectNode tree=(ObjectNode)json.tree(new UnifiedAnalysisResponse(analyzed,DataQualityStatus.COMPLETE,List.of()));
        ((ObjectNode)tree.path("result").path("analysis")).put("totalScore",70);
        ((ObjectNode)tree.path("result").path("screener")).put("quality","VALID");
        ((ObjectNode)tree.path("result").path("analysis").path("signalReport")).put("completeness","READY").set("signals",json.tree(List.of()));
        var history=bars.stream().map(b->new DailyCandle(b.openTime().toLocalDate(),b.open(),b.high(),b.low(),b.close(),b.volume())).toList();
        tree.set("priceHistory",json.tree(history));
        data=json.convert(tree,UnifiedAnalysisResponse.class);
    }
    DecisionSettings settings(String model) { return new DecisionSettings(true,"test-owner",model,"test-secret",100,30,600000,128000,12000); }
    DecisionService service(DecisionSettings s) { return new DecisionService(store,s,json,policy,contract,clock); }
    String snapshot() {
        var view=service.snapshot("005930",new SnapshotRequest("COLLECT",null),UUID.randomUUID().toString());
        var claimed=store.claim(settings.ownerScope(),"snapshot-worker",clock.millis(),180000).orElseThrow();
        assertThat(claimed.id()).isEqualTo(view.id());
        assertThat(store.complete(claimed,json.tree(Map.of("analysis",data,"origin",Map.of("source","COLLECT"))),clock.millis())).isTrue();return view.id();
    }
    View decide(String snapshot,String key) { return service.decision(snapshot,new ReviewRequest(Position.NOT_HELD,10),key); }
    Review validReview(JsonNode input) {
        return new Review("WAIT",null,
                List.of(new Reason("SIGNALS","진입 신호 확인이 필요합니다.")),List.of(new Reason("TREND","추세가 바뀔 수 있습니다.")),
                List.of(new Scenario("UPSIDE","추세를 재검토합니다."),new Scenario("RANGE","관찰을 유지합니다."),new Scenario("DOWNSIDE","진입을 보류합니다.")),
                List.of("뉴스 정보가 제공되지 않았습니다."),List.of("UPSIDE","DOWNSIDE"),ReviewTestFixtures.detail(),ReviewTestFixtures.independent(),ReviewTestFixtures.personal());
    }
    DecisionWorker worker(AiReviewPort ai) { when(ai.assess(any())).thenReturn(new MarketResponse(ReviewTestFixtures.market(),"market",json.tree(Map.of()),10,20));return new DecisionWorker(store,settings,json,mock(UnifiedAnalysisService.class),mock(UnifiedScanStore.class),ai,contract,policy,clock); }

    @Test void sameRequestAndSameContentReturnOneJobAndKeepFrozenModelAcrossDeployments() {
        String id=snapshot(); View first=decide(id,"key-a");
        assertThat(decide(id,"key-a").id()).isEqualTo(first.id());
        assertThat(decide(id,"key-b").id()).isEqualTo(first.id());
        var changed=service(settings("different-model"));
        assertThat(changed.decision(id,new ReviewRequest(Position.NOT_HELD,10),"key-a").id()).isEqualTo(first.id());
        assertThat(changed.decision(id,new ReviewRequest(Position.NOT_HELD,10),"new-key").id()).isNotEqualTo(first.id());
        assertThatThrownBy(()->service.decision(id,new ReviewRequest(Position.HELD,10),"key-a")).isInstanceOf(BusinessException.class);
        assertThat(service.decision(id,new ReviewRequest(Position.NOT_HELD,20),"different-horizon").id()).isNotEqualTo(first.id());
    }
    @Test void concurrentSubmissionsConvergeWithDifferentRequestKeys() throws Exception {
        String snapshot=snapshot(); var pool=Executors.newFixedThreadPool(8); var gate=new CountDownLatch(1);
        try {
            var futures=new ArrayList<Future<String>>();
            for(int i=0;i<8;i++) { final int n=i; futures.add(pool.submit(()->{gate.await();return decide(snapshot,"parallel-"+n).id();})); }
            gate.countDown(); Set<String> ids=new HashSet<>(); for(var f:futures) ids.add(f.get(15,TimeUnit.SECONDS));
            assertThat(ids).hasSize(1);
            assertThat(new JdbcTemplate(ds).queryForObject("SELECT COUNT(*) FROM td_job WHERE kind='DECISION'",Integer.class)).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }
    @Test void expiredLeaseCannotOverwriteNewOwnerOrCommittedResult() {
        String id=decide(snapshot(),"lease").id();
        Job old=store.claim(settings.ownerScope(),"old",clock.millis(),100).orElseThrow();
        clock.advance(101);
        Job fresh=store.claim(settings.ownerScope(),"fresh",clock.millis(),1000).orElseThrow();
        assertThat(fresh.attempt()).isEqualTo(2);
        assertThat(store.heartbeat(old,clock.millis(),1000)).isFalse();
        assertThat(store.complete(old,json.tree(Map.of("finalAction","BUY")),clock.millis())).isFalse();
        assertThat(store.complete(fresh,json.tree(Map.of("finalAction","WAIT")),clock.millis())).isTrue();
        assertThat(store.complete(old,json.tree(Map.of("finalAction","SELL")),clock.millis())).isFalse();
        assertThat(service.get(id,"DECISION").action()).isEqualTo("WAIT");
    }
    @Test void processRestartPicksUpCommittedQueueAndReadDoesNotCallAiAgain() {
        String id=decide(snapshot(),"work").id(); AiReviewPort ai=mock(AiReviewPort.class);
        when(ai.review(any())).thenAnswer(i->{var r=validReview(i.getArgument(0));return new AiResponse(r,"provider",json.tree(r),10,20);});
        try(var ignored=new WorkerCloser(worker(ai))) { ignored.worker.tick(); }
        var result=service.get(id,"DECISION"); assertThat(result.status()).isEqualTo("SUCCEEDED");assertThat(result.action()).isEqualTo("WAIT");
        assertThat(result.result().path("winProbability").isNull()).isTrue();
        assertThat(service.get(id,"DECISION").result()).isEqualTo(result.result());
        assertThat(service.history("005930",0,20).items()).hasSize(1);verify(ai,times(1)).review(any());
    }
    @Test void unknownPositionSkipsAiAndIsNotWaitOrFailure() {
        var view=service.decision(snapshot(),new ReviewRequest(Position.UNKNOWN,10),"unknown"); AiReviewPort ai=mock(AiReviewPort.class);
        try(var w=new WorkerCloser(worker(ai))) {w.worker.tick();}
        assertThat(service.get(view.id(),"DECISION").action()).isEqualTo("UNDETERMINED");verifyNoInteractions(ai);
    }
    @Test void providerMigrationPreservesOldRequestsAndRejectsLegacyQueueWithoutAiCall() {
        String snapshot=snapshot();var legacy=decide(snapshot,"legacy-key");
        ObjectNode input=(ObjectNode)store.find(settings.ownerScope(),legacy.id()).orElseThrow().input();
        ((ObjectNode)input.path("versions")).put("provider","openai-responses");
        new JdbcTemplate(ds).update("UPDATE td_job SET input_json=?,content_key=? WHERE job_id=?",json.write(input),json.hash(Map.of("owner",settings.ownerScope(),"input",input)),legacy.id());
        clock.advance(1000);
        var fresh=decide(snapshot,"gemini-key");
        assertThat(fresh.id()).isNotEqualTo(legacy.id());
        assertThat(decide(snapshot,"legacy-key").id()).isEqualTo(legacy.id());
        assertThat(store.find(settings.ownerScope(),fresh.id()).orElseThrow().input().path("versions").path("provider").asText()).isEqualTo(GeminiReviewAdapter.PROVIDER);
        AiReviewPort ai=mock(AiReviewPort.class);
        try(var w=new WorkerCloser(worker(ai))) {w.worker.tick();}
        assertThat(service.get(legacy.id(),"DECISION").errorCode()).isEqualTo("AI_PROVIDER_CHANGED");verifyNoInteractions(ai);
    }
    @Test void providerFailureIsNotAWaitOpinionAndRetriesAreBounded() {
        String id=decide(snapshot(),"failure").id(); AiReviewPort ai=mock(AiReviewPort.class);
        when(ai.review(any())).thenThrow(new AiFailure("AI_HTTP_429",true,60000));
        try(var w=new WorkerCloser(worker(ai))) {
            w.worker.tick(); assertThat(service.get(id,"DECISION").status()).isEqualTo("RETRYABLE_FAILED");
            w.worker.tick(); verify(ai,times(1)).review(any());
            clock.advance(60001); w.worker.tick();clock.advance(60001);w.worker.tick();
        }
        View result=service.get(id,"DECISION");assertThat(result.status()).isEqualTo("FAILED");assertThat(result.action()).isNull();assertThat(result.result()).isNull();
        assertThat(result.retryable()).isFalse();verify(ai,times(3)).review(any());
    }
    @Test void invalidEvidenceAndForbiddenSellFailValidation() {
        JsonNode input=store.find(settings.ownerScope(),decide(snapshot(),"invalid").id()).orElseThrow().input();
        ObjectNode bad=(ObjectNode)json.tree(validReview(input)); ((ObjectNode)bad.path("supportingReasons").get(0)).put("evidenceId","MADE_UP_NEWS");
        final JsonNode invalidEvidence=bad;
        assertThatThrownBy(()->contract.validate(contract.parse(invalidEvidence),input)).isInstanceOf(AiFailure.class);
        bad=(ObjectNode)json.tree(validReview(input));bad.put("recommendation","SELL"); final JsonNode invalidAction=bad;
        assertThatThrownBy(()->contract.validate(contract.parse(invalidAction),input)).isInstanceOf(AiFailure.class);
        ObjectNode numeric=(ObjectNode)json.tree(validReview(input));((ObjectNode)numeric.path("supportingReasons").get(0)).put("explanation","목표 90000원");
        assertThatThrownBy(()->contract.parse(numeric)).isInstanceOf(AiFailure.class);
        ObjectNode extra=(ObjectNode)json.tree(validReview(input));extra.put("winProbability",0.7);
        assertThatThrownBy(()->contract.parse(extra)).isInstanceOf(AiFailure.class);
    }
    @Test void differentOwnerCannotReadHistoryOrFindJobAndSecretsAreNotRendered() {
        String id=decide(snapshot(),"scope").id();
        assertThat(store.find("another-owner",id)).isEmpty();assertThat(store.history("another-owner","005930",0,20).items()).isEmpty();
        assertThat(settings.toString()).doesNotContain("test-secret");
    }
    @Test void detailedReviewRejectsInventedMetricsOrMissingHorizonGrounding() {
        var input=store.find(settings.ownerScope(),decide(snapshot(),"detail").id()).orElseThrow().input();
        ObjectNode review=(ObjectNode)json.tree(validReview(input));
        ((ObjectNode)review.path("detail").path("exitPlan")).set("metricIds",json.tree(List.of("INVENTED_STOP")));
        assertThatThrownBy(()->contract.validate(contract.parse(review),input)).isInstanceOf(AiFailure.class);
        ((ObjectNode)review.path("detail").path("exitPlan")).set("metricIds",json.tree(List.of("CURRENT")));
        contract.validate(contract.parse(review),input);
        ((ObjectNode)review.path("detail").path("horizonAssessment")).set("evidenceIds",json.tree(List.of("TREND")));
        assertThatThrownBy(()->contract.validate(contract.parse(review),input)).isInstanceOf(AiFailure.class);
        var missing=(ObjectNode)json.tree(validReview(input));missing.set("missingInformation",json.tree(List.of("52주 자료가 부족합니다.")));
        assertThatThrownBy(()->contract.parse(missing)).isInstanceOf(AiFailure.class);
    }
    @Test void optionalHoldingInputsChangeIdentityAndOldTwoFieldRequestKeyStillReplays() {
        String id=snapshot();var original=service.decision(id,new ReviewRequest(Position.HELD,10),"held-original");
        var updated=service.decision(id,new ReviewRequest(Position.HELD,10,BigDecimal.valueOf(900),data.result().priceDate()),"held-updated");
        assertThat(updated.id()).isNotEqualTo(original.id());
        assertThat(service.decision(id,new ReviewRequest(Position.HELD,10),"held-original").id()).isEqualTo(original.id());
        assertThatThrownBy(()->service.decision(id,new ReviewRequest(Position.HELD,10,BigDecimal.valueOf(900),null),"held-original")).isInstanceOf(BusinessException.class);
    }
    @Test void revisionCreatesNewResultWithoutReplacingOriginal() {
        var first=decide(snapshot(),"original"); Job job=store.claim(settings.ownerScope(),"owner",clock.millis(),1000).orElseThrow();
        store.complete(job,policy.finish(job.input(),validReview(job.input()),clock.millis()),clock.millis());
        var revised=service.revision(first.id(),"revision-key");
        assertThat(revised.id()).isNotEqualTo(first.id());assertThat(revised.parentId()).isEqualTo(first.id());
        assertThat(service.revision(first.id(),"revision-key").id()).isEqualTo(revised.id());
        assertThat(service.get(first.id(),"DECISION").status()).isEqualTo("SUCCEEDED");
    }
    @Test void tokenReservationsAreAtomicAndNeverExceeded() throws Exception {
        var pool=Executors.newFixedThreadPool(6);
        try {
            List<Callable<Boolean>> calls=new ArrayList<>();for(int i=0;i<10;i++) calls.add(()->store.reserveCall("budget","2026-10-06",100,1000,400));
            int accepted=0;for(var f:pool.invokeAll(calls)) if(f.get()) accepted++;
            assertThat(accepted).isEqualTo(2);
        } finally {pool.shutdownNow();}
    }
    @Test void canonicalJsonIgnoresFieldOrderAndNumericScaleButPreservesArrayOrder() {
        assertThat(json.hash(json.read("{\"b\":1.00,\"a\":2}"))).isEqualTo(json.hash(json.read("{\"a\":2.0,\"b\":1}")));
        assertThat(json.hash(List.of(1,2))).isNotEqualTo(json.hash(List.of(2,1)));
    }
    @Test void insufficientDataBlocksEvenIfPricePlanExists() {
        JsonNode p=policy.prepare(new UnifiedAnalysisResponse(data.result(),DataQualityStatus.PARTIAL,List.of("누락")),new ReviewRequest(Position.NOT_HELD,10));
        assertThat(p.path("engineActions").toString()).isEqualTo("[\"UNDETERMINED\"]");
    }
    @Test void inactiveBuySignalsAndOutOfPlanPricesCannotAuthorizeBuy() {
        assertThat(policy.prepare(data,new ReviewRequest(Position.NOT_HELD,10)).path("engineActions").toString()).doesNotContain("BUY");
        assertThat(policy.prepare(data,new ReviewRequest(Position.HELD,10)).path("engineActions").toString()).contains("HOLD").doesNotContain("SELL");
    }
    @Test void buyNeedsBothSignalAndScenarioAndSellNeedsHeldPosition() {
        var signal=new TradeSignal("buy","BREAKOUT",TradeSignal.Side.BUY,TradeSignal.State.ACTIVE,
                LocalDate.of(2026,10,5),LocalDate.of(2026,10,5),0,5,BigDecimal.valueOf(1000),BigDecimal.valueOf(900),
                BigDecimal.valueOf(900),BigDecimal.valueOf(1100),"IN_RANGE",null,List.of());
        ObjectNode tree=(ObjectNode)json.tree(data);ObjectNode a=(ObjectNode)tree.path("result").path("analysis");
        a.set("scenario",json.tree(new TradeScenario(BigDecimal.valueOf(900),BigDecimal.valueOf(1100),List.of(BigDecimal.valueOf(1200)),BigDecimal.valueOf(850))));
        ((ObjectNode)a.path("signalReport")).set("signals",json.tree(List.of(signal)));
        var valid=json.convert(tree,UnifiedAnalysisResponse.class);
        assertThat(policy.prepare(valid,new ReviewRequest(Position.NOT_HELD,10)).path("engineActions").toString()).contains("BUY");
        ((ObjectNode)tree.path("result").path("stock")).put("currentPrice",1200);
        assertThat(policy.prepare(json.convert(tree,UnifiedAnalysisResponse.class),new ReviewRequest(Position.NOT_HELD,10)).path("engineActions").toString()).doesNotContain("BUY");
        ((ObjectNode)tree.path("result").path("stock")).set("currentPrice",json.tree(data.result().stock().currentPrice()));
        ((ObjectNode)a.path("signalReport").path("signals").get(0)).put("side","SELL");
        var sell=json.convert(tree,UnifiedAnalysisResponse.class);
        assertThat(policy.prepare(sell,new ReviewRequest(Position.HELD,10)).path("engineActions").toString()).contains("SELL");
        assertThat(policy.prepare(sell,new ReviewRequest(Position.NOT_HELD,10)).path("engineActions").toString()).doesNotContain("BUY","SELL");
    }
    @Test void storedUnifiedItemIsCopiedWithoutCollectingOrCallingAi() {
        var view=service.snapshot("005930",new SnapshotRequest("UNIFIED_RUN_ITEM","existing-run"),"import");
        var analyzer=mock(UnifiedAnalysisService.class);var scans=mock(UnifiedScanStore.class);var ai=mock(AiReviewPort.class);
        when(scans.detail("existing-run","005930")).thenReturn(Optional.of(data));
        try(var w=new WorkerCloser(new DecisionWorker(store,settings,json,analyzer,scans,ai,contract,policy,clock))) {w.worker.tick();}
        var saved=service.get(view.id(),"SNAPSHOT");assertThat(saved.status()).isEqualTo("SUCCEEDED");
        assertThat(saved.result().path("origin").path("rawDataAvailable").asBoolean()).isFalse();
        assertThat(saved.result().path("analysis").path("result").path("stock").path("stockCode").asText()).isEqualTo("005930");
        verifyNoInteractions(analyzer,ai);
    }
    @Test void popupLookupIsReadOnlyAndImportsDeduplicateAcrossClientKeys() {
        assertThat(service.importedSnapshot("005930","source-run")).isNull();
        assertThat(new JdbcTemplate(ds).queryForObject("SELECT COUNT(*) FROM td_job",Integer.class)).isZero();
        var first=service.snapshot("005930",new SnapshotRequest("UNIFIED_RUN_ITEM","source-run"),"browser-one");
        var second=service.snapshot("005930",new SnapshotRequest("UNIFIED_RUN_ITEM","source-run"),"browser-two");
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(service.importedSnapshot("005930","source-run").id()).isEqualTo(first.id());
        assertThat(service.importedSnapshot("005930","other-run")).isNull();
        assertThat(store.importedSnapshot("other-owner","005930","source-run")).isEmpty();
    }
    @Test void popupRestoresJudgmentEvenWhenLegacyDuplicateImportIsNewer() {
        var first=service.snapshot("005930",new SnapshotRequest("UNIFIED_RUN_ITEM","legacy-run"),"first-import");
        var claimed=store.claim(settings.ownerScope(),"snapshot-worker",clock.millis(),180000).orElseThrow();
        assertThat(store.complete(claimed,json.tree(Map.of("analysis",data,"origin",Map.of("source","UNIFIED_RUN_ITEM","runId","legacy-run"))),clock.millis())).isTrue();
        var decision=decide(first.id(),"saved-judgment");
        clock.advance(1000);
        var duplicate=store.submit(settings.ownerScope(),"snapshot","legacy-import",json.hash("legacy-body"),json.hash("legacy-content"),
                "SNAPSHOT","005930",null,null,claimed.input(),clock.millis(),"2026-10-06",100);
        assertThat(duplicate.id()).isNotEqualTo(first.id());
        assertThat(service.importedSnapshot("005930","legacy-run").id()).isEqualTo(first.id());
        assertThat(service.history("005930",0,20,first.id()).items()).extracting(Summary::id).containsExactly(decision.id());
        assertThat(service.snapshot("005930",new SnapshotRequest("UNIFIED_RUN_ITEM","legacy-run"),"another-browser").id()).isEqualTo(first.id());
    }
    @Test void popupHistoryFiltersSnapshotBeforePagination() {
        String first=snapshot();String second=snapshot();
        var a=decide(first,"first-decision");
        service.decision(second,new ReviewRequest(Position.HELD,10),"second-decision");
        assertThat(service.history("005930",0,20,first).items()).extracting(Summary::id).containsExactly(a.id());
        assertThatThrownBy(()->service.history("000660",0,20,first)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.history("005930",0,20,"missing")).isInstanceOf(BusinessException.class);
    }
    @Test void freshSnapshotStoresInputsAndApiDoesNotExposeProvenance() {
        var view=service.snapshot("005930",new SnapshotRequest("COLLECT",null),"collect");
        var analyzer=mock(UnifiedAnalysisService.class);var ai=mock(AiReviewPort.class);
        var collected=new com.example.stockanalysis.analysis.application.model.CollectedAnalysisData(
                new AnalysisInput(data.result().stock(),AnalysisMode.SWING,data.result().priceDate(),clock.instant(),List.of(
                        new DailyCandle(data.result().priceDate(),BigDecimal.valueOf(1000),BigDecimal.valueOf(1100),BigDecimal.valueOf(900),BigDecimal.valueOf(1000),1000)),Map.of()),List.of(),List.of(),DataQualityStatus.COMPLETE);
        when(analyzer.captureAt(eq("005930"),any())).thenReturn(new UnifiedAnalysisService.CapturedAnalysis(data,collected));
        try(var w=new WorkerCloser(new DecisionWorker(store,settings,json,analyzer,mock(UnifiedScanStore.class),ai,contract,policy,clock))) {w.worker.tick();}
        assertThat(service.get(view.id(),"SNAPSHOT").status()).isEqualTo("SUCCEEDED");
        assertThat(store.find(settings.ownerScope(),view.id()).orElseThrow().result().has("provenance")).isTrue();
        assertThat(service.get(view.id(),"SNAPSHOT").result().has("provenance")).isFalse();verifyNoInteractions(ai);
    }
    @Test void manualRetryTokenIsIdempotentEvenAfterWorkerClaimsTheJob() {
        var view=decide(snapshot(),"manual");var first=store.claim(settings.ownerScope(),"one",clock.millis(),10000).orElseThrow();
        store.fail(first,"AI_CONNECTION_ERROR",true,clock.millis(),60000);
        assertThat(service.retry(view.id(),"retry-key").status()).isEqualTo("RETRYABLE_FAILED");
        var second=store.claim(settings.ownerScope(),"two",clock.millis(),10000).orElseThrow();
        assertThat(service.retry(view.id(),"retry-key").attempts()).isEqualTo(second.attempt());
    }
    static class MutableClock extends Clock {
        long now=Instant.parse("2026-10-06T01:00:00Z").toEpochMilli(); void advance(long ms){now+=ms;}
        public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return Instant.ofEpochMilli(now);}
    }
    record WorkerCloser(DecisionWorker worker) implements AutoCloseable {public void close(){worker.close();}}
}
