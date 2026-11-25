package com.example.stockanalysis.analysis.api;

import com.example.stockanalysis.analysis.domain.AnalysisRunView;
import com.example.stockanalysis.analysis.domain.AnalysisFactorView;
import com.example.stockanalysis.engine.model.AnalysisResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record SuitabilityScoresResponse(String status, BigDecimal rise, BigDecimal entry,
                                        BigDecimal risk, int availableFactors, int totalFactors) {
    private static final List<String> RISE = List.of("TREND", "MOMENTUM", "VOLUME", "SUPPLY_DEMAND", "RELATIVE_STRENGTH");
    private static final List<String> ENTRY = List.of("ENTRY_LOCATION", "ENTRY_REWARD_RISK", "ENTRY_STOP_DISTANCE");
    private static final List<String> RISK = List.of("RISK_STABILITY", "LIQUIDITY");

    static SuitabilityScoresResponse from(AnalysisRunView view) {
        return calculate(view.engineVersion(), view.totalScore(), view.entryFrom() != null, view.factors());
    }

    public static SuitabilityScoresResponse from(AnalysisResult result) {
        var factors = result.factors().stream().map(f -> {
            BigDecimal contribution = f.score() == null ? null : f.score().multiply(f.weight()).setScale(8, RoundingMode.HALF_UP);
            return new AnalysisFactorView(f.factorCode(), f.rawValue(), f.score(), f.weight(), contribution, f.reason());
        }).toList();
        return calculate(result.engineVersion(), result.totalScore(), result.scenario() != null, factors);
    }

    private static SuitabilityScoresResponse calculate(String engineVersion, BigDecimal totalScore,
                                                        boolean hasEntry, List<AnalysisFactorView> factors) {
        boolean v5 = engineVersion != null && List.of("analysis-v5-swing", "analysis-v6-swing", "analysis-v7-swing").contains(engineVersion);
        if (!v5 && !"analysis-v4-swing".equals(engineVersion)) return null;
        int available = (int) factors.stream().filter(f -> (RISE.contains(f.factorCode())
                || ENTRY.contains(f.factorCode()) || RISK.contains(f.factorCode())) && f.score() != null).count();
        int total = v5 ? 10 : 9;
        String status = totalScore == null ? "UNAVAILABLE" : available < total ? "PROVISIONAL" : "COMPLETE";
        boolean provisional = totalScore == null;
        return new SuitabilityScoresResponse(status,
                group(factors, RISE, ".6", provisional, v5),
                group(factors, ENTRY, ".3", provisional, v5 && hasEntry),
                group(factors, RISK, ".1", provisional, v5), available, total);
    }

    private static BigDecimal group(List<AnalysisFactorView> factors, List<String> codes, String weight,
                                    boolean provisional, boolean canShowProvisional) {
        if (provisional && (!canShowProvisional
                || factors.stream().noneMatch(f -> codes.contains(f.factorCode()) && f.score() != null))) return null;
        return factors.stream().filter(f -> codes.contains(f.factorCode()) && f.contribution() != null)
                .map(f -> f.contribution()).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(weight), 4, RoundingMode.HALF_UP);
    }
}
