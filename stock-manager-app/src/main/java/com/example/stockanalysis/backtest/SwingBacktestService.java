package com.example.stockanalysis.backtest;

import static com.example.stockanalysis.backtest.SwingBacktestModels.*;

import com.example.stockanalysis.analysis.application.port.out.StockChartDataProvider;
import com.example.stockanalysis.analysis.domain.StockPricePeriod;
import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.shared.exception.BusinessException;
import com.example.stockanalysis.shared.exception.ErrorCode;
import com.example.stockanalysis.unified.eod.TradingCalendar;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
class SwingBacktestService {
    private static final Logger log = LoggerFactory.getLogger(SwingBacktestService.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final LocalTime CAPTURE_FROM = LocalTime.of(20, 10);
    private static final int MAX_STOCKS = 10;
    private final TradingCalendar calendar;
    private final StockChartDataProvider charts;
    private final SwingBacktestStore store;
    private final Clock clock;
    private final boolean paperEnabled;
    private final SwingBacktestEngine engine = new SwingBacktestEngine();

    SwingBacktestService(TradingCalendar calendar, StockChartDataProvider charts, SwingBacktestStore store,
                         Clock clock, @Value("${swing.backtest.paper-enabled:false}") boolean paperEnabled) {
        this.calendar = calendar;
        this.charts = charts;
        this.store = store;
        this.clock = clock;
        this.paperEnabled = paperEnabled;
    }

    Result historical(Request request) {
        var settings = validate(request);
        var now = ZonedDateTime.now(clock.withZone(SEOUL));
        var latest = now.toLocalTime().isBefore(CAPTURE_FROM) ? now.toLocalDate().minusDays(1) : now.toLocalDate();
        if (settings.to() == null || settings.to().isBefore(settings.from()) || settings.to().isAfter(latest)
                || settings.to().isAfter(settings.from().plusDays(70))) {
            throw invalid("시작일과 종료일을 확인하세요. 장 마감 데이터가 확정된 기간만 검증할 수 있습니다.");
        }
        var sessions = new ArrayList<LocalDate>();
        for (var date = settings.from(); !date.isAfter(settings.to()); date = date.plusDays(1)) {
            if (day(date).status() == TradingCalendar.Status.OPEN) sessions.add(date);
        }
        if (sessions.isEmpty() || sessions.size() > SESSIONS) {
            throw invalid("과거 검증 기간은 1~25거래일이어야 합니다.");
        }
        var run = run(UUID.randomUUID(), settings, sessions, List.of());
        var end = sessions.get(sessions.size() - 1);
        var history = load(settings.stockCodes(), run.warmup().get(0), end, new HashMap<>());
        if (history.isEmpty()) throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "검증할 일봉을 확보하지 못했습니다.");
        var frame = new Frame(clock.instant(), history);
        var frames = new HashMap<LocalDate, Frame>();
        sessions.forEach(date -> frames.put(date, frame));
        return engine.replay(run, "HISTORICAL", end, frames);
    }

    PaperStatus paperStatus() {
        return new PaperStatus(paperEnabled, paperEnabled ? store.recent() : List.of());
    }

    Result start(UUID id, Request request) {
        requirePaper();
        var settings = validate(request);
        if (settings.to() != null) throw invalid("모의검증은 시작일부터 25거래일 동안 진행합니다.");
        var existing = store.find(id);
        if (existing != null) return existingResult(existing, settings);
        var today = LocalDate.now(clock.withZone(SEOUL));
        if (settings.from().isBefore(today) || settings.from().isAfter(today.plusDays(35))) {
            throw invalid("모의검증 시작일은 오늘부터 35일 안의 거래일이어야 합니다.");
        }
        if (day(settings.from()).status() != TradingCalendar.Status.OPEN) {
            throw invalid("모의검증 시작일을 개장일로 선택하세요.");
        }
        var sessions = nextSessions(settings.from(), SESSIONS);
        var followUp = nextSessions(sessions.get(SESSIONS - 1).plusDays(1), settings.holdingDays() - 1);
        var run = run(id, settings, sessions, followUp);
        try {
            store.insert(run);
        } catch (DuplicateKeyException duplicate) {
            var winner = store.find(id);
            if (winner == null) throw duplicate;
            return existingResult(winner, settings);
        }
        return result(run);
    }

    private Result existingResult(Run existing, Request settings) {
        if (!existing.settings().equals(settings)) {
            throw invalid("이미 다른 조건으로 등록된 모의검증 요청입니다.");
        }
        return result(existing);
    }

    Result get(UUID id) {
        requirePaper();
        var run = store.find(id);
        if (run == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "모의검증을 찾을 수 없습니다.");
        return result(run);
    }

    private Result result(Run run) {
        var frames = store.frames(run.id());
        var today = LocalDate.now(clock.withZone(SEOUL));
        var asOf = frames.containsKey(today) ? today : today.minusDays(1);
        return engine.replay(run, "PAPER", asOf, frames);
    }

