package com.example.stockanalysis.judgment;

import com.example.stockanalysis.engine.model.TradeScenario;
import com.example.stockanalysis.engine.model.TradeSignal;
import java.math.BigDecimal;
import java.util.List;

/** Immutable explanation of the common new-entry decision, independent of holdings and market ranking. */
public record NewEntryAssessment(String policyVersion, Status status, List<String> reasons,
                                 TradeSignal selectedSignal, TradeScenario effectiveScenario,
                                 BigDecimal grossRewardRisk, List<SignalCheck> signals,
                                 SuitabilityTier suitability, Timing timing) {
    public NewEntryAssessment(String policyVersion, Status status, List<String> reasons, TradeSignal selectedSignal,
                              TradeScenario effectiveScenario, BigDecimal grossRewardRisk, List<SignalCheck> signals) {
        this(policyVersion,status,reasons,selectedSignal,effectiveScenario,grossRewardRisk,signals,null,null);
    }
    public record Timing(Status status,List<String> reasons,boolean signalWaiting) {
        public Timing { reasons=List.copyOf(reasons); }
    }
    public enum Status { ELIGIBLE, WATCH, AVOID, DEFER }
    public record SignalCheck(String signalId, String type, int ageBars, boolean eligible,
                              BigDecimal entryFrom, BigDecimal entryTo, List<String> reasons) {
        public SignalCheck { reasons = List.copyOf(reasons); }
    }
    public NewEntryAssessment { reasons = List.copyOf(reasons); signals = List.copyOf(signals); }
}
