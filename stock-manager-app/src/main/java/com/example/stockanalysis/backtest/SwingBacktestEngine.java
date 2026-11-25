package com.example.stockanalysis.backtest;

import static com.example.stockanalysis.backtest.SwingBacktestModels.*;

import com.example.stockanalysis.engine.model.DailyCandle;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

final class SwingBacktestEngine {
    private static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal ONE = BigDecimal.ONE;

    private record Position(Signal signal, LocalDate entryDate, BigDecimal price, long quantity,
                            BigDecimal cost, int entryIndex) {}

    Result replay(Run run, String mode, LocalDate asOf, Map<LocalDate, Frame> frames) {
        if (!STRATEGY.equals(run.strategy())) throw new IllegalArgumentException("Unsupported swing strategy");
        var dates = new ArrayList<>(run.warmup());
        dates.addAll(run.sessions());
        dates.addAll(run.followUp());
        var settings = run.settings();
        var fee = BigDecimal.valueOf(settings.feeBps(), 4);
        var slip = BigDecimal.valueOf(settings.slippageBps(), 4);
        var cash = settings.capital();
        var trades = new ArrayList<Trade>();
        var followUpTrades = new ArrayList<Trade>();
        var positions = new TreeMap<String, Position>();
        var brokenPriceBasis = new HashSet<String>();
        var lastObserved = new HashMap<String, DailyCandle>();
        var equity = new ArrayList<Equity>();
        var coverage = new ArrayList<Coverage>();
        var pending = new ArrayList<Signal>();
        int captured = 0;
        int settled = 0;
        int begin = run.warmup().size();
        int end = begin + run.sessions().size();
        for (int i = begin; i < dates.size() && !dates.get(i).isAfter(asOf); i++) {
            var date = dates.get(i);
            var frame = frames.get(date);
            var windowDates = dates.subList(i - SwingBreakoutSelector.WARMUP, i + 1);
            var byCode = new TreeMap<String, Map<LocalDate, DailyCandle>>();
            if (frame != null) {
                frame.histories().forEach((code, bars) -> {
                    var index = index(bars);
                    var previous = lastObserved.get(code);
                    if (previous != null && !sameBar(previous, index.get(previous.businessDate()))) {
                        brokenPriceBasis.add(code);
                    }
                    if (!brokenPriceBasis.contains(code)) {
                        byCode.put(code, index);
                        var current = index.get(date);
                        if (current != null) lastObserved.put(code, current);
                    }
                });
            }

            // Size at the prior close. Today's closing prices and sale proceeds cannot fund the opening trades.
            BigDecimal priorEquity = equity.isEmpty() ? cash : equity.get(equity.size() - 1).value();
            for (var signal : pending) {
                var bar = byCode.getOrDefault(signal.stockCode(), Map.of()).get(date);
                var price = bar == null ? null : bar.open().multiply(ONE.add(slip), MC);
                var unitCost = price == null ? null : price.multiply(ONE.add(fee), MC);
                if (bar == null || priorEquity == null) {
                    trades.add(unfilled(signal, "DATA_GAP"));
                } else if (bar.volume() == 0) {
                    trades.add(unfilled(signal, "NO_FILL"));
                } else if (positions.containsKey(signal.stockCode())) {
                    trades.add(unfilled(signal, "ALREADY_HELD"));
                } else if (positions.size() >= settings.maxPositions()) {
                    trades.add(unfilled(signal, "POSITION_LIMIT"));
                } else {
                    var budget = priorEquity.divide(BigDecimal.valueOf(settings.maxPositions()), MC).min(cash);
                    long quantity = budget.divideToIntegralValue(unitCost).longValueExact();
                    if (quantity == 0) {
                        trades.add(unfilled(signal, "INSUFFICIENT_CASH"));
                    } else {
                        var cost = unitCost.multiply(BigDecimal.valueOf(quantity), MC);
                        positions.put(signal.stockCode(), new Position(signal, date, price, quantity, cost, i));
                        cash = cash.subtract(cost);
                    }
                }
            }
            pending.clear();

            var positionIterator = positions.values().iterator();
            while (positionIterator.hasNext()) {
                var position = positionIterator.next();
                var bar = byCode.getOrDefault(position.signal().stockCode(), Map.of()).get(date);
                int held = i - position.entryIndex() + 1;
                if (held < settings.holdingDays() || bar == null || bar.volume() == 0) continue;
                var price = bar.close().multiply(ONE.subtract(slip), MC);
                var proceeds = price.multiply(BigDecimal.valueOf(position.quantity()), MC).multiply(ONE.subtract(fee), MC);
                cash = cash.add(proceeds);
                var trade = new Trade(position.signal(), position.entryDate(), position.price(), position.quantity(),
                        date, price, proceeds.subtract(position.cost()), held == settings.holdingDays() ? "CLOSED" : "CLOSED_LATE");
                (i < end ? trades : followUpTrades).add(trade);
                positionIterator.remove();
            }
            if (i >= end) continue;

            settled++;
            boolean observed = byCode.values().stream().anyMatch(bars -> bars.containsKey(date));
            if (observed) captured++;
            var unavailable = new ArrayList<String>();
            for (var code : settings.stockCodes()) {
                var index = byCode.getOrDefault(code, Map.of());
                var window = windowDates.stream().map(index::get).toList();
                if (window.contains(null)) {
                    unavailable.add(code);
                    continue;
                }
                var signal = SwingBreakoutSelector.select(code, windowDates, window);
                if (signal != null) {
                    if (i == end - 1) trades.add(unfilled(signal, "WINDOW_ENDED"));
                    else pending.add(signal);
                }
            }
            coverage.add(new Coverage(date, !observed ? "MISSING" : unavailable.isEmpty() ? "COMPLETE" : "PARTIAL",
                    List.copyOf(unavailable)));
            BigDecimal value = observed ? cash : null;
            for (var position : positions.values()) {
                var bar = byCode.getOrDefault(position.signal().stockCode(), Map.of()).get(date);
                if (value == null || bar == null) {
                    value = null;
                    break;
                }
                value = value.add(bar.close().multiply(BigDecimal.valueOf(position.quantity()), MC));
            }
            equity.add(new Equity(date, value));
            if (i == end - 1) {
                for (var position : positions.values()) trades.add(open(position));
            }
        }
        if (settled < run.sessions().size()) {
            for (var position : positions.values()) trades.add(open(position));
        }
        for (var signal : pending) trades.add(unfilled(signal, "PENDING_ENTRY"));
        trades.sort(Comparator.comparing((Trade trade) -> trade.signal().date()).thenComparing(trade -> trade.signal().stockCode()));
        var summary = summarize(settings.capital(), equity, trades);
        String status = settled < run.sessions().size() ? "RUNNING"
                : coverage.stream().anyMatch(day -> !"COMPLETE".equals(day.status())) ? "PARTIAL" : "COMPLETE";
        return new Result(run, mode, status, asOf, settled, captured, summary, List.copyOf(equity), List.copyOf(trades),
                List.copyOf(followUpTrades), List.copyOf(coverage));
    }

