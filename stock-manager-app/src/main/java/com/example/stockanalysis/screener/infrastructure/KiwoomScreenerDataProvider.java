package com.example.stockanalysis.screener.infrastructure;

import com.example.stockanalysis.engine.screener.*;
import com.example.stockanalysis.engine.screener.ScreenerTypes.Mode;
import com.example.stockanalysis.kiwoom.client.KiwoomApiException;
import com.example.stockanalysis.kiwoom.tr.api.KiwoomTrExecutionResponse;
import com.example.stockanalysis.kiwoom.tr.application.KiwoomTrExecutionService;
import com.example.stockanalysis.screener.application.ScreenerDataProvider;
import com.example.stockanalysis.screener.config.ScreenerProperties;
import com.example.stockanalysis.screener.domain.ScreenerItem.Stock;
import com.example.stockanalysis.screener.domain.ScreenerRun;
import com.example.stockanalysis.screener.domain.ScreenerRunRequest.Market;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CancellationException;
import org.springframework.stereotype.Component;

@Component
public class KiwoomScreenerDataProvider implements ScreenerDataProvider {
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final KiwoomTrExecutionService client;
    private final ObjectMapper mapper;
    private final ScreenerProperties properties;
    private final Clock clock;
    private final ScreenerStockTypeCatalog stockTypes;
    private long lastCallNanos;

    public KiwoomScreenerDataProvider(KiwoomTrExecutionService client, ObjectMapper mapper, ScreenerProperties properties, Clock clock, ScreenerStockTypeCatalog stockTypes) {
        this.client = client; this.mapper = mapper; this.properties = properties; this.clock = clock; this.stockTypes = stockTypes;
    }

    @Override public List<Stock> universe(Market market) {
        Map<String, Stock> stocks = new TreeMap<>();
        for (Market target : market == Market.ALL ? List.of(Market.KOSPI, Market.KOSDAQ) : List.of(market)) {
            String key = "";
            Set<String> seen = new HashSet<>();
            boolean finished = false;
            for (int page = 0; page < properties.maxPages(); page++) {
                var response = call("ka10099", key, Map.of("mrkt_tp", target == Market.KOSPI ? "0" : "10"));
                JsonNode rows = rows(response, "list");
                for (JsonNode row : rows) {
                    String code = required(row, "code");
                    if (!code.matches("[0-9A-Z]{6}")) throw new IllegalArgumentException("invalid universe stock code");
                    String date = row.path("regDay").asText("");
                    Stock stock = new Stock(code, required(row, "name"), target.name(), row.path("state").asText(""),
                            date.isBlank() ? null : LocalDate.parse(date, DAY), stockTypes.typeOf(code));
                    Stock previous = stocks.putIfAbsent(code, stock);
                    if (previous != null && !previous.equals(stock)) throw new IllegalArgumentException("conflicting universe stock");
                }
                key = next(response, seen);
                if (key.isEmpty()) { finished = true; break; }
            }
            if (!finished) throw new IllegalStateException("UNIVERSE_PAGE_LIMIT");
        }
        if (stocks.isEmpty()) throw new IllegalStateException("EMPTY_UNIVERSE");
        return List.copyOf(stocks.values());
    }

    @Override public void validateStockType(String code) { stockTypes.requireStock(code); }

    @Override public Collected collect(String code, ScreenerRun run) {
        // Also protect explicit codes and pending items from runs created before the ST-only policy.
        stockTypes.requireStock(code);
        List<String> warnings = new ArrayList<>();
        warnings.add("STOCK_TYPE_ST");
        boolean daily = run.request().mode() == Mode.SWING;
        Window primary = chart(code, run, daily);
        List<ScreenerBar> dailyBars = daily ? primary.bars() : List.of();
        if (!primary.complete()) warnings.add("SOURCE_WINDOW_TRUNCATED");
        if (!daily) {
            try {
                Window extra = chart(code, run, true);
                dailyBars = extra.bars();
                if (!extra.complete()) warnings.add("DAILY_SOURCE_WINDOW_TRUNCATED");
            } catch (CancellationException e) { throw e;
            } catch (RuntimeException e) { warnings.add("DAILY_FEATURE_SOURCE_UNAVAILABLE"); }
            warnings.add("MINUTE_TIMESTAMP_TREATED_AS_OPEN");
        }
        warnings.add("DAILY_FINALIZED_AT_NEXT_MIDNIGHT");
        if (run.cutoff().toLocalDate().isBefore(LocalDate.now(clock.withZone(SEOUL)))) warnings.add("RETROSPECTIVE_SOURCE_NOT_POINT_IN_TIME");
        return new Collected(new ScreenerInput(code, run.request().timeframe(), run.cutoff(), primary.bars(), dailyBars), primary.complete(), warnings);
    }

    public record HistoricalBar(LocalDate date, long open, long high, long low, long close,
                                long volume, long turnover, long previousClose) {}

