package com.example.stockanalysis.decision;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Explicit opt-in only: two paid passes using synthetic analysis, never a real portfolio. */
@EnabledIfEnvironmentVariable(named="GEMINI_SMOKE_TEST",matches="true")
class GeminiLiveSmokeTest {
    @Test void realAdapterAcceptsContractAndValidatesStructuredReview() {
        String key=System.getenv("GEMINI_API_KEY");assertThat(key!=null && !key.isBlank()).isTrue();
        var json=new DecisionJson(new ObjectMapper());var contract=new AiReviewContract(json) {
            @Override public DecisionModels.Review parse(com.fasterxml.jackson.databind.JsonNode node) {
                // Opt-in test uses synthetic inputs only; keep a reviewable sample without request headers/keys.
                try {java.nio.file.Files.writeString(java.nio.file.Path.of("target","gemini-personalized-sample.json"),json.write(node));}
                catch(java.io.IOException e) {throw new java.io.UncheckedIOException(e);}
                return super.parse(node);
            }
        };
        var settings=new DecisionSettings(true,"smoke","gemini-3.8-flash",key,100,30,300000,128000,12000);
        var fixture=new DecisionWorkflowTest();fixture.setup();
        var policy=fixture.policy.prepare(fixture.data,new DecisionModels.ReviewRequest(DecisionModels.Position.NOT_HELD,null,null,null,DecisionModels.Preferences.defaults()));
        assertThat(policy.path("blockReason").isNull()).isTrue();
        var input=json.tree(Map.of("versions",Map.of("provider",GeminiReviewAdapter.PROVIDER,"model",settings.model(),"maxOutputTokens",12000,"thinkingLevel",settings.thinkingLevel()),
                "prompt",AiReviewContract.PROMPT,"schema",contract.schema(),"policy",policy,"context",policy.path("context")));
        var adapter=new GeminiReviewAdapter(settings,json,contract);var suitability=new SuitabilityContract(json);
        com.fasterxml.jackson.databind.node.ObjectNode marketInput=input.deepCopy();marketInput.set("policy",SuitabilityContract.marketInput(policy));marketInput.put("prompt",SuitabilityContract.MARKET_PROMPT);marketInput.set("schema",suitability.marketSchema());
        var market=adapter.assess(marketInput);suitability.validateMarket(market.assessment(),marketInput.path("policy"));
        try {java.nio.file.Files.writeString(java.nio.file.Path.of("target","gemini-market-sample.json"),json.write(market.assessment()));}
        catch(java.io.IOException e) {throw new java.io.UncheckedIOException(e);}
        ((com.fasterxml.jackson.databind.node.ObjectNode)input.path("policy")).set("marketAssessment",market.assessment());
        var response=adapter.review(input);
        contract.validate(response.review(),input);
        assertThat(response.review().detail()).isNotNull();assertThat(response.inputTokens()).isPositive();
    }
}