    private Map<LocalDate, DailyCandle> index(List<DailyCandle> candles) {
        var indexed = new HashMap<LocalDate, DailyCandle>();
        for (var candle : candles) {
            if (indexed.putIfAbsent(candle.businessDate(), candle) != null) {
                throw new IllegalArgumentException("Duplicate daily candle");
            }
        }
        return indexed;
    }

    private boolean sameBar(DailyCandle a, DailyCandle b) {
        return b != null && a.open().compareTo(b.open()) == 0 && a.high().compareTo(b.high()) == 0
                && a.low().compareTo(b.low()) == 0 && a.close().compareTo(b.close()) == 0 && a.volume() == b.volume();
    }

    private Trade open(Position position) {
        return new Trade(position.signal(), position.entryDate(), position.price(), position.quantity(),
                null, null, null, "OPEN");
    }

    private Trade unfilled(Signal signal, String status) {
        return new Trade(signal, null, null, null, null, null, null, status);
    }

    private Summary summarize(BigDecimal capital, List<Equity> equity, List<Trade> trades) {
        var closed = trades.stream().filter(t -> t.netPnl() != null).toList();
        int entered = (int) trades.stream().filter(t -> t.entryDate() != null).count();
        int wins = (int) closed.stream().filter(t -> t.netPnl().signum() > 0).count();
        var total = closed.stream().map(Trade::netPnl).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal returnPercent = null;
        BigDecimal drawdown = null;
        if (!equity.isEmpty() && equity.stream().allMatch(point -> point.value() != null)) {
            var peak = capital;
            drawdown = BigDecimal.ZERO;
            for (var point : equity) {
                peak = peak.max(point.value());
                drawdown = drawdown.max(percent(peak.subtract(point.value()), peak));
            }
            returnPercent = percent(equity.get(equity.size() - 1).value().subtract(capital), capital);
        }
        return new Summary(trades.size(), entered, closed.size(), wins, entered - closed.size(),
                closed.isEmpty() ? null : percent(BigDecimal.valueOf(wins), BigDecimal.valueOf(closed.size())),
                closed.isEmpty() ? null : total.divide(BigDecimal.valueOf(closed.size()), 2, RoundingMode.HALF_UP),
                returnPercent, drawdown);
    }

    private BigDecimal percent(BigDecimal amount, BigDecimal base) {
        return amount.movePointRight(2).divide(base, 4, RoundingMode.HALF_UP);
    }
}
