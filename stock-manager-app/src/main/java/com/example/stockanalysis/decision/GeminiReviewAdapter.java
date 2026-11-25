package com.example.stockanalysis.decision;

import static com.example.stockanalysis.decision.DecisionModels.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GeminiReviewAdapter implements AiReviewPort {
    public static final String PROVIDER="gemini-generate-content-v1";
    private final DecisionSettings settings; private final DecisionJson json; private final AiReviewContract contract;
    private final HttpClient client; private final URI endpoint;
    @Autowired public GeminiReviewAdapter(DecisionSettings settings,DecisionJson json,AiReviewContract contract) {
        this(settings,json,contract,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),URI.create("https://generativelanguage.googleapis.com/v1beta/models/"));
    }
    GeminiReviewAdapter(DecisionSettings settings,DecisionJson json,AiReviewContract contract,HttpClient client,URI endpoint) {
        this.settings=settings;this.json=json;this.contract=contract;this.client=client;this.endpoint=endpoint;
    }
    public static Map<String,Object> request(JsonNode input) {
        var policy=input.path("policy").deepCopy();
        if(policy instanceof com.fasterxml.jackson.databind.node.ObjectNode object) object.remove(java.util.List.of("comparisons","accountRisk"));
        Map<String,Object> generation=new LinkedHashMap<>();generation.put("candidateCount",1);generation.put("maxOutputTokens",input.path("versions").path("maxOutputTokens").asInt());
        generation.put("responseMimeType","application/json");generation.put("responseJsonSchema",generationSchema(input.path("schema")));
        String thinking=input.path("versions").path("thinkingLevel").asText("");
        if(!thinking.isBlank()) generation.put("thinkingConfig",Map.of("thinkingLevel",thinking));
        return Map.of("systemInstruction",Map.of("parts",List.of(Map.of("text",input.path("prompt").asText()))),
                "contents",List.of(Map.of("role","user","parts",List.of(Map.of("text",policy.toString())))),
                "generationConfig",generation);
    }
    // Array cardinality expansion makes the independent report schema too complex for Gemini.
    // Retain the full frozen contract; enforce its counts in AiReviewContract after generation.
    static JsonNode generationSchema(JsonNode schema) {
        JsonNode copy=schema.deepCopy();removeArrayBounds(copy);return copy;
    }
    private static void removeArrayBounds(JsonNode node) {
        if(node instanceof com.fasterxml.jackson.databind.node.ObjectNode object) {object.remove("minItems");object.remove("maxItems");}
        if(node.isContainerNode()) node.forEach(GeminiReviewAdapter::removeArrayBounds);
    }
    @Override public AiResponse review(JsonNode input) {
        var generated=generate(input);return new AiResponse(contract.parse(generated.output()),generated.providerId(),generated.raw(),generated.inputTokens(),generated.outputTokens());
    }
    @Override public MarketResponse assess(JsonNode input) {
        var generated=generate(input);return new MarketResponse(generated.output(),generated.providerId(),generated.raw(),generated.inputTokens(),generated.outputTokens());
    }
    private record Generated(JsonNode output,String providerId,JsonNode raw,long inputTokens,long outputTokens) {}
    private Generated generate(JsonNode input) {
        if(settings.apiKey().isBlank() || input.path("versions").path("model").asText().isBlank()) throw new AiFailure("AI_NOT_CONFIGURED",false);
        if(!PROVIDER.equals(input.path("versions").path("provider").asText())) throw new AiFailure("AI_PROVIDER_CHANGED",false);
        String model=input.path("versions").path("model").asText();
        if(!model.matches("gemini-[A-Za-z0-9._-]+")) throw new AiFailure("AI_MODEL_INVALID",false);
        var request=HttpRequest.newBuilder(URI.create(endpoint.toString()+model+":generateContent")).timeout(Duration.ofSeconds(120)).header("x-goog-api-key",settings.apiKey())
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.write(request(input)),StandardCharsets.UTF_8)).build();
        try {
            // Request output is bounded; do not expose provider error bodies or credentials.
            var response=client.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status=response.statusCode();
            if(status!=200) throw new AiFailure("AI_HTTP_"+status,status==429 || status>=500,retryAfter(response.headers().firstValue("Retry-After").orElse(null)));
            if(response.body().length()>1_000_000) throw new AiFailure("AI_RESPONSE_TOO_LARGE",false);
            JsonNode body=json.read(response.body());
            if(!body.path("promptFeedback").path("blockReason").asText().isBlank()) throw new AiFailure("AI_REFUSAL",false);
            JsonNode candidates=body.path("candidates");
            if(!candidates.isArray() || candidates.size()!=1) throw new AiFailure("AI_INVALID_RESPONSE",false);
            JsonNode candidate=candidates.get(0);String finish=candidate.path("finishReason").asText();
            if(Set.of("SAFETY","RECITATION","BLOCKLIST","PROHIBITED_CONTENT","SPII","IMAGE_SAFETY").contains(finish)) throw new AiFailure("AI_REFUSAL",false);
            if(!"STOP".equals(finish)) throw new AiFailure("AI_INCOMPLETE",false);
            StringBuilder output=new StringBuilder();
            for(JsonNode part:candidate.path("content").path("parts")) {
                if(part.path("thought").asBoolean()) continue;
                if(!part.path("text").isTextual()) throw new AiFailure("AI_INVALID_RESPONSE",false);
                output.append(part.path("text").asText());
            }
            if(output.isEmpty()) throw new AiFailure("AI_INVALID_RESPONSE",false);
            JsonNode usage=body.path("usageMetadata");
            return new Generated(json.read(output.toString()),body.path("responseId").asText(null),body,usage.path("promptTokenCount").asLong(),
                    usage.path("candidatesTokenCount").asLong()+usage.path("thoughtsTokenCount").asLong());
        } catch(AiFailure e) { throw e; }
        catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new AiFailure("AI_INTERRUPTED",true); }
        catch(java.io.IOException e) { throw new AiFailure("AI_CONNECTION_ERROR",true); }
        catch(RuntimeException e) { throw new AiFailure("AI_INVALID_RESPONSE",false); }
    }
    private long retryAfter(String value) {
        if(value==null) return 0;
        try { return Math.max(0,Math.min(86_400_000,Long.parseLong(value)*1000)); }
        catch(NumberFormatException e) {
            try { return Math.max(0,Math.min(86_400_000,Duration.between(Instant.now(),ZonedDateTime.parse(value,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).toMillis())); }
            catch(RuntimeException ignored) { return 0; }
        }
    }
}
