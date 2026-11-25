package com.example.stockanalysis.decision;

import com.example.stockanalysis.holdings.*;
import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class IntegratedDecisionTest {
    DecisionWorkflowTest f;HoldingService holdings;IntegratedDecisionService service;DecisionOutcomeStore outcomes;
    @BeforeEach void setup() {
        f=new DecisionWorkflowTest();f.setup();
        new ResourceDatabasePopulator(new FileSystemResource(Path.of("..","database","oracle","ddl","26_integrated_decision.sql"))).execute(f.ds);
        holdings=mock(HoldingService.class);when(holdings.single("005930")).thenReturn(new HoldingModels.Snapshot(f.clock.instant(),"AVAILABLE",Map.of()));
        var calendar=mock(com.example.stockanalysis.unified.eod.TradingCalendar.class);
        when(calendar.day(any())).thenAnswer(invocation->{LocalDate d=invocation.getArgument(0);return new com.example.stockanalysis.unified.eod.TradingCalendar.Day(d,
            Set.of(DayOfWeek.SATURDAY,DayOfWeek.SUNDAY).contains(d.getDayOfWeek())?com.example.stockanalysis.unified.eod.TradingCalendar.Status.CLOSED:com.example.stockanalysis.unified.eod.TradingCalendar.Status.OPEN,LocalTime.of(20,0),"test-calendar");});
        service=new IntegratedDecisionService(f.store,f.settings,f.json,holdings,f.clock,new DecisionValidityService(calendar,f.json),new AccountRiskService(new JdbcTemplate(f.ds),f.settings,f.clock));f.service.integrated(service);
        outcomes=new DecisionOutcomeStore(f.ds,f.json);
    }
    @Test void idempotentReplayFreezesBalanceAndNewRequestCapturesRevision() {
        String snapshot=f.snapshot();var first=service.create(snapshot,new IntegratedDecisionService.Request(5,null),"one",null);
        assertThat(first.input().path("mode").asText()).isEqualTo("INTEGRATED_DECISION");
        when(holdings.single("005930")).thenReturn(balance(2));
        assertThat(service.create(snapshot,new IntegratedDecisionService.Request(5,null),"one",null).id()).isEqualTo(first.id());
        verify(holdings,times(1)).single("005930");
        var second=service.create(snapshot,new IntegratedDecisionService.Request(5,null),"two",null);
        assertThat(second.input().path("policy").path("holding").path("holding").path("revision").asLong()).isEqualTo(2);
        assertThat(first.input().path("policy").path("holding").path("positionStatus").asText()).isEqualTo("NOT_HELD");
        assertThatThrownBy(()->service.create(snapshot,new IntegratedDecisionService.Request(6,null),"one",null)).isInstanceOf(BusinessException.class);
    }
    @Test void riskRevisionIsFrozenAndOnlyNewRequestsUseUpdatedAccountSettings() {
        new ResourceDatabasePopulator(new FileSystemResource(Path.of("..","database","oracle","ddl","27_account_risk.sql"))).execute(f.ds);
        var risk=new AccountRiskService(new JdbcTemplate(f.ds),f.settings,f.clock);
        risk.save(new AccountRiskModels.Edit(BigDecimal.valueOf(100000),BigDecimal.valueOf(10000),BigDecimal.valueOf(500),BigDecimal.valueOf(5),LocalDate.of(2026,10,6),0));
        String snapshot=f.snapshot();var first=service.create(snapshot,new IntegratedDecisionService.Request(5,null),"risk-one",null);
        risk.save(new AccountRiskModels.Edit(BigDecimal.valueOf(100000),BigDecimal.valueOf(5000),BigDecimal.valueOf(100),BigDecimal.valueOf(5),LocalDate.of(2026,10,6),1));
        var replay=service.create(snapshot,new IntegratedDecisionService.Request(5,null),"risk-one",null);
        assertThat(replay.id()).isEqualTo(first.id());assertThat(replay.input().path("policy").path("accountRisk").path("settings").path("revision").asLong()).isEqualTo(1);
        var second=service.create(snapshot,new IntegratedDecisionService.Request(5,null),"risk-two",null);
        assertThat(second.input().path("policy").path("accountRisk").path("settings").path("revision").asLong()).isEqualTo(2);
    }
    @Test void actualDecisionWorkerRoutesSingleIntegratedCallAndPersistsPlans() {
        var job=service.create(f.snapshot(),new IntegratedDecisionService.Request(1,null),"worker",null);
        var ai=mock(AiReviewPort.class);
        try(var worker=f.worker(ai)) {
            when(ai.assess(any())).thenReturn(new DecisionModels.MarketResponse(review("WAIT"),"response-id",f.json.tree(Map.of()),20,30));
            worker.orchestrator(new DecisionOrchestrator(f.store,f.json,f.clock));worker.tick();
        }
        assertThat(f.service.get(job.id(),"DECISION").status()).isEqualTo("SUCCEEDED");
        assertThat(outcomes.plans(f.settings.ownerScope(),job.id())).hasSize(2);
        assertThat(outcomes.plans(f.settings.ownerScope(),job.id())).allSatisfy(plan ->
                assertThat(plan.input().path("evalPolicyVersion").asText()).isEqualTo(PositionOutcomeEngine.VERSION));
        verify(ai,times(1)).assess(any());verify(ai,never()).review(any());
        assertThat(new JdbcTemplate(f.ds).queryForObject("SELECT call_count FROM td_budget WHERE owner_scope=? AND budget_day=?",Integer.class,f.settings.ownerScope(),"2026-10-06")).isEqualTo(1);
    }
    @Test void currentValidityReadPreservesTheStoredDecisionAndItsHash() {
        var job=service.create(f.snapshot(),new IntegratedDecisionService.Request(1,null),"immutable-validity",null);
        var claimed=f.store.claim(f.settings.ownerScope(),"one",f.clock.millis(),10000).orElseThrow();
        f.store.complete(claimed,new DecisionArbitrator(f.json).finish(job.input(),review("WAIT"),null,f.clock.millis()),f.clock.millis());
        var before=f.store.find(f.settings.ownerScope(),job.id()).orElseThrow();
        assertThat(service.currentValidity(job.id()).path("status").asText()).isEqualTo("VALID");
        f.clock.advance(Duration.ofHours(12).toMillis());
        assertThat(service.currentValidity(job.id()).path("status").asText()).isEqualTo("REVIEW_REQUIRED");
        var after=f.store.find(f.settings.ownerScope(),job.id()).orElseThrow();
        assertThat(after.resultHash()).isEqualTo(before.resultHash());assertThat(after.result()).isEqualTo(before.result());
        assertThatThrownBy(()->service.currentValidity("missing")).isInstanceOf(BusinessException.class);
    }
    @Test void heldQuantitiesFreezeForBothArmsAndEarlyClosuresContinueUntilBenchmarkIsObserved() {
        var fixture=new HeldPositionSizingTest();var p=fixture.held(60);String today=LocalDate.ofInstant(f.clock.instant(),DecisionService.SEOUL).toString();
        p.put("priceDate","2026-10-05");p.put("engineAction","REDUCE");p.put("horizonTradingDays",1);
        ((ObjectNode)p.path("holding").path("holding")).put("balanceDate",today);
        ((ObjectNode)p.path("accountRisk").path("settings")).put("balanceDate",today);
        ((ObjectNode)p.path("research")).put("collectedAt",f.clock.instant().toString());
        var input=f.json.tree(Map.of("mode","INTEGRATED_DECISION","policy",p,"versions",Map.of("sizing",PositionSizingPolicy.VERSION,"policy",DecisionContextBuilder.VERSION)));
        var job=f.store.submit(f.settings.ownerScope(),"held-test","held-key","hash","input-hash","DECISION","005930",f.snapshot(),null,input,f.clock.millis(),today,100);
        var claimed=f.store.claim(f.settings.ownerScope(),"held",f.clock.millis(),10000).orElseThrow();
        assertThat(f.store.complete(claimed,new DecisionArbitrator(f.json).finish(input,review("REDUCE"),null,f.clock.millis()),f.clock.millis())).isTrue();
        var plans=outcomes.plans(f.settings.ownerScope(),job.id());assertThat(plans).hasSize(2);
        for(var plan:plans){
            assertThat(plan.input().path("action").asText()).isEqualTo("REDUCE");assertThat(plan.input().path("quantity").asInt()).isEqualTo(12);
            assertThat(plan.input().path("evalPolicyVersion").asText()).isEqualTo(PositionOutcomeEngine.VERSION);
            outcomes.observe(f.settings.ownerScope(),plan,List.of(),ignored->f.json.tree(Map.of("status","CLOSED","observedTradingDays",0)),f.clock.millis());
        }
        assertThat(outcomes.due(f.settings.ownerScope(),f.clock.millis()+300001)).contains(job.id());
    }
    @Test void monetaryComparisonNeverSubtractsReturnsWithDifferentCapitalBases() {
        var job=service.create(f.snapshot(),new IntegratedDecisionService.Request(1,null),"amount-comparison",null);
        var claimed=f.store.claim(f.settings.ownerScope(),"amount",f.clock.millis(),10000).orElseThrow();
        f.store.complete(claimed,new DecisionArbitrator(f.json).finish(job.input(),review("WAIT"),null,f.clock.millis()),f.clock.millis());
        for(var plan:outcomes.plans(f.settings.ownerScope(),job.id())) {
            boolean ai=plan.role().equals("AI_FINAL");
            outcomes.observe(f.settings.ownerScope(),plan,List.of(),ignored->f.json.tree(Map.of("status","CLOSED","observedTradingDays",1,"startingCapital",ai?1306:1000,"netPnlAmount",ai?185:100,"netReturnPct",ai?14:10)),f.clock.millis());
        }
        var comparison=new DecisionOutcomeService(outcomes,f.store,f.settings,f.json,mock(com.example.stockanalysis.unified.eod.TradingCalendar.class),mock(com.example.stockanalysis.unified.performance.PerformanceSources.class),f.clock).comparison(job.id());
        assertThat(comparison.get("status")).isEqualTo("READY_AMOUNT");assertThat(comparison.get("netReturnDifferencePct")).isNull();
        assertThat((BigDecimal)comparison.get("forwardPnlDifferenceAmount")).isEqualByComparingTo("85");
        assertThat(outcomes.due(f.settings.ownerScope(),f.clock.millis()+300001)).doesNotContain(job.id());
    }
    @Test void queuedV3JobsKeepLegacySizingAndV4RejectsMixedSizingVersions() {
        var latest=service.create(f.snapshot(),new IntegratedDecisionService.Request(1,null),"latest-version",null);
        ObjectNode input=latest.input().deepCopy();((ObjectNode)input.path("versions")).put("policy","integrated-policy-v3-account-risk").put("sizing",PositionSizingPolicy.LEGACY_VERSION);
        ((ObjectNode)input.path("policy").path("holding")).put("positionStatus","UNKNOWN");
        var old=f.store.submit(f.settings.ownerScope(),"legacy-version","old-key","hash","old-hash","DECISION","005930",latest.snapshotId(),null,input,f.clock.millis(),"2026-10-06",100);
        var ai=mock(AiReviewPort.class);var orchestrator=new DecisionOrchestrator(f.store,f.json,f.clock);
        assertThat(orchestrator.run(old,ai,()->{throw new AssertionError("Unexpected AI reservation");}).path("decisionAction").asText()).isEqualTo("DEFER");verifyNoInteractions(ai);
        ((ObjectNode)input.path("versions")).put("policy",DecisionContextBuilder.VERSION);
        var mixed=f.store.submit(f.settings.ownerScope(),"mixed-version","mixed-key","hash","mixed-hash","DECISION","005930",latest.snapshotId(),null,input,f.clock.millis(),"2026-10-06",100);
        assertThatThrownBy(()->orchestrator.run(mixed,ai,()->{})).isInstanceOf(DecisionModels.AiFailure.class).hasMessageContaining("POLICY_VERSION_CHANGED");
    }
    @Test void staleQueuedJobsSkipAiAndPreserveTheExpiredInput() {
        var job=service.create(f.snapshot(),new IntegratedDecisionService.Request(1,null),"stale-queue",null);
        f.clock.advance(Duration.ofHours(12).toMillis());var ai=mock(AiReviewPort.class);
        try(var worker=f.worker(ai)){worker.orchestrator(new DecisionOrchestrator(f.store,f.json,f.clock));worker.tick();}
        var result=f.service.get(job.id(),"DECISION");
        assertThat(result.status()).isEqualTo("SUCCEEDED");assertThat(result.result().path("decisionAction").asText()).isEqualTo("DEFER");
        assertThat(result.result().path("blockReasons").toString()).contains("DECISION_REVIEW_REQUIRED");verifyNoInteractions(ai);
    }
    @Test void observationWorkerUsesNextSessionAndFirstStoredEvidenceIsImmutable() {
        var job=service.create(f.snapshot(),new IntegratedDecisionService.Request(1,null),"observation",null);
        var claimed=f.store.claim(f.settings.ownerScope(),"one",f.clock.millis(),10000).orElseThrow();
        f.store.complete(claimed,new DecisionArbitrator(f.json).finish(job.input(),review("WAIT"),null,f.clock.millis()),f.clock.millis());
        var calendar=mock(com.example.stockanalysis.unified.eod.TradingCalendar.class);
        when(calendar.day(any())).thenAnswer(invocation->{var date=(LocalDate)invocation.getArgument(0);return new com.example.stockanalysis.unified.eod.TradingCalendar.Day(date,com.example.stockanalysis.unified.eod.TradingCalendar.Status.OPEN,LocalTime.of(20,0),"calendar-test");});
        var prices=mock(com.example.stockanalysis.unified.performance.PerformanceSources.class);
        var date=LocalDate.of(2026,10,7);var evidence=sample(date,100,115,95,110);
        when(prices.monitoringHistory(any(),anyList(),any())).thenReturn(List.of(f.json.convert(evidence,com.example.stockanalysis.unified.performance.PerformanceModels.Observation.class)));
        var observer=new DecisionOutcomeService(outcomes,f.store,f.settings,f.json,calendar,prices,f.clock);
        f.clock.advance(Duration.ofDays(3).toMillis());observer.tick();
        var plan=outcomes.plans(f.settings.ownerScope(),job.id()).stream().filter(p->p.role().equals("AI_FINAL")).findFirst().orElseThrow();
        assertThat(plan.outcome().path("status").asText()).isEqualTo("OBSERVED");
        assertThat(plan.outcome().path("dates").get(0).asText()).isEqualTo("2026-10-07");
        assertThat(outcomes.observations(f.settings.ownerScope(),plan.id())).hasSize(1);
        outcomes.observe(f.settings.ownerScope(),plan,List.of(sample(date,100,135,95,130)),observations->{assertThat(f.json.hash(observations.get(0))).isEqualTo(f.json.hash(evidence));return plan.outcome();},f.clock.millis());
        assertThatThrownBy(()->observer.get("missing")).isInstanceOf(BusinessException.class);
        verify(prices,atLeastOnce()).monitoringHistory(any(),eq(List.of(date)),any());
    }
    @Test void completedJobAndBothPlansCommitTogetherAndFencedWorkerCannotCommit() {
        var job=service.create(f.snapshot(),new IntegratedDecisionService.Request(2,null),"pair",null);
        var stale=f.store.claim(f.settings.ownerScope(),"old",f.clock.millis(),10).orElseThrow();f.clock.advance(11);
        var current=f.store.claim(f.settings.ownerScope(),"new",f.clock.millis(),10000).orElseThrow();
        var result=new DecisionArbitrator(f.json).finish(job.input(),review("BUY"),null,f.clock.millis());
        assertThat(f.store.complete(stale,result,f.clock.millis())).isFalse();assertThat(outcomes.plans(f.settings.ownerScope(),job.id())).isEmpty();
        assertThat(f.store.complete(current,result,f.clock.millis())).isTrue();
        var plans=outcomes.plans(f.settings.ownerScope(),job.id());assertThat(plans).hasSize(2);
        assertThat(plans).extracting(DecisionOutcomeStore.Plan::role).containsExactlyInAnyOrder("ENGINE","AI_FINAL");
        assertThat(plans.get(0).opportunity()).isEqualTo(plans.get(1).opportunity());
        assertThat(result.path("decisionAction").asText()).isEqualTo("WAIT");
        assertThat(result.path("blockReasons").toString()).contains("RESEARCH_MISSING");
        assertThat(result.path("quantity").isNull()).isTrue();
        assertThat(outcomes.plans("other",job.id())).isEmpty();
        assertThat(f.service.get(job.id(),"DECISION").mode()).isEqualTo("INTEGRATED_DECISION");
        assertThat(f.store.history(f.settings.ownerScope(),"005930",0,20).items().get(0).contractVersion()).isEqualTo(IntegratedDecisionContract.VERSION);
    }
    @Test void planFailureRollsBackJobCompletion() {
        var job=service.create(f.snapshot(),null,"rollback",null);var claimed=f.store.claim(f.settings.ownerScope(),"one",f.clock.millis(),10000).orElseThrow();
        new JdbcTemplate(f.ds).execute("DROP TABLE td_decision_observation");
        new JdbcTemplate(f.ds).execute("DROP TABLE td_decision_outcome");
        assertThatThrownBy(()->f.store.complete(claimed,new DecisionArbitrator(f.json).finish(job.input(),review("WAIT"),null,f.clock.millis()),f.clock.millis())).isInstanceOf(RuntimeException.class);
        assertThat(f.store.find(f.settings.ownerScope(),job.id()).orElseThrow().status()).isEqualTo("RUNNING");
        assertThat(new JdbcTemplate(f.ds).queryForObject("SELECT COUNT(*) FROM td_decision_plan",Integer.class)).isZero();
    }
    @Test void researchMustMatchOwnerSnapshotAndFreshness() {
        String s=f.snapshot();String other=f.snapshot();
        var r=research(s,"test-owner",f.clock.instant(),"VALID");
        assertThat(service.create(s,new IntegratedDecisionService.Request(3,r.id()),"research",null).input().path("policy").path("research").path("jobId").asText()).isEqualTo(r.id());
        assertThatThrownBy(()->service.create(other,new IntegratedDecisionService.Request(3,r.id()),"wrong-snapshot",null)).isInstanceOf(BusinessException.class);
        f.clock.advance(Duration.ofDays(8).toMillis());
        assertThatThrownBy(()->service.create(s,new IntegratedDecisionService.Request(3,r.id()),"stale",null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.create(s,new IntegratedDecisionService.Request(3,"missing"),"missing",null)).isInstanceOf(BusinessException.class);
    }
    @Test void terminalAiFailurePreservesExitAndUnknownBalancesSkipAi() {
        var ai=mock(AiReviewPort.class);when(ai.assess(any())).thenThrow(new DecisionModels.AiFailure("AI_INVALID_RESPONSE",false));
        var p=basePolicy("HELD","SELL");((ObjectNode)p.path("holding")).put("action","EXIT_TRIGGERED");
        var input=f.json.tree(Map.of("policy",p,"versions",Map.of("contract",IntegratedDecisionContract.VERSION,"policy",DecisionContextBuilder.VERSION)));
        var job=new DecisionModels.Job("fake","owner","DECISION","005930","s",null,"RUNNING",input,null,null,null,0L,null,1,"one",999L,null,false);
        var orchestrator=new DecisionOrchestrator(f.store,f.json,f.clock);
        var result=orchestrator.run(job,ai,()->{});
        assertThat(result.path("decisionAction").asText()).isEqualTo("SELL");assertThat(result.path("aiErrorCode").asText()).isEqualTo("AI_INVALID_RESPONSE");
        ((ObjectNode)p.path("holding")).put("positionStatus","UNKNOWN");((ObjectNode)input).set("policy",p);
        clearInvocations(ai);orchestrator.run(job,ai,()->{});verifyNoInteractions(ai);
    }
    @Test void quotesMayContainNumbersButUnsupportedQuotesAndInventedReferencesFail() {
        var p=basePolicy("NOT_HELD","WAIT");
        ((ObjectNode)p.path("research")).set("sources",f.json.tree(List.of(Map.of("id","S1","title","계약금액 100억원 공시"))));
        var valid=(ObjectNode)review("WAIT");
        ((ObjectNode)valid.path("supportingReasons").get(0)).set("sourceIds",f.json.tree(List.of("S1")));
        ((ObjectNode)valid.path("supportingReasons").get(0)).put("quote","계약금액 100억원");
        var contract=new IntegratedDecisionContract(f.json);contract.validate(valid,p);
        ((ObjectNode)valid.path("supportingReasons").get(0)).put("quote","매출 200억원");
        assertThatThrownBy(()->contract.validate(valid,p)).isInstanceOf(DecisionModels.AiFailure.class);
        ((ObjectNode)valid.path("supportingReasons").get(0)).put("quote","계약금액 100억원");
        ((ObjectNode)valid.path("supportingReasons").get(0)).set("evidenceIds",f.json.tree(List.of("FAKE")));
        assertThatThrownBy(()->contract.validate(valid,p)).isInstanceOf(DecisionModels.AiFailure.class);
    }
    @Test void arbitratorBlocksPartialActionsAndExpiredInputsWithoutSuppressingRuleFacts() {
        var p=basePolicy("HELD","ADD");var a=new DecisionArbitrator(f.json);
        assertThat(a.finish(f.json.tree(Map.of("policy",p)),review("ADD"),null,f.clock.millis()).path("decisionAction").asText()).isEqualTo("HOLD");
        assertThat(a.finish(f.json.tree(Map.of("policy",p)),review("HOLD"),null,f.clock.millis()+Duration.ofDays(8).toMillis()).path("decisionAction").asText()).isEqualTo("DEFER");
        p.put("personalStopBreached",true);
        assertThat(a.finish(f.json.tree(Map.of("policy",p)),review("HOLD"),null,f.clock.millis()).path("decisionAction").asText()).isEqualTo("DEFER");
    }
    @Test void revisionCreatesNewFrozenContextWithoutChangingParent() {
        var first=service.create(f.snapshot(),new IntegratedDecisionService.Request(3,null),"first",null);
        var claimed=f.store.claim(f.settings.ownerScope(),"one",f.clock.millis(),10000).orElseThrow();
        f.store.complete(claimed,new DecisionArbitrator(f.json).finish(first.input(),review("WAIT"),null,f.clock.millis()),f.clock.millis());
        when(holdings.single("005930")).thenReturn(balance(3));
        var revised=f.service.revision(first.id(),"revision");
        assertThat(revised.parentId()).isEqualTo(first.id());assertThat(revised.mode()).isEqualTo("INTEGRATED_DECISION");
        assertThat(outcomes.plans(f.settings.ownerScope(),first.id())).hasSize(2);
    }
    @Test void variableHorizonMultipleTargetsAndAmbiguousPathsAreEvaluatedAfterRegistration() {
        var p=plan("BUY",2);var engine=new DecisionOutcomeEngine(f.json);var dates=List.of(LocalDate.of(2026,10,7),LocalDate.of(2026,10,8));
        var observation=sample(dates.get(0),100,112,96,108);
        var result=engine.evaluate(p,dates,List.of(observation,sample(dates.get(1),108,125,101,120)),Instant.parse("2026-10-09T12:00:00Z"));
        assertThat(result.path("status").asText()).isEqualTo("CLOSED");assertThat(result.path("fills")).hasSize(2);
        assertThat(result.path("exitReason").asText()).isEqualTo("TARGET");
        var ambiguous=engine.evaluate(p,dates,List.of(sample(dates.get(0),100,125,80,100)),Instant.parse("2026-10-09T12:00:00Z"));
        assertThat(ambiguous.path("ambiguous").asBoolean()).isTrue();assertThat(ambiguous.path("exitReason").asText()).isEqualTo("STOP");
        var gap=engine.evaluate(p,dates,List.of(sample(dates.get(0),100,105,95,101),sample(dates.get(1),80,85,75,82)),Instant.parse("2026-10-09T12:00:00Z"));
        assertThat(gap.path("exitReason").asText()).isEqualTo("GAP_STOP");assertThat(gap.path("netReturnPct").decimalValue()).isNegative();
    }
    @Test void futureMissingPricesAndOutOfRangeEntryNeverProduceInventedReturns() {
        var dates=List.of(LocalDate.of(2026,10,7));var engine=new DecisionOutcomeEngine(f.json);
        var future=engine.evaluate(plan("BUY",1),dates,List.of(sample(dates.get(0),100,120,95,115)),Instant.parse("2026-10-06T12:00:00Z"));
        assertThat(future.path("netReturnPct").isNull()).isTrue();
        var missing=engine.evaluate(plan("BUY",1),dates,List.of(),Instant.parse("2026-10-09T12:00:00Z"));assertThat(missing.path("status").asText()).isEqualTo("DATA_GAP");
        var noEntry=engine.evaluate(plan("BUY",1),dates,List.of(sample(dates.get(0),130,140,125,135)),Instant.parse("2026-10-09T12:00:00Z"));assertThat(noEntry.path("status").asText()).isEqualTo("NOT_ENTERED");
    }
    @Test void sellAndWaitCompareCashToForwardHoldingWithoutClaimingHistoricalPnl() {
        var date=LocalDate.of(2026,10,7);var result=new DecisionOutcomeEngine(f.json).evaluate(plan("SELL",1),List.of(date),List.of(sample(date,100,110,70,80)),Instant.parse("2026-10-09T12:00:00Z"));
        assertThat(result.path("status").asText()).isEqualTo("OBSERVED");assertThat(result.path("netReturnPct").decimalValue()).isNegative();
        assertThat(result.path("differencePct").decimalValue()).isPositive();assertThat(result.path("basis").asText()).isEqualTo("PER_UNIT_FORWARD_SIMULATION");
    }
    HoldingModels.Snapshot balance(long revision){return new HoldingModels.Snapshot(f.clock.instant(),"AVAILABLE",Map.of("005930",new HoldingModels.Holding("005930","삼성전자",BigDecimal.TEN,BigDecimal.valueOf(900),null,LocalDate.of(2026,10,5),null,null,revision)));}
    DecisionModels.Job research(String snapshot,String owner,Instant collected,String status) {
        var original=f.store.find(f.settings.ownerScope(),snapshot).orElseThrow();
        var job=f.store.submit(owner,"test-research",UUID.randomUUID().toString(),"hash",UUID.randomUUID().toString(),"DECISION","005930",snapshot,null,f.json.tree(Map.of("mode","NEWS_INSIGHTS","snapshotHash",original.resultHash())),f.clock.millis(),"2026-10-06",100);
        var claimed=f.store.claim(owner,"news",f.clock.millis(),10000).orElseThrow();
        var result=f.json.tree(Map.of("generationMode","NEWS_INSIGHTS","evidence",Map.of("collectedAt",collected.toString(),"sources",List.of(Map.of("id","S1","title","공식 계약 공시","depth","OFFICIAL_DOCUMENT","excerpt","계약을 체결했습니다."))),"report",Map.of("signalInterpretation",Map.of("stance","CHECK_REQUIRED"))));
        f.store.complete(claimed,result,f.clock.millis());return f.store.find(owner,job.id()).orElseThrow();
    }
    ObjectNode basePolicy(String position,String action){return (ObjectNode)f.json.tree(Map.of("priceDate","2026-10-05","analysisTiming","VALID","engineAction",action,"entryEligible",true,"collectionQuality","COMPLETE","holding",Map.of("positionStatus",position,"action",action),"research",Map.of("status","MISSING","sources",List.of()),"evidence",Map.of("RULES",Map.of()),"plan",Map.of()));}
    JsonNode review(String action){var reason=Map.of("text","근거를 확인합니다.","evidenceIds",List.of("RULES"),"sourceIds",List.of(),"quote",f.json.tree(null));return f.json.tree(Map.of("action",action,"conclusion","추세 조건을 확인합니다.","supportingReasons",List.of(reason),"opposingReasons",List.of(reason),"reviewConditions",List.of("추세 이탈 시 재검토합니다."),"missingInformation",List.of()));}
    JsonNode plan(String action,int days){return f.json.tree(Map.of("action",action,"horizonTradingDays",days,"registeredAt","2026-10-06T12:00:00Z","roundTripFeeBps",30,"slippageBps",10,"plan",Map.of("entryFrom",95,"entryTo",105,"stopLoss",90,"targets",List.of(110,120))));}
    JsonNode sample(LocalDate date,int open,int high,int low,int close){return f.json.tree(new com.example.stockanalysis.unified.performance.PerformanceModels.Observation("STOCK","005930",date,"VALID",new DailyCandle(date,BigDecimal.valueOf(open),BigDecimal.valueOf(high),BigDecimal.valueOf(low),BigDecimal.valueOf(close),1000),"source",date.atTime(20,0).atZone(DecisionService.SEOUL).toInstant(),"EOD_SNAPSHOT"));}
}
