package com.example.stockanalysis.engine.strategy;

import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.AnalysisMode;
import com.example.stockanalysis.engine.model.FactorResult;
import com.example.stockanalysis.engine.model.StrategyAction;
import com.example.stockanalysis.engine.model.StrategyDecision;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.example.stockanalysis.engine.factor.internal.SwingTimeframeFactorCalculator.DAILY_CODE;
import static com.example.stockanalysis.engine.factor.internal.SwingTimeframeFactorCalculator.MONTHLY_CODE;
import static com.example.stockanalysis.engine.factor.internal.SwingTimeframeFactorCalculator.WEEKLY_CODE;

public final class DefaultStrategyPolicy implements StrategyPolicy {

    @Override
    public StrategyDecision decide(
            AnalysisInput input,
            BigDecimal totalScore,
            List<FactorResult> factors
    ) {
        boolean pullbackWait = isPullbackWait(input, factors);
        StrategyAction action = action(input, totalScore, factors, pullbackWait);
        boolean applyPullbackWait = pullbackWait && action == StrategyAction.WAIT;
        String modeName = input.mode().name();
        Comparator<FactorResult> reasonOrder = Comparator.comparing(FactorResult::score);
        if (action != StrategyAction.AVOID) reasonOrder = reasonOrder.reversed();
        List<String> reasons = factors.stream()
                .sorted(reasonOrder)
                .limit(3)
                .map(FactorResult::reason)
                .toList();
        return new StrategyDecision(
                applyPullbackWait ? "SWING_PULLBACK_WAIT" : modeName + "_" + action.name(),
                applyPullbackWait ? "스윙 눌림목 대기" : strategyName(input, action),
                action,
                reasons
        );
    }

    private StrategyAction action(
            AnalysisInput input,
            BigDecimal score,
            List<FactorResult> factors,
            boolean pullbackWait
    ) {
        if (input.mode() == AnalysisMode.SWING) {
            Optional<BigDecimal> monthly = factorScore(factors, MONTHLY_CODE);
            if (monthly.isPresent() && monthly.orElseThrow().compareTo(BigDecimal.valueOf(40)) < 0) {
                return StrategyAction.AVOID;
            }
            Optional<BigDecimal> risk = factorScore(
                    factors,
                    com.example.stockanalysis.engine.factor.internal.RiskStabilityFactorCalculator.CODE
            );
            if (risk.isPresent() && risk.orElseThrow().compareTo(BigDecimal.valueOf(35)) < 0) {
                return StrategyAction.AVOID;
            }
            Optional<BigDecimal> structure = factorScore(
                    factors,
                    com.example.stockanalysis.engine.factor.internal.TrendFactorCalculator.CODE
            );
            Optional<BigDecimal> momentum = factorScore(
                    factors,
                    com.example.stockanalysis.engine.factor.internal.MomentumFactorCalculator.CODE
            );
            if (structure.isPresent() && momentum.isPresent()
                    && structure.orElseThrow().compareTo(BigDecimal.valueOf(35)) < 0
                    && momentum.orElseThrow().compareTo(BigDecimal.valueOf(40)) < 0) {
                return StrategyAction.AVOID;
            }
            if (pullbackWait) {
                return StrategyAction.WAIT;
            }
        }
        if (score.compareTo(BigDecimal.valueOf(75)) >= 0
                && meetsBuyCandidateMinimums(factors)) {
            return StrategyAction.BUY_CANDIDATE;
        }
        if (score.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return StrategyAction.WATCH;
        }
        if (score.compareTo(BigDecimal.valueOf(45)) >= 0) {
            return StrategyAction.WAIT;
        }
        return StrategyAction.AVOID;
    }

    private boolean isPullbackWait(AnalysisInput input, List<FactorResult> factors) {
        if (input.mode() != AnalysisMode.SWING) return false;
        Optional<BigDecimal> structure = factorScore(
                factors,
                com.example.stockanalysis.engine.factor.internal.TrendFactorCalculator.CODE
        );
        Optional<BigDecimal> momentum = factorScore(
                factors,
                com.example.stockanalysis.engine.factor.internal.MomentumFactorCalculator.CODE
        );
        Optional<BigDecimal> daily = factorScore(factors, DAILY_CODE);
        Optional<BigDecimal> weekly = factorScore(factors, WEEKLY_CODE);
        if (structure.isPresent() && momentum.isPresent()) {
            return structure.orElseThrow().compareTo(BigDecimal.valueOf(60)) >= 0
                    && momentum.orElseThrow().compareTo(BigDecimal.valueOf(40)) >= 0
                    && momentum.orElseThrow().compareTo(BigDecimal.valueOf(60)) < 0
                    && weekly.map(score -> score.compareTo(BigDecimal.valueOf(60)) >= 0)
                            .orElse(true);
        }
        return daily.isPresent()
                && weekly.isPresent()
                && weekly.orElseThrow().compareTo(BigDecimal.valueOf(65)) >= 0
                && daily.orElseThrow().compareTo(BigDecimal.valueOf(60)) < 0;
    }

    private boolean meetsBuyCandidateMinimums(List<FactorResult> factors) {
        return atLeastWhenPresent(factors,
                com.example.stockanalysis.engine.factor.internal.TrendFactorCalculator.CODE, 60)
                && atLeastWhenPresent(factors,
                com.example.stockanalysis.engine.factor.internal.MomentumFactorCalculator.CODE, 55)
                && atLeastWhenPresent(factors,
                com.example.stockanalysis.engine.factor.internal.RiskStabilityFactorCalculator.CODE, 45);
    }

    private boolean atLeastWhenPresent(List<FactorResult> factors, String code, int minimum) {
        return factorScore(factors, code)
                .map(score -> score.compareTo(BigDecimal.valueOf(minimum)) >= 0)
                .orElse(true);
    }

    private Optional<BigDecimal> factorScore(List<FactorResult> factors, String code) {
        return factors.stream()
                .filter(factor -> code.equals(factor.factorCode()))
                .map(FactorResult::score)
                .findFirst();
    }

    private String strategyName(AnalysisInput input, StrategyAction action) {
        String prefix = input.mode().name().equals("SWING") ? "스윙" : "단기";
        return switch (action) {
            case BUY_CANDIDATE -> prefix + " 매수 후보";
            case WATCH -> prefix + " 관찰";
            case WAIT -> prefix + " 대기";
            case AVOID -> prefix + " 회피";
        };
    }
}
