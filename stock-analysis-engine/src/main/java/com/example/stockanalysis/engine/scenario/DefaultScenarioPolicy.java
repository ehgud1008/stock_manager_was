package com.example.stockanalysis.engine.scenario;

import com.example.stockanalysis.engine.factor.internal.RiskStabilityFactorCalculator;
import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.engine.model.FactorResult;
import com.example.stockanalysis.engine.model.StrategyDecision;
import com.example.stockanalysis.engine.model.TradeScenario;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

public final class DefaultScenarioPolicy implements ScenarioPolicy {

    @Override
    public TradeScenario calculate(
            AnalysisInput input,
            BigDecimal totalScore,
            StrategyDecision strategy,
            List<FactorResult> factors
    ) {
        List<DailyCandle> candles = input.dailyCandles().stream()
                .sorted(Comparator.comparing(DailyCandle::businessDate))
                .toList();
        BigDecimal current = input.stock().currentPrice();
        BigDecimal volatility = RiskStabilityFactorCalculator.averageTrueRange(candles, 14);
        if (volatility.signum() == 0) {
            volatility = current.multiply(new BigDecimal("0.02"));
        }

        PriceLevels levels = priceLevels(candles, current);
        boolean pullback = strategy.strategyCode().contains("PULLBACK");
        boolean breakout = current.compareTo(levels.resistance()) > 0;
        BigDecimal entryFrom;
        BigDecimal entryTo;
        if (pullback) {
            BigDecimal anchor = levels.support().max(levels.averageClose()).min(current);
            entryFrom = money(anchor.subtract(volatility.multiply(new BigDecimal("0.25")))
                    .max(current.multiply(new BigDecimal("0.92"))));
            entryTo = money(anchor.add(volatility.multiply(new BigDecimal("0.25")))
                    .min(current));
        } else if (breakout) {
            entryFrom = money(levels.resistance()
                    .max(current.subtract(volatility.multiply(new BigDecimal("0.25")))));
            entryTo = money(current.add(volatility.multiply(new BigDecimal("0.10"))));
        } else {
            entryFrom = money(current.subtract(volatility.multiply(new BigDecimal("0.50")))
                    .max(current.multiply(new BigDecimal("0.98"))));
            entryTo = money(current);
        }
        if (entryFrom.compareTo(entryTo) > 0) entryFrom = entryTo;

        BigDecimal structuralStop = levels.support()
                .subtract(volatility.multiply(new BigDecimal("0.50")));
        BigDecimal maximumRiskStop = entryFrom.multiply(new BigDecimal("0.92"));
        BigDecimal stopLoss = money(structuralStop.max(maximumRiskStop));
        if (stopLoss.compareTo(entryFrom) >= 0) {
            stopLoss = entryFrom.subtract(volatility.multiply(new BigDecimal("0.50")))
                    .min(entryFrom.subtract(BigDecimal.ONE));
            stopLoss = money(stopLoss.max(BigDecimal.ONE));
        }
        BigDecimal risk = entryTo.subtract(stopLoss).max(volatility);
        BigDecimal targetOne = money(entryTo.add(risk.multiply(new BigDecimal("1.50")))
                .max(levels.resistance()));
        BigDecimal targetTwo = money(entryTo.add(risk.multiply(new BigDecimal("2.00"))));
        if (targetOne.compareTo(entryTo) <= 0) targetOne = entryTo.add(BigDecimal.ONE);
        if (targetTwo.compareTo(targetOne) <= 0) {
            targetTwo = targetOne.add(BigDecimal.ONE);
        }
        return new TradeScenario(entryFrom, entryTo, List.of(targetOne, targetTwo), stopLoss);
    }

    private PriceLevels priceLevels(List<DailyCandle> candles, BigDecimal fallback) {
        int end = Math.max(0, candles.size() - 1);
        int from = Math.max(0, end - 20);
        List<DailyCandle> sample = candles.subList(from, end);
        if (sample.isEmpty()) sample = candles;
        BigDecimal support = sample.stream().map(DailyCandle::low)
                .min(BigDecimal::compareTo).orElse(fallback);
        BigDecimal resistance = sample.stream().map(DailyCandle::high)
                .max(BigDecimal::compareTo).orElse(fallback);
        BigDecimal averageClose = sample.stream().map(DailyCandle::close)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(sample.size()), 8, RoundingMode.HALF_UP);
        return new PriceLevels(support, resistance, averageClose);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    private record PriceLevels(
            BigDecimal support,
            BigDecimal resistance,
            BigDecimal averageClose
    ) {
    }
}