    void captureToday() {
        var now = ZonedDateTime.now(clock.withZone(SEOUL));
        if (!paperEnabled || now.toLocalTime().isBefore(CAPTURE_FROM)) return;
        var today = now.toLocalDate();
        var cache = new HashMap<String, List<DailyCandle>>();
        for (var run : store.due(today)) {
            if (!run.sessions().contains(today) && !run.followUp().contains(today)) continue;
            var histories = load(run.settings().stockCodes(), run.warmup().get(0), today, cache);
            // Retry API failures within the capture window. Persist a partial observation on the last pass.
            boolean allCurrent = run.settings().stockCodes().stream().allMatch(code ->
                    histories.getOrDefault(code, List.of()).stream().anyMatch(bar -> bar.businessDate().equals(today)));
            if (!allCurrent && now.toLocalTime().isBefore(LocalTime.of(23, 45))) continue;
            var capturedAt = clock.instant();
            if (!capturedAt.atZone(SEOUL).toLocalDate().equals(today)) return;
            store.capture(run.id(), today, new Frame(capturedAt, histories));
        }
    }

    private Map<String, List<DailyCandle>> load(List<String> codes, LocalDate from, LocalDate to,
                                                Map<String, List<DailyCandle>> cache) {
        var histories = new TreeMap<String, List<DailyCandle>>();
        for (String code : codes) {
            try {
                var bars = cache.computeIfAbsent(code, key -> fetch(key, to));
                var bounded = bars.stream().filter(bar -> !bar.businessDate().isBefore(from)).toList();
                if (!bounded.isEmpty()) histories.put(code, bounded);
            } catch (RuntimeException error) {
                if (Thread.currentThread().isInterrupted()) throw error;
                log.warn("Swing daily data unavailable. stockCode={}, date={}, causeType={}", code, to, error.getClass().getSimpleName());
            }
        }
        return Map.copyOf(histories);
    }

    private List<DailyCandle> fetch(String code, LocalDate to) {
        var dates = new HashSet<LocalDate>();
        return charts.fetch(code, StockPricePeriod.DAY, 1, to).stream()
                .map(point -> new DailyCandle(point.timestamp().atZoneSameInstant(SEOUL).toLocalDate(),
                        point.open(), point.high(), point.low(), point.close(), point.volume()))
                .filter(bar -> !bar.businessDate().isAfter(to))
                .peek(bar -> {
                    if (!dates.add(bar.businessDate())) throw new IllegalArgumentException("Duplicate daily candle");
                }).toList();
    }

    private Run run(UUID id, Request settings, List<LocalDate> sessions, List<LocalDate> followUp) {
        var from = sessions.get(0);
        var warmup = new ArrayList<LocalDate>();
        for (var date = from.minusDays(1); warmup.size() < SwingBreakoutSelector.WARMUP; date = date.minusDays(1)) {
            if (day(date).status() == TradingCalendar.Status.OPEN) warmup.add(date);
        }
        Collections.reverse(warmup);
        return new Run(id, STRATEGY, settings, day(from).version(), List.copyOf(warmup), List.copyOf(sessions),
                List.copyOf(followUp), clock.instant());
    }

    private List<LocalDate> nextSessions(LocalDate from, int count) {
        var sessions = new ArrayList<LocalDate>();
        for (var date = from; sessions.size() < count; date = date.plusDays(1)) {
            if (day(date).status() == TradingCalendar.Status.OPEN) sessions.add(date);
        }
        return List.copyOf(sessions);
    }

    private TradingCalendar.Day day(LocalDate date) {
        var day = calendar.day(date);
        if (day.status() == TradingCalendar.Status.UNKNOWN) {
            throw invalid("거래일 달력에 " + date + " 자료가 없습니다. 달력 범위를 확인하세요.");
        }
        return day;
    }

    private Request validate(Request request) {
        if (request == null || request.from() == null || request.stockCodes() == null
                || request.stockCodes().isEmpty() || request.stockCodes().size() > MAX_STOCKS
                || request.stockCodes().stream().anyMatch(code -> code == null || !code.matches("[0-9A-Z]{6}"))) {
            throw invalid("시작일과 6자리 종목코드 1~10개를 입력하세요.");
        }
        if (request.holdingDays() < 1 || request.holdingDays() > 20 || request.maxPositions() < 1
                || request.maxPositions() > request.stockCodes().size()
                || request.capital() == null || request.capital().scale() > 0
                || request.capital().compareTo(BigDecimal.valueOf(10_000)) < 0
                || request.capital().compareTo(BigDecimal.valueOf(1_000_000_000_000L)) > 0
                || request.feeBps() < 0 || request.feeBps() > 500 || request.slippageBps() < 0 || request.slippageBps() > 500) {
            throw invalid("보유기간, 초기 자금, 동시 보유 한도와 비용 설정을 확인하세요.");
        }
        var codes = request.stockCodes().stream().distinct().sorted().toList();
        if (codes.size() != request.stockCodes().size()) throw invalid("중복된 종목코드를 제거하세요.");
        return new Request(codes, request.from(), request.to(), request.holdingDays(), request.capital().setScale(0),
                request.maxPositions(), request.feeBps(), request.slippageBps());
    }

    private void requirePaper() {
        if (!paperEnabled) throw invalid("25거래일 모의검증이 이 서버에서 활성화되지 않았습니다.");
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_REQUEST, message);
    }
}
