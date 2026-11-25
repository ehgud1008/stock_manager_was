package com.example.stockanalysis.analysis.config;

import com.example.stockanalysis.engine.api.StockAnalysisEngine;
import com.example.stockanalysis.engine.factor.internal.MomentumFactorCalculator;
import com.example.stockanalysis.engine.factor.internal.RelativeStrengthFactorCalculator;
import com.example.stockanalysis.engine.factor.internal.RiskStabilityFactorCalculator;
import com.example.stockanalysis.engine.factor.internal.SupplyDemandFactorCalculator;
import com.example.stockanalysis.engine.factor.internal.SwingTimeframeFactorCalculator;
import com.example.stockanalysis.engine.factor.internal.TrendFactorCalculator;
import com.example.stockanalysis.engine.factor.internal.ValueQualityFactorCalculator;
import com.example.stockanalysis.engine.factor.internal.VolumeFactorCalculator;
import com.example.stockanalysis.engine.internal.DefaultStockAnalysisEngine;
import com.example.stockanalysis.engine.scenario.DefaultScenarioPolicy;
import com.example.stockanalysis.engine.scoring.WeightedAverageScorePolicy;
import com.example.stockanalysis.engine.strategy.DefaultStrategyPolicy;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class StockAnalysisEngineConfig {

    public static final String ENGINE_VERSION = com.example.stockanalysis.engine.factor.internal.SwingSuitabilityEngine.VERSION;
    public static final String CONFIG_VERSION = com.example.stockanalysis.engine.signal.SwingSignalEngine.VERSION;

    public static String engineVersion(com.example.stockanalysis.engine.model.AnalysisMode mode) {
        return mode == com.example.stockanalysis.engine.model.AnalysisMode.SWING ? ENGINE_VERSION : "analysis-v3";
    }

    public static String configVersion(com.example.stockanalysis.engine.model.AnalysisMode mode) {
        return mode == com.example.stockanalysis.engine.model.AnalysisMode.SWING ? CONFIG_VERSION : "swing-directional-v1";
    }

    @Bean
    public StockAnalysisEngine stockAnalysisEngine() {
        StockAnalysisEngine legacy = new DefaultStockAnalysisEngine(
                "analysis-v3",
                List.of(
                        new TrendFactorCalculator(),
                        new MomentumFactorCalculator(),
                        new VolumeFactorCalculator(),
                        new SupplyDemandFactorCalculator(),
                        new RelativeStrengthFactorCalculator(),
                        new RiskStabilityFactorCalculator(),
                        new ValueQualityFactorCalculator(),
                        SwingTimeframeFactorCalculator.daily(),
                        SwingTimeframeFactorCalculator.weekly(),
                        SwingTimeframeFactorCalculator.monthly()
                ),
                new WeightedAverageScorePolicy(),
                new DefaultStrategyPolicy(),
                new DefaultScenarioPolicy()
        );
        StockAnalysisEngine swing = new com.example.stockanalysis.engine.factor.internal.SwingSuitabilityEngine();
        return input -> {
            if (input.mode() != com.example.stockanalysis.engine.model.AnalysisMode.SWING) return legacy.analyze(input);
            var result = swing.analyze(input);
            var signals = new com.example.stockanalysis.engine.signal.SwingSignalEngine().evaluate(input, result.scenario());
            return new com.example.stockanalysis.engine.model.AnalysisResult(result.engineVersion(), result.mode(),
                    result.totalScore(), result.factors(), result.strategy(), result.scenario(), result.warnings(), signals);
        };
    }
}
