package com.example.stockanalysis.decision;

import static com.example.stockanalysis.decision.DecisionModels.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;

class SuitabilityTest {
    DecisionWorkflowTest f;SuitabilityContract contract;
    @BeforeEach void setup() {f=new DecisionWorkflowTest();f.setup();contract=new SuitabilityContract(f.json);}
    Preferences aggressive() {return new Preferences(70,20,10,"OCCASIONAL","EARLY","HIGH","QUICK");}
    ObjectNode input(ReviewRequest request) {return (ObjectNode)f.json.tree(Map.of("context",request,"policy",f.policy.prepare(f.data,request)));}
    @Test void omittedHorizonMeansAiProposalAndLegacyRequestReplayRemainsIntact() {
        var request=f.json.convert(f.json.read("{\"positionStatus\":\"NOT_HELD\"}"),ReviewRequest.class);
        assertThat(request.horizonTradingDays()).isNull();String snapshot=f.snapshot();
        var first=f.service.decision(snapshot,request,"auto");assertThat(f.service.decision(snapshot,request,"another-auto").id()).isEqualTo(first.id());
        assertThat(f.service.decision(snapshot,new ReviewRequest(Position.NOT_HELD,null,null,null,Preferences.defaults()),"explicit-defaults").id()).isEqualTo(first.id());
        var policy=f.store.find(f.settings.ownerScope(),first.id()).orElseThrow().input().path("policy");
        assertThat(policy.path("horizonMode").asText()).isEqualTo("AI_PROPOSED");assertThat(policy.path("metrics").has("HORIZON")).isFalse();
        assertThat(policy.path("horizonPlan").path("referenceOnly").asBoolean()).isTrue();
        var legacy=f.decide(snapshot,"old-key");assertThat(f.decide(snapshot,"old-key").id()).isEqualTo(legacy.id());
        assertThat(legacy.id()).isNotEqualTo(first.id());
    }
    @Test void preferenceChangesAffectIdentityButNeverMarketPayload() {
        String snapshot=f.snapshot();var a=new ReviewRequest(Position.NOT_HELD,null,null,null,Preferences.defaults());var b=new ReviewRequest(Position.NOT_HELD,null,null,null,aggressive());
        var first=f.service.decision(snapshot,a,"first");var second=f.service.decision(snapshot,b,"second");assertThat(first.id()).isNotEqualTo(second.id());
        assertThatThrownBy(()->f.service.decision(snapshot,b,"first")).isInstanceOf(com.example.stockanalysis.shared.exception.BusinessException.class);
        var one=f.store.find(f.settings.ownerScope(),first.id()).orElseThrow().input().path("marketPolicy");
        var held=input(new ReviewRequest(Position.HELD,60,BigDecimal.valueOf(700),f.data.result().priceDate(),aggressive()));
        assertThat(f.json.hash(SuitabilityContract.marketInput(held.path("policy")))).isEqualTo(f.json.hash(one));
        assertThat(one.toString()).doesNotContain("preferences","aggressive","averageBuyPrice","purchasedOn","horizonTradingDays","positionStatus","engineAssessment");
    }
    @Test void invalidWeightsAndPreferencesAreRejectedBeforeCreatingJobs() {
        String snapshot=f.snapshot();
        for(var preferences:List.of(new Preferences(70,30,10,"DAILY","EARLY","HIGH","QUICK"),new Preferences(-1,61,40,"DAILY","EARLY","HIGH","QUICK"),new Preferences(20,60,20,"NEVER","EARLY","HIGH","QUICK")))
            assertThatThrownBy(()->f.service.decision(snapshot,new ReviewRequest(Position.NOT_HELD,null,null,null,preferences),UUID.randomUUID().toString())).isInstanceOf(com.example.stockanalysis.shared.exception.BusinessException.class);
        assertThat(f.service.history("005930",0,20).items()).isEmpty();
    }
    @Test void engineIncompleteDoesNotPreventMarketAssessmentButLongTermCannotBeInvented() {
        var market=SuitabilityContract.marketInput(f.policy.prepare(f.data,new ReviewRequest(Position.NOT_HELD,10)));
        var assessment=ReviewTestFixtures.market();contract.validateMarket(assessment,market);
        var invalid=assessment.deepCopy();((ObjectNode)invalid.path("styles").get(3)).put("status","SUITABLE");
        assertThatThrownBy(()->contract.validateMarket(invalid,market)).hasMessage("AI_INVALID_RESPONSE");
        var missing=assessment.deepCopy();((ObjectNode)missing.path("styles").get(0)).set("evidenceIds",f.json.tree(List.of("INVENTED_NEWS")));
        assertThatThrownBy(()->contract.validateMarket(missing,market)).isInstanceOf(AiFailure.class);
    }
    @Test void infrequentMonitoringCannotBeCalledSuitableOrProduceBuy() {
        var input=input(new ReviewRequest(Position.NOT_HELD,10,null,null,aggressive()));var r=(ObjectNode)f.json.tree(IndependentReviewTest.independentBuy(input,f.json));
        assertThatThrownBy(()->f.contract.validate(f.contract.parse(r),input)).hasMessage("AI_INVALID_RESPONSE");
        ((ObjectNode)r.path("personalization")).put("fit","MISMATCH");assertThatThrownBy(()->f.contract.validate(f.contract.parse(r),input)).isInstanceOf(AiFailure.class);
        r.put("recommendation","WAIT");r.putNull("planId");((ObjectNode)r.path("personalization")).set("transitions",ReviewTestFixtures.personal().path("transitions"));f.contract.validate(f.contract.parse(r),input);
    }
    @Test void suggestedRangeMustFitSelectedMethodAndPrimaryStrategy() {
        var input=input(new ReviewRequest(Position.NOT_HELD,null,null,null));var r=(ObjectNode)f.json.tree(IndependentReviewTest.independentBuy(input,f.json));
        var period=(ObjectNode)r.path("personalization").path("holdingPeriod");period.put("minTradingDays",6).put("maxTradingDays",15);f.contract.validate(f.contract.parse(r),input);
        period.put("maxTradingDays",8);assertThatThrownBy(()->f.contract.validate(f.contract.parse(r),input)).isInstanceOf(AiFailure.class);
        period.put("minTradingDays",20).put("maxTradingDays",6);assertThatThrownBy(()->f.contract.validate(f.contract.parse(r),input)).isInstanceOf(AiFailure.class);
    }
    @Test void priceTransitionsRequireExactSavedReferencesAndNextActionCannotContradictWait() {
        var input=input(new ReviewRequest(Position.NOT_HELD,10));var r=(ObjectNode)f.json.tree(f.validReview(input));
        var transition=(ObjectNode)r.path("personalization").path("transitions").get(0);transition.put("assessmentMode","PRICE_CHECK");
        var price=input.path("policy").path("metrics").path("CURRENT").path("value");transition.set("priceCondition",f.json.tree(Map.of("sourcePath","/metrics/CURRENT/value","operator","ABOVE","price",price)));
        f.contract.validate(f.contract.parse(r),input);
        ((ObjectNode)transition.path("priceCondition")).put("price",1);assertThatThrownBy(()->f.contract.validate(f.contract.parse(r),input)).isInstanceOf(AiFailure.class);
        transition.put("assessmentMode","AI_REVIEW");transition.putNull("priceCondition");
        ((ObjectNode)r.path("personalization").path("nextAction")).put("action","CONSIDER_ENTRY");assertThatThrownBy(()->f.contract.validate(f.contract.parse(r),input)).isInstanceOf(AiFailure.class);
    }
    @Test void retryReusesMarketAssessmentAndChargesEachProviderCallOnce() {
        String id=f.decide(f.snapshot(),"two-stage").id();var ai=mock(AiReviewPort.class);
        when(ai.review(any())).thenThrow(new AiFailure("AI_HTTP_429",true,1000)).thenAnswer(inv->{var review=f.validReview(inv.getArgument(0));return new AiResponse(review,"personal",f.json.tree(Map.of()),30,40);});
        var worker=f.worker(ai);try {worker.tick();assertThat(f.service.get(id,"DECISION").status()).isEqualTo("RETRYABLE_FAILED");
            f.clock.advance(20000);worker.tick();} finally {worker.close();}
        assertThat(f.service.get(id,"DECISION").status()).isEqualTo("SUCCEEDED");verify(ai,times(1)).assess(any());verify(ai,times(2)).review(any());
        assertThat(f.service.get(id,"DECISION").result().path("marketAssessment")).isEqualTo(ReviewTestFixtures.market());
        assertThat(new JdbcTemplate(f.ds).queryForObject("SELECT SUM(call_count) FROM td_budget",Integer.class)).isEqualTo(3);
        assertThat(new JdbcTemplate(f.ds).queryForObject("SELECT SUM(input_tokens) FROM td_attempt WHERE job_id=?",Integer.class,id)).isEqualTo(40);
        assertThat(new JdbcTemplate(f.ds).queryForObject("SELECT SUM(output_tokens) FROM td_attempt WHERE job_id=?",Integer.class,id)).isEqualTo(60);
    }
    @Test void staleWorkerCannotEstablishMarketCache() {
        String id=f.decide(f.snapshot(),"fence-market").id();var job=f.store.claim(f.settings.ownerScope(),"old",f.clock.millis(),100).orElseThrow();
        f.clock.advance(101);f.store.claim(f.settings.ownerScope(),"new",f.clock.millis(),1000).orElseThrow();
        assertThat(f.store.recordMarket(job,new MarketResponse(ReviewTestFixtures.market(),"market",f.json.tree(Map.of()),1,2),f.clock.millis())).isFalse();
        assertThat(f.store.marketAssessment(f.store.find(f.settings.ownerScope(),id).orElseThrow())).isEmpty();
    }
}
