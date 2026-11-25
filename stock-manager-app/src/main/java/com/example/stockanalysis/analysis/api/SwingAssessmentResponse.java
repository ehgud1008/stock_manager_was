package com.example.stockanalysis.analysis.api;

import com.example.stockanalysis.analysis.domain.AnalysisRunView;
import com.example.stockanalysis.engine.factor.internal.SwingSuitabilityEngine;

/** Explicit v5 fields reconstructed from the persisted, versioned decision code, not from translated reason text. */
public record SwingAssessmentResponse(String setupType, String setupLabel, String detailCode, String detailLabel,
                                      String scenarioStatus) {
    static SwingAssessmentResponse from(AnalysisRunView view) {
        if (!("analysis-v5-swing".equals(view.engineVersion()) || "analysis-v6-swing".equals(view.engineVersion()) || "analysis-v7-swing".equals(view.engineVersion())) || view.strategyCode() == null) return null;
        return fromStrategy(view.strategyCode(), view.entryFrom() != null);
    }

    public static SwingAssessmentResponse fromStrategy(String strategyCode, boolean hasEntry) {
        String[] code = strategyCode.split(":");
        if (code.length != 4 || !"SWING_V5".equals(code[0])) return null;
        String label = switch (code[1]) {
            case "BREAKOUT" -> "돌파형";
            case "PULLBACK" -> "눌림목형";
            case "CONTINUATION" -> "추세 지속형";
            case "RANGE" -> "방향 미확정 구간형";
            default -> "유형 미확정";
        };
        return new SwingAssessmentResponse(code[1], label, code[2], SwingSuitabilityEngine.detailLabel(code[2]),
                hasEntry ? "AVAILABLE" : "UNAVAILABLE");
    }
}
