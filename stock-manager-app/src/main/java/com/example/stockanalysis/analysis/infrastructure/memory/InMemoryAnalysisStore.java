package com.example.stockanalysis.analysis.infrastructure.memory;

import com.example.stockanalysis.analysis.application.model.CollectedAnalysisData;
import com.example.stockanalysis.analysis.application.model.NewAnalysisRun;
import com.example.stockanalysis.analysis.application.port.out.AnalysisStore;
import com.example.stockanalysis.analysis.domain.AnalysisFactorView;
import com.example.stockanalysis.analysis.domain.AnalysisRunView;
import com.example.stockanalysis.analysis.domain.AnalysisStatus;
import com.example.stockanalysis.analysis.domain.DataQualityStatus;
import com.example.stockanalysis.analysis.domain.StockPricePoint;
import com.example.stockanalysis.engine.model.AnalysisMode;
import com.example.stockanalysis.engine.model.AnalysisResult;
import com.example.stockanalysis.engine.model.DailyCandle;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryAnalysisStore implements AnalysisStore {

    private final AtomicLong sequence = new AtomicLong();
    private final Map<Long, StoredRun> runs = new ConcurrentHashMap<>();

    @Override
    public long createRun(NewAnalysisRun run) {
        long id = sequence.incrementAndGet();
        runs.put(id, new StoredRun(run, runningView(id, run), null));
        return id;
    }

    @Override
    public void saveCompleted(
            long runId,
            CollectedAnalysisData data,
            AnalysisResult result,
            Instant completedAt,
            com.example.stockanalysis.judgment.TradeJudgment judgment
    ) {
        StoredRun stored = required(runId);
        List<AnalysisFactorView> factors = result.factors().stream()
                .map(factor -> new AnalysisFactorView(
                        factor.factorCode(),
                        factor.rawValue(),
                        factor.score(),
                        factor.weight(),
                        factor.score() == null ? null : factor.score().multiply(factor.weight()),
                        factor.reason()
                ))
                .toList();
        AnalysisRunView view = new AnalysisRunView(
                runId,
                stored.request().stockCode(),
                data.input().stock().stockName(),
                stored.request().analysisMode(),
                AnalysisStatus.COMPLETED,
                data.dataQualityStatus(),
                stored.request().baseDate(),
                result.engineVersion(),
                result.totalScore(),
                grade(result.totalScore()),
                result.strategy().strategyCode(),
                result.strategy().strategyName(),
                result.strategy().action().name(),
                data.input().stock().currentPrice(),
                result.scenario() == null ? null : result.scenario().entryFrom(),
                result.scenario() == null ? null : result.scenario().entryTo(),
                result.scenario() == null ? List.of() : result.scenario().targets(),
                result.scenario() == null ? null : result.scenario().stopLoss(),
                data.input().dailyCandles().size(),
                factors,
                result.strategy().reasons(),
                result.warnings(),
                null,
                null,
                stored.request().dataCutoffAt(),
                completedAt,
                result.signalReport(), judgment
        );
        runs.put(runId, new StoredRun(stored.request(), view, data));
    }

    @Override
    public void markFailed(long runId, String errorCode, String errorMessage, Instant failedAt) {
        StoredRun stored = required(runId);
        AnalysisRunView old = stored.view();
        AnalysisRunView failed = new AnalysisRunView(
                old.analysisRunId(), old.stockCode(), old.stockName(), old.analysisMode(),
                AnalysisStatus.FAILED, DataQualityStatus.INVALID, old.baseDate(),
                old.engineVersion(), old.totalScore(), old.scoreGrade(), old.strategyCode(),
                old.strategyName(), old.strategyAction(), old.currentPrice(), old.entryFrom(),
                old.entryTo(), old.targets(), old.stopLoss(), old.historicalSampleCount(),
                old.factors(), old.reasons(), old.warnings(), errorCode, errorMessage,
                old.requestedAt(), failedAt
        );
        runs.put(runId, new StoredRun(stored.request(), failed, stored.data()));
    }

    @Override
    public Optional<AnalysisRunView> findById(long runId) {
        return Optional.ofNullable(runs.get(runId)).map(StoredRun::view);
    }

    @Override
    public Optional<AnalysisRunView> findLatest(String stockCode, AnalysisMode mode) {
        return stream(stockCode, mode).findFirst().map(StoredRun::view);
    }

    @Override
    public Optional<AnalysisRunView> findCompleted(
            String stockCode,
            AnalysisMode mode,
            LocalDate baseDate
    ) {
        return stream(stockCode, mode)
                .filter(run -> run.view().status() == AnalysisStatus.COMPLETED)
                .filter(run -> run.view().baseDate().equals(baseDate))
                .findFirst()
                .map(StoredRun::view);
    }

    @Override
    public List<AnalysisRunView> findHistory(
            String stockCode,
            AnalysisMode mode,
            int offset,
            int size
    ) {
        return stream(stockCode, mode)
                .skip(offset)
                .limit(size)
                .map(StoredRun::view)
                .toList();
    }

    @Override
    public long countHistory(String stockCode, AnalysisMode mode) {
        return stream(stockCode, mode).count();
    }

    @Override
    public List<StockPricePoint> findPrices(
            String stockCode,
            LocalDate from,
            LocalDate to
    ) {
        Map<LocalDate, StockPricePoint> prices = new LinkedHashMap<>();
        stream(stockCode, null)
                .filter(run -> run.data() != null)
                .flatMap(run -> run.data().input().dailyCandles().stream())
                .sorted(Comparator.comparing(DailyCandle::businessDate))
                .filter(candle -> from == null || !candle.businessDate().isBefore(from))
                .filter(candle -> to == null || !candle.businessDate().isAfter(to))
                .forEach(candle -> prices.put(candle.businessDate(), new StockPricePoint(
                        candle.businessDate(), candle.open(), candle.high(), candle.low(),
                        candle.close(), candle.volume()
                )));
        return List.copyOf(prices.values());
    }

    private java.util.stream.Stream<StoredRun> stream(String stockCode, AnalysisMode mode) {
        return runs.entrySet().stream()
                .sorted(Map.Entry.<Long, StoredRun>comparingByKey().reversed())
                .map(Map.Entry::getValue)
                .filter(run -> run.request().stockCode().equals(stockCode))
                .filter(run -> mode == null || run.request().analysisMode() == mode);
    }

    private StoredRun required(long runId) {
        StoredRun run = runs.get(runId);
        if (run == null) {
            throw new IllegalStateException("analysis run not found: " + runId);
        }
        return run;
    }

    private AnalysisRunView runningView(long id, NewAnalysisRun run) {
        return new AnalysisRunView(
                id, run.stockCode(), null, run.analysisMode(), AnalysisStatus.RUNNING,
                DataQualityStatus.PENDING, run.baseDate(), run.engineVersion(), null, null,
                null, null, null, null, null, null, List.of(), null, 0,
                List.of(), List.of(), List.of(), null, null, run.dataCutoffAt(), null
        );
    }

    private String grade(BigDecimal score) {
        if (score == null) return null;
        if (score.compareTo(BigDecimal.valueOf(80)) >= 0) return "A";
        if (score.compareTo(BigDecimal.valueOf(65)) >= 0) return "B";
        if (score.compareTo(BigDecimal.valueOf(50)) >= 0) return "C";
        return "D";
    }

    private record StoredRun(
            NewAnalysisRun request,
            AnalysisRunView view,
            CollectedAnalysisData data
    ) {
    }
}
