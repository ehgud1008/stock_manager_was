package com.example.stockanalysis.analysis.api;

import com.example.stockanalysis.analysis.domain.AnalysisRunView;
import com.example.stockanalysis.analysis.domain.AnalysisStatus;
import com.example.stockanalysis.analysis.domain.DataQualityStatus;
import com.example.stockanalysis.engine.model.AnalysisMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record StockAnalysisResponse(
        long analysisRunId,
        String stockCode,
        String stockName,
        AnalysisMode analysisMode,
        AnalysisStatus status,
        DataQualityStatus dataQualityStatus,
        LocalDate baseDate,
        String engineVersion,
        BigDecimal totalScore,
        SuitabilityScoresResponse suitabilityScores,
        SwingAssessmentResponse swingAssessment,
        com.example.stockanalysis.engine.model.SignalReport signalReport,
        TimeframeScoresResponse timeframeScores,
        String scoreGrade,
        String strategyCode,
        String strategyName,
        String strategyAction,
        BigDecimal currentPrice,
        BigDecimal entryFrom,
        BigDecimal entryTo,
        List<BigDecimal> targets,
        BigDecimal stopLoss,
        int historicalSampleCount,
        List<AnalysisFactorResponse> factors,
        List<String> reasons,
        List<String> warnings,
        String errorCode,
        String errorMessage,
        Instant requestedAt,
        Instant completedAt,
        com.example.stockanalysis.judgment.TradeJudgment tradeJudgment,
        com.example.stockanalysis.holdings.HoldingModels.Evaluation holdingAnalysis,
        com.example.stockanalysis.engine.model.TradeScenario.TargetBasis targetBasis) {
    public StockAnalysisResponse(
        long analysisRunId,
        String stockCode,
        String stockName,
        AnalysisMode analysisMode,
        AnalysisStatus status,
        DataQualityStatus dataQualityStatus,
        LocalDate baseDate,
        String engineVersion,
        BigDecimal totalScore,
        SuitabilityScoresResponse suitabilityScores,
        SwingAssessmentResponse swingAssessment,
        com.example.stockanalysis.engine.model.SignalReport signalReport,
        TimeframeScoresResponse timeframeScores,
        String scoreGrade,
        String strategyCode,
        String strategyName,
        String strategyAction,
        BigDecimal currentPrice,
        BigDecimal entryFrom,
        BigDecimal entryTo,
        List<BigDecimal> targets,
        BigDecimal stopLoss,
        int historicalSampleCount,
        List<AnalysisFactorResponse> factors,
        List<String> reasons,
        List<String> warnings,
        String errorCode,
        String errorMessage,
        Instant requestedAt,
        Instant completedAt,
        com.example.stockanalysis.judgment.TradeJudgment tradeJudgment,
        com.example.stockanalysis.holdings.HoldingModels.Evaluation holdingAnalysis) {
        this(analysisRunId, stockCode, stockName, analysisMode, status, dataQualityStatus, baseDate, engineVersion, totalScore, suitabilityScores, swingAssessment, signalReport, timeframeScores, scoreGrade, strategyCode, strategyName, strategyAction, currentPrice, entryFrom, entryTo, targets, stopLoss, historicalSampleCount, factors, reasons, warnings, errorCode, errorMessage, requestedAt, completedAt, tradeJudgment, holdingAnalysis,null);
    }
    public StockAnalysisResponse(long analysisRunId,
        String stockCode,
        String stockName,
        AnalysisMode analysisMode,
        AnalysisStatus status,
        DataQualityStatus dataQualityStatus,
        LocalDate baseDate,
        String engineVersion,
        BigDecimal totalScore,
        SuitabilityScoresResponse suitabilityScores,
        SwingAssessmentResponse swingAssessment,
        com.example.stockanalysis.engine.model.SignalReport signalReport,
        TimeframeScoresResponse timeframeScores,
        String scoreGrade,
        String strategyCode,
        String strategyName,
        String strategyAction,
        BigDecimal currentPrice,
        BigDecimal entryFrom,
        BigDecimal entryTo,
        List<BigDecimal> targets,
        BigDecimal stopLoss,
        int historicalSampleCount,
        List<AnalysisFactorResponse> factors,
        List<String> reasons,
        List<String> warnings,
        String errorCode,
        String errorMessage,
        Instant requestedAt,
        Instant completedAt,
        com.example.stockanalysis.judgment.TradeJudgment tradeJudgment) {
        this(analysisRunId, stockCode, stockName, analysisMode, status, dataQualityStatus, baseDate, engineVersion, totalScore, suitabilityScores, swingAssessment, signalReport, timeframeScores, scoreGrade, strategyCode, strategyName, strategyAction, currentPrice, entryFrom, entryTo, targets, stopLoss, historicalSampleCount, factors, reasons, warnings, errorCode, errorMessage, requestedAt, completedAt, tradeJudgment, null);
    }
    public StockAnalysisResponse withHolding(com.example.stockanalysis.holdings.HoldingModels.Evaluation holding) {
        return new StockAnalysisResponse(analysisRunId, stockCode, stockName, analysisMode, status, dataQualityStatus, baseDate, engineVersion, totalScore, suitabilityScores, swingAssessment, signalReport, timeframeScores, scoreGrade, strategyCode, strategyName, strategyAction, currentPrice, entryFrom, entryTo, targets, stopLoss, historicalSampleCount, factors, reasons, warnings, errorCode, errorMessage, requestedAt, completedAt, tradeJudgment, holding,targetBasis);
    }


    public static StockAnalysisResponse from(AnalysisRunView view) {
        return new StockAnalysisResponse(
                view.analysisRunId(),
                view.stockCode(),
                view.stockName(),
                view.analysisMode(),
                view.status(),
                view.dataQualityStatus(),
                view.baseDate(),
                view.engineVersion(),
                view.totalScore(),
                SuitabilityScoresResponse.from(view),
                SwingAssessmentResponse.from(view),
                view.signalReport(),
                TimeframeScoresResponse.from(view.factors()),
                view.scoreGrade(),
                view.strategyCode(),
                view.strategyName(),
                view.strategyAction(),
                view.currentPrice(),
                view.entryFrom(),
                view.entryTo(),
                view.targets(),
                view.stopLoss(),
                view.historicalSampleCount(),
                view.factors().stream().map(AnalysisFactorResponse::from).toList(),
                view.reasons(),
                view.warnings(),
                view.errorCode(),
                view.errorMessage(),
                view.requestedAt(),
                view.completedAt(),
                view.tradeJudgment(), view.holdingAnalysis(),view.targetBasis()
        );
    }
}
