package com.example.stockanalysis.decision;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record DecisionSettings(
        @Value("${trade-decision.enabled:false}") boolean enabled,
        @Value("${trade-decision.owner-scope:local-owner}") String ownerScope,
        @Value("${trade-decision.model:}") String model,
        @Value("${app.gemini.api-key:}") String apiKey,
        @Value("${trade-decision.daily-requests:100}") int dailyRequests,
        @Value("${trade-decision.daily-ai-calls:30}") int dailyCalls,
        @Value("${trade-decision.daily-token-budget:300000}") long dailyTokens,
        @Value("${trade-decision.max-input-bytes:128000}") int maxInputBytes,
        @Value("${trade-decision.max-output-tokens:12000}") int maxOutputTokens,
        @Value("${trade-decision.thinking-level:LOW}") String thinkingLevel) {
    public DecisionSettings(boolean enabled,String ownerScope,String model,String apiKey,int dailyRequests,int dailyCalls,long dailyTokens,int maxInputBytes,int maxOutputTokens) {
        this(enabled,ownerScope,model,apiKey,dailyRequests,dailyCalls,dailyTokens,maxInputBytes,maxOutputTokens,"LOW");
    }
    @org.springframework.beans.factory.annotation.Autowired
    public DecisionSettings {
        if (ownerScope == null || !ownerScope.matches("[A-Za-z0-9._-]{1,100}") || dailyRequests < 1 || dailyCalls < 1
                || dailyTokens < 1 || maxInputBytes < 1000 || maxInputBytes > 200000 || maxOutputTokens < 256 || maxOutputTokens > 16000)
            throw new IllegalArgumentException("Invalid trade-decision configuration");
        if(thinkingLevel==null || !java.util.Set.of("","LOW","MEDIUM","HIGH").contains(thinkingLevel)) throw new IllegalArgumentException("Invalid thinking level");
    }
    public boolean aiConfigured() { return !apiKey.isBlank() && !model.isBlank(); }
    // Never expose configuration secrets in logs, including record-generated toString.
    @Override public String toString() { return "DecisionSettings[enabled=" + enabled + ",aiConfigured=" + aiConfigured() + "]"; }
}
