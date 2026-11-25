package com.example.stockanalysis.decision;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class DecisionModels {
    private DecisionModels() {}
    public enum Position { UNKNOWN, NOT_HELD, HELD }
    public record SnapshotRequest(
            @Schema(description = "COLLECT: 새 분석 수집, UNIFIED_RUN_ITEM: 저장된 종합분석에서 가져오기",
                    allowableValues = {"COLLECT", "UNIFIED_RUN_ITEM"}, example = "COLLECT")
            @NotNull @Pattern(regexp="COLLECT|UNIFIED_RUN_ITEM") String source,
            @Schema(description = "UNIFIED_RUN_ITEM이면 필수 원본 실행 ID. COLLECT이면 생략")
            @Size(max=36) String runId) {}
    public record Preferences(@Min(0) @Max(100) int aggressive, @Min(0) @Max(100) int balanced, @Min(0) @Max(100) int conservative,
                              String monitoring, String entryPreference, String volatilityTolerance, String lossResponse) {
        public static Preferences defaults() {return new Preferences(20,60,20,"DAILY","CONFIRMED","MODERATE","STRUCTURAL");}
    }
    public record ReviewRequest(@NotNull Position positionStatus, @Min(1) @Max(252) Integer horizonTradingDays,
                                @DecimalMin(value="0",inclusive=false) @Digits(integer=12,fraction=2) BigDecimal averageBuyPrice,
                                LocalDate purchasedOn, @Valid Preferences preferences,
                                @DecimalMin(value="0",inclusive=false) @Digits(integer=10,fraction=0) BigDecimal quantity,
                                LocalDate balanceDate) {
        public ReviewRequest(Position positionStatus,Integer horizonTradingDays,BigDecimal price,LocalDate date,Preferences preferences) {this(positionStatus,horizonTradingDays,price,date,preferences,null,null);}
        public ReviewRequest(Position positionStatus,int horizonTradingDays) {this(positionStatus,horizonTradingDays,null,null,null);}
        public ReviewRequest(Position positionStatus,Integer horizonTradingDays,BigDecimal price,LocalDate date) {this(positionStatus,horizonTradingDays,price,date,null);}
        public Preferences effectivePreferences() {return preferences==null?Preferences.defaults():preferences;}
    }
    public record Job(String id, String owner, String kind, String stockCode, String snapshotId, String parentId,
                      String status, JsonNode input, JsonNode result, String resultHash, String action, long createdAt,
                      Long completedAt, int attempt, String leaseOwner, Long leaseUntil, String errorCode, boolean retryable) {}
    public record View(String id, String kind, String stockCode, String snapshotId, String parentId, String status,
                       JsonNode context, JsonNode result, String action, long createdAt, Long completedAt,
                       int attempts, String errorCode, boolean retryable, String mode, String contractVersion) {
        public View(String id,String kind,String code,String snapshot,String parent,String status,JsonNode context,JsonNode result,String action,long created,Long completed,int attempts,String error,boolean retryable) {
            this(id,kind,code,snapshot,parent,status,context,result,action,created,completed,attempts,error,retryable,null,null);
        }
    }
    public record History(List<Summary> items, int page, int size, boolean hasMore) {}
    public record Summary(String id, String stockCode, String snapshotId, String parentId, String status,
                          String action, long createdAt, Long completedAt, String errorCode, String mode, String contractVersion) {}
    public record Review(String recommendation, String planId, List<Reason> supportingReasons,
                         List<Reason> opposingReasons, List<Scenario> scenarios, List<String> missingInformation,
                         List<String> reviewConditionIds, Detail detail, Independent independent, JsonNode personalization) {}
    public record Independent(String strength, String alignment, Explanation engineComparison,
                              List<Strategy> strategies, String strategyOmissionReason) {}
    public record PriceLevel(BigDecimal value, String kind, String sourcePath, BigDecimal adjustmentPct,
                             String basis) {}
    public record Strategy(String id, int horizonTradingDays, PriceLevel entryLow, PriceLevel entryHigh,
                           PriceLevel stop, List<PriceLevel> targets, String rationale,
                           List<String> evidenceIds, List<String> assumptions, String invalidation, String timeCondition) {}
    public record Explanation(String text,List<String> evidenceIds,List<String> metricIds) {}
    public record Detail(Explanation conclusion,Explanation horizonAssessment,Explanation entryPlan,
                         Explanation exitPlan,Explanation timeReview) {}
    public record Reason(String evidenceId, String explanation) {}
    public record Scenario(String conditionId, String response, String trigger, String invalidation, List<String> metricIds,
                           String strategyId, String development, String newInvestorAction, String holderAction, String timeCondition) {
        public Scenario(String conditionId,String response) {this(conditionId,response,"조건 충족 여부를 확인합니다.","조건이 깨지면 재평가합니다.",List.of(),
                null,"추세 전개를 관찰합니다.","확인 전 진입을 보류합니다.","추세 이탈 시 보유를 재검토합니다.","신호 만료 시 재검토합니다.");}
    }
    public record AiResponse(Review review, String providerId, JsonNode raw, long inputTokens, long outputTokens) {}
    public record MarketResponse(JsonNode assessment, String providerId, JsonNode raw, long inputTokens, long outputTokens) {}
    public static class AiFailure extends RuntimeException {
        private final String code; private final boolean retryable; private final long delayMillis;
        public AiFailure(String code, boolean retryable) { this(code,retryable,0); }
        public AiFailure(String code, boolean retryable, long delayMillis) { super(code); this.code=code; this.retryable=retryable; this.delayMillis=delayMillis; }
        public String code() { return code; } public boolean retryable() { return retryable; } public long delayMillis() { return delayMillis; }
    }
}
