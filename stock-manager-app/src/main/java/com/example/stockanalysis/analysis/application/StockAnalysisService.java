package com.example.stockanalysis.analysis.application;

import com.example.stockanalysis.analysis.application.model.CollectedAnalysisData;
import com.example.stockanalysis.analysis.application.model.NewAnalysisRun;
import com.example.stockanalysis.analysis.application.port.out.AnalysisDataProvider;
import com.example.stockanalysis.analysis.application.port.out.AnalysisStore;
import com.example.stockanalysis.analysis.application.port.out.StockChartDataProvider;
import com.example.stockanalysis.analysis.config.StockAnalysisEngineConfig;
import com.example.stockanalysis.analysis.domain.AnalysisRunView;
import com.example.stockanalysis.analysis.domain.StockChartPoint;
import com.example.stockanalysis.analysis.domain.StockPricePeriod;
import com.example.stockanalysis.engine.api.StockAnalysisEngine;
import com.example.stockanalysis.engine.model.AnalysisMode;
import com.example.stockanalysis.engine.model.AnalysisResult;
import com.example.stockanalysis.shared.exception.BusinessException;
import com.example.stockanalysis.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class StockAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(StockAnalysisService.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<Integer> MINUTE_INTERVALS = Set.of(1, 3, 5, 10, 15, 30, 45, 60);

    private final AnalysisDataProvider dataProvider;
    private final AnalysisStore store;
    private final StockChartDataProvider chartDataProvider;
    private final StockAnalysisEngine engine;
    private final Clock clock;

    public StockAnalysisService(
            AnalysisDataProvider dataProvider,
            AnalysisStore store,
            StockChartDataProvider chartDataProvider,
            StockAnalysisEngine engine,
            Clock clock
    ) {
        this.dataProvider = dataProvider;
        this.store = store;
        this.chartDataProvider = chartDataProvider;
        this.engine = engine;
        this.clock = clock;
    }

    private com.example.stockanalysis.holdings.HoldingService holdings;
    @org.springframework.beans.factory.annotation.Autowired
    public void setHoldings(com.example.stockanalysis.holdings.HoldingService holdings) {this.holdings=holdings;}
    private AnalysisRunView withHolding(AnalysisRunView view) {
        return holdings==null?view:view.withHolding(holdings.evaluation("STOCK:"+view.analysisRunId()));
    }

    public AnalysisRunView execute(String stockCode, AnalysisExecutionCommand command) {
        var holdingSnapshot=holdings==null?null:holdings.single(stockCode);
        LocalDate baseDate = command.baseDate() == null
                ? LocalDate.now(clock.withZone(SEOUL))
                : command.baseDate();
        validateBaseDate(baseDate);
        if (command.position() != null && command.analysisMode() != AnalysisMode.SWING) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "보유 포지션 신호는 SWING 모드에서만 지원합니다.");
        }
        if (command.position() != null && command.position().held()
                && (command.position().openedAt().isAfter(clock.instant())
                || command.position().openedAt().atZone(SEOUL).toLocalDate().isAfter(baseDate))) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "진입 시각은 현재 시각 및 분석 기준일 이후일 수 없습니다.");
        }

        // Live signal lifecycle and user-specific positions must not reuse a same-day snapshot.
        if (!command.forceRecalculate() && command.analysisMode() != AnalysisMode.SWING && command.position() == null && holdings == null) {
            var existing = store.findCompleted(stockCode, command.analysisMode(), baseDate);
            if (existing.isPresent()
                    && StockAnalysisEngineConfig.engineVersion(command.analysisMode()).equals(
                            existing.orElseThrow().engineVersion()
                    )) {
                return existing.get();
            }
        }

        Instant startedAt = clock.instant();
        long runId = store.createRun(new NewAnalysisRun(
                UUID.randomUUID().toString(),
                stockCode,
                command.analysisMode(),
                baseDate,
                StockAnalysisEngineConfig.engineVersion(command.analysisMode()),
                StockAnalysisEngineConfig.configVersion(command.analysisMode()),
                startedAt,
                command.forceRecalculate(),
                command.requestedBy()
        ));

        try (MDC.MDCCloseable ignored = MDC.putCloseable("jobId", Long.toString(runId))) {
            log.info("종목분석 실행 시작. runId={}, stockCode={}, mode={}, baseDate={}",
                    runId, stockCode, command.analysisMode(), baseDate);
            try {
                CollectedAnalysisData collected = dataProvider.collect(
                        stockCode,
                        command.analysisMode(),
                        baseDate
                );
                var position = command.position();
                if (command.analysisMode() == AnalysisMode.SWING && holdingSnapshot != null
                        && "AVAILABLE".equals(holdingSnapshot.status())) {
                    var registered = holdingSnapshot.items().get(stockCode);
                    if (registered == null || registered.quantity().signum() == 0)
                        position = new com.example.stockanalysis.engine.model.PositionContext(false, null, null, null, List.of(), null);
                }
                if (position != null) {
                    var source = collected.input();
                    var input = new com.example.stockanalysis.engine.model.AnalysisInput(source.stock(), source.mode(),
                            source.baseDate(), source.dataCutoffAt(), source.dailyCandles(), source.weeklyCandles(),
                            source.monthlyCandles(), source.observations(), position, source.confirmedThrough());
                    collected = new CollectedAnalysisData(input, collected.sources(), collected.warnings(), collected.dataQualityStatus());
                }
                long engineStarted = System.nanoTime();
                AnalysisResult calculated = engine.analyze(collected.input());
                double engineMs = (System.nanoTime() - engineStarted) / 1_000_000.0;
                AnalysisResult result = new AnalysisResult(
                        calculated.engineVersion(),
                        calculated.mode(),
                        calculated.totalScore(),
                        calculated.factors(),
                        calculated.strategy(),
                        calculated.scenario(),
                        java.util.stream.Stream.concat(collected.warnings().stream(), calculated.warnings().stream())
                                .distinct().toList(),
                        calculated.signalReport()
                );
                CollectedAnalysisData completedData = new CollectedAnalysisData(collected.input(), collected.sources(),
                        result.warnings(), !com.example.stockanalysis.analysis.domain.DataQualityStatus.hasBlockingWarnings(result.mode(), result.warnings()) ? collected.dataQualityStatus()
                        : com.example.stockanalysis.analysis.domain.DataQualityStatus.PARTIAL);
                com.example.stockanalysis.judgment.TradeJudgment judgment = null;
                if (result.mode() == AnalysisMode.SWING) {
                    var input = collected.input();
                    var priceBar = input.dailyCandles().stream().max(java.util.Comparator.comparing(
                            com.example.stockanalysis.engine.model.DailyCandle::businessDate)).orElseThrow();
                    boolean confirmed = input.confirmedThrough() != null
                            ? !priceBar.businessDate().isAfter(input.confirmedThrough())
                            : !priceBar.businessDate().plusDays(1).atStartOfDay(SEOUL).toInstant().isAfter(input.dataCutoffAt());
                    // Do not relabel a live quote or an unfinished daily candle as a confirmed close.
                    boolean samePrice = priceBar.close().compareTo(input.stock().currentPrice()) == 0;
                    judgment = new com.example.stockanalysis.judgment.TradeJudgmentEngine().evaluate(
                            new com.example.stockanalysis.judgment.TradeJudgmentEngine.Input(result, input.stock().currentPrice(),
                                    priceBar.businessDate(), input.dataCutoffAt(),
                                    confirmed && samePrice ? collected.dataQualityStatus().name() : "PARTIAL"));
                }
                store.saveCompleted(runId, completedData, result, clock.instant(), judgment);
                com.example.stockanalysis.holdings.HoldingModels.Evaluation holdingEvaluation=null;
                if(holdingSnapshot!=null) {
                    var scenario=result.scenario();
                    var input=collected.input();
                    var priceBar=input.dailyCandles().stream().filter(b->!b.businessDate().isAfter(baseDate))
                        .filter(b->input.confirmedThrough()!=null?!b.businessDate().isAfter(input.confirmedThrough()):!b.businessDate().plusDays(1).atStartOfDay(SEOUL).toInstant().isAfter(input.dataCutoffAt()))
                        .max(java.util.Comparator.comparing(com.example.stockanalysis.engine.model.DailyCandle::businessDate)).orElse(null);
                    var priceDate=priceBar==null?null:priceBar.businessDate();
                    holdingEvaluation=com.example.stockanalysis.holdings.HoldingModels.evaluate(holdingSnapshot,stockCode,
                            priceBar==null?null:priceBar.close(),priceDate,scenario==null?null:scenario.stopLoss(),scenario==null?java.util.List.of():scenario.targets(),judgment);
                    if(!"UNAVAILABLE".equals(holdingSnapshot.status())) holdings.save("STOCK:"+runId,holdingEvaluation);
                }
                log.info("종목분석 실행 완료. runId={}, stockCode={}, score={}, action={}, engineMs={}",
                        runId, stockCode, result.totalScore(), result.strategy().action(), engineMs);
                return store.findById(runId).orElseThrow().withHolding(holdingEvaluation);
            } catch (RuntimeException exception) {
                ErrorCode errorCode = exception instanceof BusinessException businessException
                        ? businessException.getErrorCode()
                        : ErrorCode.INTERNAL_SERVER_ERROR;
                store.markFailed(
                        runId,
                        errorCode.name(),
                        safeMessage(exception),
                        clock.instant()
                );
                log.warn("종목분석 실행 실패. runId={}, errorCode={}, causeType={}",
                        runId, errorCode, exception.getClass().getSimpleName());
                throw exception;
            }
        }
    }

    public AnalysisRunView getRun(long runId) {
        return store.findById(runId).map(this::withHolding).orElseThrow(() -> new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                "종목분석 실행 결과를 찾을 수 없습니다."
        ));
    }

    public AnalysisRunView getLatest(String stockCode, AnalysisMode mode) {
        return store.findLatest(stockCode, mode).map(this::withHolding).orElseThrow(() -> new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                "해당 종목의 분석 결과를 찾을 수 없습니다."
        ));
    }

    public AnalysisHistoryPage getHistory(
            String stockCode,
            AnalysisMode mode,
            int page,
            int size
    ) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "page는 0 이상, size는 1~100이어야 합니다."
            );
        }
        long total = store.countHistory(stockCode, mode);
        List<AnalysisRunView> content = store.findHistory(stockCode, mode, page * size, size);
        int totalPages = total == 0 ? 0 : (int) ((total + size - 1) / size);
        return new AnalysisHistoryPage(content, page, size, total, totalPages);
    }

    public List<StockChartPoint> getPrices(
            String stockCode,
            String period,
            int minuteInterval,
            LocalDate from,
            LocalDate to
    ) {
        StockPricePeriod pricePeriod = StockPricePeriod.from(period);
        if (pricePeriod == StockPricePeriod.MINUTE
                && !MINUTE_INTERVALS.contains(minuteInterval)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "분봉 간격은 1, 3, 5, 10, 15, 30, 45, 60분 중 하나여야 합니다."
            );
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "from은 to보다 늦을 수 없습니다."
            );
        }
        LocalDate baseDate = to == null ? LocalDate.now(clock.withZone(SEOUL)) : to;
        validateBaseDate(baseDate);
        return chartDataProvider.fetch(stockCode, pricePeriod, minuteInterval, baseDate).stream()
                .filter(point -> from == null || !point.timestamp().toLocalDate().isBefore(from))
                .filter(point -> to == null || !point.timestamp().toLocalDate().isAfter(to))
                .toList();
    }

    private void validateBaseDate(LocalDate baseDate) {
        if (baseDate.isAfter(LocalDate.now(clock.withZone(SEOUL)))) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "기준일은 오늘보다 늦을 수 없습니다."
            );
        }
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 1900 ? message.substring(0, 1900) : message;
    }
}