    public List<HistoricalBar> historicalDaily(String code, LocalDate from, LocalDate to) {
        var request = Map.<String, Object>of("stk_cd", code, "base_dt", to.format(DAY), "upd_stkpc_tp", "0");
        var bars = new TreeMap<LocalDate, HistoricalBar>();
        var seen = new HashSet<String>();
        String key = "";
        for (int page = 0; page < properties.maxPages(); page++) {
            var response = call("ka10081", key, request);
            boolean reachedStart = false;
            for (JsonNode row : rows(response, "stk_dt_pole_chart_qry")) {
                var date = LocalDate.parse(required(row, "dt"), DAY);
                if (date.isAfter(to)) throw new IllegalArgumentException("요청 종료일 이후의 키움 일봉");
                if (!date.isAfter(from)) reachedStart = true;
                if (date.isBefore(from)) continue;
                long close = price(row, "cur_prc").longValueExact();
                long diff = number(row, "pred_pre").longValueExact();
                String sign = row.path("pred_pre_sig").asText("");
                if (sign.equals("4") || sign.equals("5")) diff = -Math.abs(diff);
                var bar = new HistoricalBar(date, price(row, "open_pric").longValueExact(),
                        price(row, "high_pric").longValueExact(), price(row, "low_pric").longValueExact(),
                        close, number(row, "trde_qty").longValueExact(),
                        number(row, "trde_prica").movePointRight(6).longValueExact(), close - diff);
                var previous = bars.putIfAbsent(date, bar);
                if (previous != null && !previous.equals(bar)) throw new IllegalArgumentException("키움 일봉 중복 불일치");
            }
            key = next(response, seen);
            if (key.isEmpty() || reachedStart) return List.copyOf(bars.values());
        }
        throw new IllegalStateException("키움 일봉 연속조회 한도를 초과했습니다.");
    }

    private record Window(List<ScreenerBar> bars, boolean complete) {}
    private Window chart(String code, ScreenerRun run, boolean daily) {
        Map<String, Object> request = new HashMap<>();
        request.put("stk_cd", code);
        request.put("upd_stkpc_tp", "1");
        if (daily) request.put("base_dt", run.cutoff().toLocalDate().format(DAY));
        else request.put("tic_scope", String.valueOf(run.request().interval()));
        List<ScreenerBar> collected = new ArrayList<>();
        String key = "";
        Set<String> seen = new HashSet<>();
        for (int page = 0; page < properties.maxPages(); page++) {
            var response = call(daily ? "ka10081" : "ka10080", key, request);
            for (JsonNode row : rows(response, daily ? "stk_dt_pole_chart_qry" : "stk_min_pole_chart_qry")) {
                LocalDateTime open = daily ? LocalDate.parse(required(row, "dt"), DAY).atStartOfDay()
                        : LocalDateTime.parse(required(row, "cntr_tm"), MINUTE);
                LocalDateTime closed = daily ? open.plusDays(1) : open.plusMinutes(run.request().interval());
                if (closed.isAfter(run.cutoff())) continue;
                collected.add(new ScreenerBar(open, closed, price(row, "open_pric"), price(row, "high_pric"),
                        price(row, "low_pric"), price(row, "cur_prc"), number(row, "trde_qty").longValueExact(),
                        daily && !row.path("trde_prica").asText("").isBlank() ? number(row, "trde_prica") : null));
            }
            List<ScreenerBar> normalized = DefaultScreenerEngine.normalize(collected, run.cutoff());
            key = next(response, seen);
            boolean enough = normalized.size() >= run.config().requestedBars() && (!daily ||
                    !normalized.get(0).openTime().toLocalDate().isAfter(run.cutoff().toLocalDate().minusWeeks(52)));
            if (key.isEmpty() || enough) return new Window(trim(normalized, daily, run.config().requestedBars()), true);
            if (normalized.size() > 19000) return new Window(trim(normalized, daily, run.config().requestedBars()), false);
            if (page == properties.maxPages() - 1) return new Window(trim(normalized, daily, run.config().requestedBars()), false);
        }
        throw new IllegalStateException("unreachable");
    }
    private List<ScreenerBar> trim(List<ScreenerBar> bars, boolean daily, int requested) {
        return daily ? bars : List.copyOf(bars.subList(Math.max(0, bars.size() - requested), bars.size()));
    }
    private JsonNode rows(KiwoomTrExecutionResponse response, String name) {
        if (response == null || response.body() == null || response.header() == null) throw new IllegalArgumentException("missing API response");
        JsonNode body = mapper.valueToTree(response.body());
        if (body.has("return_code") && body.path("return_code").asInt(-1) != 0) throw new IllegalArgumentException("API returned failure");
        JsonNode rows = body.path(name);
        if (!rows.isArray()) throw new IllegalArgumentException("missing API rows");
        return rows;
    }
    private String next(KiwoomTrExecutionResponse response, Set<String> seen) {
        if (!"Y".equals(response.header().contYn())) return "";
        String next = response.header().nextKey();
        if (next == null || next.isBlank() || !seen.add(next)) throw new IllegalArgumentException("invalid continuation token");
        return next;
    }
    private static String required(JsonNode row, String field) {
        String value = row.path(field).asText("").trim();
        if (value.isEmpty()) throw new IllegalArgumentException("missing chart field: " + field);
        return value;
    }
    private static BigDecimal number(JsonNode row, String field) { return new BigDecimal(required(row, field).replace(",", "")); }
    private static BigDecimal price(JsonNode row, String field) { return number(row, field).abs(); }
    private synchronized KiwoomTrExecutionResponse call(String api, String key, Map<String, Object> request) {
        for (int attempt = 0; ; attempt++) {
            long delay = properties.minCallIntervalMillis() - (System.nanoTime() - lastCallNanos) / 1_000_000;
            pause(Math.max(0, delay));
            lastCallNanos = System.nanoTime();
            try { return client.executeWithServerAuthorization(api, key.isEmpty() ? "N" : "Y", key, request);
            } catch (KiwoomApiException e) {
                Integer status = e.getExternalStatusCode();
                if (attempt >= properties.maxRetries() || status == null || !(status == 429 || status >= 500 && status < 600)) throw e;
                pause(Math.min(10000, 500L << attempt));
            }
        }
    }
    private static void pause(long millis) {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException("interrupted");
        try { if (millis > 0) Thread.sleep(millis); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new CancellationException("interrupted"); }
    }
}
