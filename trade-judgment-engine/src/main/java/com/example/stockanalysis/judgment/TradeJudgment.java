package com.example.stockanalysis.judgment;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TradeJudgment(String policyVersion, String analysisVersion, Instant asOf, LocalDate priceDate,
                            String positionScope, Decision notHeld, Decision held, List<String> limitations,
                            NewEntryAssessment newEntry) {
    public TradeJudgment(String policyVersion, String analysisVersion, Instant asOf, LocalDate priceDate,
                         String positionScope, Decision notHeld, Decision held, List<String> limitations) {
        this(policyVersion, analysisVersion, asOf, priceDate, positionScope, notHeld, held, limitations, null);
    }
    public enum Action { STRONG_BUY_CANDIDATE, BUY_CANDIDATE, WATCH, AVOID, HOLD, REVIEW_HOLDING,
        SELL_PRIORITY, EXIT_TRIGGERED, TAKE_PROFIT_REVIEW, DEFER }
    public record Decision(Action action, List<String> reasons, List<String> reviewConditions) {
        public Decision { reasons = List.copyOf(reasons); reviewConditions = List.copyOf(reviewConditions); }
    }
    public TradeJudgment { limitations = List.copyOf(limitations); }
}
