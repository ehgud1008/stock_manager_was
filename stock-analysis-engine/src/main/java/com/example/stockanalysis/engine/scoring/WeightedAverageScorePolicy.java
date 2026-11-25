package com.example.stockanalysis.engine.scoring;

import com.example.stockanalysis.engine.model.FactorResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class WeightedAverageScorePolicy implements ScorePolicy {

    @Override
    public BigDecimal calculate(List<FactorResult> factors) {
        if (factors == null || factors.isEmpty()) {
            throw new IllegalArgumentException("factors must not be empty");
        }

        BigDecimal weightedScore = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (FactorResult factor : factors) {
            weightedScore = weightedScore.add(factor.score().multiply(factor.weight()));
            totalWeight = totalWeight.add(factor.weight());
        }
        return weightedScore.divide(totalWeight, 4, RoundingMode.HALF_UP);
    }
}
