package com.example.stockanalysis.analysis.domain;

import com.example.stockanalysis.engine.model.AnalysisMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AnalysisRunView(
        long analysisRunId,
        String stockCode,
        String stockName,
        AnalysisMode analysisMode,
        AnalysisStatus status,
        DataQualityStatus dataQualityStatus,
        LocalDate baseDate,
        String engineVersion,
        BigDecimal totalScore,
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
        List<AnalysisFactorView> factors,
        List<String> reasons,
        List<String> warnings,
        String errorCode,
        String errorMessage,
        Instant requestedAt,
        Instant completedAt,
        com.example.stockanalysis.engine.model.SignalReport signalReport,
        com.example.stockanalysis.judgment.TradeJudgment tradeJudgment,
        com.example.stockanalysis.holdings.HoldingModels.Evaluation holdingAnalysis,
        com.example.stockanalysis.engine.model.TradeScenario.TargetBasis targetBasis) {
    public AnalysisRunView(
        long analysisRunId,
        String stockCode,
        String stockName,
        AnalysisMode analysisMode,
        AnalysisStatus status,
        DataQualityStatus dataQualityStatus,
        LocalDate baseDate,
        String engineVersion,
        BigDecimal totalScore,
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
        List<AnalysisFactorView> factors,
        List<String> reasons,
        List<String> warnings,
        String errorCode,
        String errorMessage,
        Instant requestedAt,
        Instant completedAt,
        com.example.stockanalysis.engine.model.SignalReport signalReport,
        com.example.stockanalysis.judgment.TradeJudgment tradeJudgment,
        com.example.stockanalysis.holdings.HoldingModels.Evaluation holdingAnalysis) {
        this(analysisRunId, stockCode, stockName, analysisMode, status, dataQualityStatus, baseDate, engineVersion, totalScore, scoreGrade, strategyCode, strategyName, strategyAction, currentPrice, entryFrom, entryTo, targets, stopLoss, historicalSampleCount, factors, reasons, warnings, errorCode, errorMessage, requestedAt, completedAt, signalReport, tradeJudgment, holdingAnalysis,null);
    }
    public AnalysisRunView(long analysisRunId,
        String stockCode,
        String stockName,
        AnalysisMode analysisMode,
        AnalysisStatus status,
        DataQualityStatus dataQualityStatus,
        LocalDate baseDate,
        String engineVersion,
        BigDecimal totalScore,
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
        List<AnalysisFactorView> factors,
        List<String> reasons,
        List<String> warnings,
        String errorCode,
        String errorMessage,
        Instant requestedAt,
        Instant completedAt,
        com.example.stockanalysis.engine.model.SignalReport signalReport,
        com.example.stockanalysis.judgment.TradeJudgment tradeJudgment) {
        this(analysisRunId, stockCode, stockName, analysisMode, status, dataQualityStatus, baseDate, engineVersion, totalScore, scoreGrade, strategyCode, strategyName, strategyAction, currentPrice, entryFrom, entryTo, targets, stopLoss, historicalSampleCount, factors, reasons, warnings, errorCode, errorMessage, requestedAt, completedAt, signalReport, tradeJudgment, null);
    }
    public AnalysisRunView withHolding(com.example.stockanalysis.holdings.HoldingModels.Evaluation holding) {
        return new AnalysisRunView(analysisRunId, stockCode, stockName, analysisMode, status, dataQualityStatus, baseDate, engineVersion, totalScore, scoreGrade, strategyCode, strategyName, strategyAction, currentPrice, entryFrom, entryTo, targets, stopLoss, historicalSampleCount, factors, reasons, warnings, errorCode, errorMessage, requestedAt, completedAt, signalReport, tradeJudgment, holding,targetBasis);
    }

    public AnalysisRunView(long analysisRunId, String stockCode, String stockName, AnalysisMode analysisMode,
                           AnalysisStatus status, DataQualityStatus dataQualityStatus, LocalDate baseDate,
                           String engineVersion, BigDecimal totalScore, String scoreGrade, String strategyCode,
                           String strategyName, String strategyAction, BigDecimal currentPrice, BigDecimal entryFrom,
                           BigDecimal entryTo, List<BigDecimal> targets, BigDecimal stopLoss, int historicalSampleCount,
                           List<AnalysisFactorView> factors, List<String> reasons, List<String> warnings,
                           String errorCode, String errorMessage, Instant requestedAt, Instant completedAt,
                           com.example.stockanalysis.engine.model.SignalReport signalReport) {
        this(analysisRunId, stockCode, stockName, analysisMode, status, dataQualityStatus, baseDate, engineVersion,
                totalScore, scoreGrade, strategyCode, strategyName, strategyAction, currentPrice, entryFrom, entryTo,
                targets, stopLoss, historicalSampleCount, factors, reasons, warnings, errorCode, errorMessage,
                requestedAt, completedAt, signalReport, null);
    }

    public AnalysisRunView(long analysisRunId, String stockCode, String stockName, AnalysisMode analysisMode,
                           AnalysisStatus status, DataQualityStatus dataQualityStatus, LocalDate baseDate,
                           String engineVersion, BigDecimal totalScore, String scoreGrade, String strategyCode,
                           String strategyName, String strategyAction, BigDecimal currentPrice, BigDecimal entryFrom,
                           BigDecimal entryTo, List<BigDecimal> targets, BigDecimal stopLoss, int historicalSampleCount,
                           List<AnalysisFactorView> factors, List<String> reasons, List<String> warnings,
                           String errorCode, String errorMessage, Instant requestedAt, Instant completedAt) {
        this(analysisRunId, stockCode, stockName, analysisMode, status, dataQualityStatus, baseDate, engineVersion,
                totalScore, scoreGrade, strategyCode, strategyName, strategyAction, currentPrice, entryFrom, entryTo,
                targets, stopLoss, historicalSampleCount, factors, reasons, warnings, errorCode, errorMessage,
                requestedAt, completedAt, null);
    }

    public AnalysisRunView {
        targets = targets == null ? List.of() : List.copyOf(targets);
        factors = factors == null ? List.of() : List.copyOf(factors);
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
