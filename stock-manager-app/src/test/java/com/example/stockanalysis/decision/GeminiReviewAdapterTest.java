package com.example.stockanalysis.decision;

import static com.example.stockanalysis.decision.DecisionModels.*;
import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;

class GeminiReviewAdapterTest {
    HttpServer server; DecisionJson json=new DecisionJson(new ObjectMapper()); AiReviewContract contract=new AiReviewContract(json);
    AtomicReference<String> requestBody=new AtomicReference<>(),path=new AtomicReference<>(),key=new AtomicReference<>(),authorization=new AtomicReference<>();
    int status=200; String response; GeminiReviewAdapter adapter;
    @BeforeEach void setup() throws Exception {
        server=HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(),0),0);
        server.createContext("/models/",exchange->{
            path.set(exchange.getRequestURI().toString());key.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            exchange.getResponseHeaders().add("Retry-After","30");byte[] bytes=response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();
        adapter=new GeminiReviewAdapter(new DecisionSettings(true,"local","gemini-changed-config","test-key",100,30,300000,24000,3000),json,contract,HttpClient.newHttpClient(),
                URI.create("http://localhost:"+server.getAddress().getPort()+"/models/"));
    }
    @AfterEach void stop(){server.stop(0);}
    ObjectNode input() { return (ObjectNode)json.tree(Map.of("versions",Map.of("provider",GeminiReviewAdapter.PROVIDER,"model","gemini-frozen-model","maxOutputTokens",3000),"prompt",AiReviewContract.PROMPT,"schema",contract.schema(),"policy",Map.of("allowedActions",List.of("WAIT")))); }
    Review review(){return new Review("WAIT",null,List.of(new Reason("SIGNALS","확인이 필요합니다.")),List.of(new Reason("TREND","추세가 바뀔 수 있습니다.")),
            List.of(new Scenario("UPSIDE","재검토합니다."),new Scenario("RANGE","대기합니다."),new Scenario("DOWNSIDE","진입을 보류합니다.")),List.of("추가 근거가 필요합니다."),List.of("UPSIDE"),ReviewTestFixtures.detail(),ReviewTestFixtures.independent(),ReviewTestFixtures.personal());}
    String success(List<?> parts) { return json.write(Map.of("responseId","gemini-response","candidates",List.of(Map.of("finishReason","STOP","content",Map.of("parts",parts))),
            "usageMetadata",Map.of("promptTokenCount",50,"candidatesTokenCount",70,"thoughtsTokenCount",20))); }
    @Test void sendsFrozenModelAndSchemaWithHeaderOnlyKeyAndParsesThinkingUsage() {
        response=success(List.of(Map.of("text",json.write(review()))));
        var result=adapter.review(input()); var request=json.read(requestBody.get());
        assertThat(path.get()).isEqualTo("/models/gemini-frozen-model:generateContent");
        assertThat(key.get()).isEqualTo("test-key");assertThat(authorization.get()).isNull();
        assertThat(requestBody.get()).doesNotContain("test-key");assertThat(request.has("tools")).isFalse();
        assertThat(request.path("generationConfig").path("responseJsonSchema")).isEqualTo(GeminiReviewAdapter.generationSchema(contract.schema()));
        assertThat(request.path("generationConfig").path("responseJsonSchema").toString()).doesNotContain("minItems","maxItems");
        assertThat(contract.schema().path("properties").path("scenarios").path("minItems").asInt()).isEqualTo(3);
        assertThat(request.path("generationConfig").path("maxOutputTokens").asInt()).isEqualTo(3000);
        assertThat(result.review().recommendation()).isEqualTo("WAIT");assertThat(result.providerId()).isEqualTo("gemini-response");
        assertThat(result.inputTokens()).isEqualTo(50);assertThat(result.outputTokens()).isEqualTo(90);
    }
    @Test void ignoresThoughtPartsAndJoinsTextParts() {
        String text=json.write(review());int split=text.length()/2;
        response=success(List.of(Map.of("text","private reasoning","thought",true),Map.of("text",text.substring(0,split)),Map.of("text",text.substring(split))));
        assertThat(adapter.review(input()).review().recommendation()).isEqualTo("WAIT");
    }
    @Test void rateLimitsHonorRetryAfterWithoutLeakingErrorBody() {
        status=429;response="private external error text";
        assertThatThrownBy(()->adapter.review(input())).isInstanceOfSatisfying(AiFailure.class,e->{
            assertThat(e.retryable()).isTrue();assertThat(e.delayMillis()).isEqualTo(30000);assertThat(e.getMessage()).isEqualTo("AI_HTTP_429");});
    }
    @Test void authenticationFailureIsTerminal() {
        status=403;response="private key details";
        assertThatThrownBy(()->adapter.review(input())).isInstanceOfSatisfying(AiFailure.class,e->{assertThat(e.retryable()).isFalse();assertThat(e.getMessage()).isEqualTo("AI_HTTP_403");});
    }
    @Test void refusesIncompleteAndBlockedResponses() {
        response="{\"candidates\":[{\"finishReason\":\"MAX_TOKENS\"}]}";
        assertThatThrownBy(()->adapter.review(input())).hasMessage("AI_INCOMPLETE");
        response="{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}";
        assertThatThrownBy(()->adapter.review(input())).hasMessage("AI_REFUSAL");
        response="{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}";
        assertThatThrownBy(()->adapter.review(input())).hasMessage("AI_REFUSAL");
    }
    @Test void invalidJsonAndEmptyCandidatesAreTerminalRatherThanTradingAdvice() {
        for(String body:List.of("not-json","{\"candidates\":[]}",success(List.of(Map.of("text","not-json"))))) {
            response=body;assertThatThrownBy(()->adapter.review(input())).isInstanceOfSatisfying(AiFailure.class,e->assertThat(e.retryable()).isFalse());
        }
    }
    @Test void refusesLegacyProviderAndInvalidModelBeforeSendingAnything() {
        var legacy=input();((ObjectNode)legacy.path("versions")).put("provider","openai-responses");
        assertThatThrownBy(()->adapter.review(legacy)).hasMessage("AI_PROVIDER_CHANGED");
        var invalid=input();((ObjectNode)invalid.path("versions")).put("model","gemini-x/../../other");
        assertThatThrownBy(()->adapter.review(invalid)).hasMessage("AI_MODEL_INVALID");assertThat(requestBody.get()).isNull();
    }
    @Test void usesFrozenThinkingLevelAndLeavesOlderInputsUnchanged() {
        var input=input();((ObjectNode)input.path("versions")).put("thinkingLevel","MEDIUM");
        assertThat(json.tree(GeminiReviewAdapter.request(input)).path("generationConfig").path("thinkingConfig").path("thinkingLevel").asText()).isEqualTo("MEDIUM");
        assertThat(json.tree(GeminiReviewAdapter.request(input())).path("generationConfig").has("thinkingConfig")).isFalse();
    }
}
