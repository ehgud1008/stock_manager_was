package com.example.stockanalysis.backtest;

import static com.example.stockanalysis.backtest.MarketStudy.*;
import com.example.stockanalysis.engine.model.DailyCandle;
import java.math.*;
import java.time.LocalDate;
import java.util.*;

/** Consumes sessions in ascending order. Only the previous close can generate an opening order. */
final class MarketReplay {
    private static final MathContext MC = MathContext.DECIMAL128;
    private static final int WINDOW = SwingBreakoutSelector.WARMUP + 1;
    private final Spec spec;
    private final Map<String, ArrayDeque<DailyCandle>> windows = new HashMap<>();
    private final Map<String, Holding> holdings = new TreeMap<>();
    private final Set<String> uncertain = new HashSet<>();
    private final List<Day> days = new ArrayList<>();
    private final List<Trade> trades = new ArrayList<>();
    private List<Pick> pending = List.of();
    private BigDecimal cash, priorValue, peak, drawdown = BigDecimal.ZERO;
    private boolean completeValuation = true;
    private int signals, entered, closed, wins;
    private BigDecimal netTotal = BigDecimal.ZERO;
    private BigDecimal grossWins = BigDecimal.ZERO, grossLosses = BigDecimal.ZERO, tradeReturns = BigDecimal.ZERO;
    private final Map<String,Long> lastCloses = new HashMap<>();
    private int losses, writeOffs;
    private record Holding(Pick pick, LocalDate date, BigDecimal price, long qty, BigDecimal cost, int index) {}

    MarketReplay(Spec spec) {
        this.spec = spec;
        cash = BigDecimal.valueOf(spec.capital());
        priorValue = cash;
        peak = cash;
    }

    void session(LocalDate date, Map<String, Quote> rows) {
        boolean active = !date.isBefore(spec.from());
        // An unexplained basis change or disappearance in an open position has no known settlement price.
        for (var iterator = holdings.values().iterator(); iterator.hasNext();) {
            var holding = iterator.next();
            var code = holding.pick().code();
            Quote row = rows.get(code);
            Long previous = lastCloses.get(code);
            if (row != null && !(previous != null && row.volume() > 0 && previous != row.previousClose())) continue;
            if (spec.missingSettlement() == MissingSettlement.WRITE_OFF) {
                settle(holding, date, BigDecimal.ZERO, "WRITE_OFF");
                writeOffs++;
                iterator.remove();
            } else uncertain.add(code);
        }
        if (active) {
            for (Pick pick : pending) enter(pick, date, rows.get(pick.code()));
            sell(date, rows);
        }
        windows.keySet().retainAll(rows.keySet());
        var candidates = new ArrayList<Pick>();
        int eligible = 0;
        for (Quote row : rows.values()) {
            if (row.close() > 0) lastCloses.put(row.code(), row.close());
            var window = windows.computeIfAbsent(row.code(), ignored -> new ArrayDeque<>());
            if (basisChanged(window, row)) window.clear();
            if (row.open() <= 0 || row.volume() == 0) { window.clear(); continue; }
            window.addLast(new DailyCandle(date, bd(row.open()), bd(row.high()), bd(row.low()), bd(row.close()), row.volume()));
            if (window.size() > WINDOW) window.removeFirst();
            if (!active || window.size() != WINDOW || row.turnover() < spec.minTurnover()) continue;
            eligible++;
            var bars = List.copyOf(window);
            var signal = SwingBreakoutSelector.select(row.code(), bars.stream().map(DailyCandle::businessDate).toList(), bars);
            if (signal == null) continue;
            if (spec.strategy() == Strategy.VOLUME_BREAKOUT) {
                double mean = bars.subList(1, WINDOW - 1).stream().mapToDouble(DailyCandle::volume).average().orElseThrow();
                if (row.volume() < 1.5 * mean) continue;
            }
            candidates.add(new Pick(row.code(), row.name(), date, row.close(), signal.breakoutPrice().longValueExact(), row.turnover()));
        }
        if (!active) return;
        candidates.sort(Comparator.comparingLong(Pick::turnover).reversed().thenComparing(Pick::code));
        pending = candidates;
        signals += candidates.size();
        BigDecimal value = cash;
        for (var h : holdings.values()) {
            var row = rows.get(h.pick().code());
            if (uncertain.contains(h.pick().code()) || row == null || row.close() <= 0) { value = null; break; }
            value = value.add(bd(row.close()).multiply(bd(h.qty())));
        }
        if (value == null) completeValuation = false;
        else {
            peak = peak.max(value);
            drawdown = drawdown.max(pct(peak.subtract(value), peak));
        }
        priorValue = value;
        days.add(new Day(date, rows.size(), eligible, candidates.size(), value));
    }

    private boolean basisChanged(Deque<DailyCandle> bars, Quote row) {
        return !bars.isEmpty() && row.previousClose() > 0 && bars.getLast().close().compareTo(bd(row.previousClose())) != 0;
    }

    private void enter(Pick pick, LocalDate date, Quote row) {
        String reason = null;
        if (row == null || row.open() <= 0) reason = "NO_OPEN";
        else if (row.previousClose() != pick.close()) reason = "BASIS_CHANGE";
        else if (row.volume() == 0) reason = "NO_OPEN";
        else if (priorValue == null || !uncertain.isEmpty()) reason = "UNKNOWN_EQUITY";
        else if (holdings.containsKey(pick.code())) reason = "ALREADY_HELD";
        else if (holdings.size() >= spec.positions()) reason = "POSITION_LIMIT";
        if (reason == null) {
            var price = apply(row.open(), spec.slipBps());
            var unit = price.multiply(multiplier(spec.feeBps()));
            var budget = priorValue.divide(bd(spec.positions()), MC).min(cash);
            long qty = budget.divideToIntegralValue(unit).longValueExact();
            if (qty > 0) {
                var cost = unit.multiply(bd(qty));
                cash = cash.subtract(cost);
                holdings.put(pick.code(), new Holding(pick,date,price,qty,cost,days.size()));
                entered++;
                return;
            }
            reason = "INSUFFICIENT_CASH";
        }
        trades.add(skipped(pick, reason));
    }

    private void sell(LocalDate date, Map<String, Quote> rows) {
        var iterator = holdings.values().iterator();
        while (iterator.hasNext()) {
            var h = iterator.next();
            if (days.size() - h.index() + 1 < spec.hold() || uncertain.contains(h.pick().code())) continue;
            var row = rows.get(h.pick().code());
            if (row == null || row.volume() == 0 || row.close() <= 0) continue;
            var price = apply(row.close(), -spec.slipBps());
            settle(h, date, price, "CLOSED");
            iterator.remove();
        }
    }

    private void settle(Holding h, LocalDate date, BigDecimal price, String status) {
        var proceeds = price.multiply(multiplier(-spec.feeBps())).multiply(bd(h.qty()));
        var net = proceeds.subtract(h.cost());
        cash = cash.add(proceeds);
        netTotal = netTotal.add(net);
        tradeReturns = tradeReturns.add(pct(net,h.cost()));
        closed++;
        if (net.signum() > 0) {
            wins++;
            grossWins = grossWins.add(net);
        } else if (net.signum() < 0) {
            losses++;
            grossLosses = grossLosses.add(net.abs());
        }
        trades.add(new Trade(h.pick(), h.date(), h.price(), h.qty(), date, price, net, status));
    }

    Result finish(List<LocalDate> empty, String hash) {
        if (days.isEmpty()) throw invalid("기간 내 시장 거래일 데이터가 없습니다.");
        var all = new ArrayList<>(trades);
        pending.forEach(pick -> all.add(skipped(pick, "WINDOW_ENDED")));
        holdings.values().forEach(h -> all.add(new Trade(h.pick(),h.date(),h.price(),h.qty(),null,null,null,
                uncertain.contains(h.pick().code()) ? "UNRESOLVED" : "OPEN")));
        var metrics = new Metrics(days.size(), signals, entered, closed, wins, closed == 0 ? null : pct(bd(wins), bd(closed)),
                closed == 0 ? null : netTotal.divide(bd(closed), 2, RoundingMode.HALF_UP),
                completeValuation ? pct(priorValue.subtract(bd(spec.capital())), bd(spec.capital())) : null,
                completeValuation ? drawdown : null, holdings.size(), mean(grossWins,wins),
                mean(grossLosses.negate(),losses), grossLosses.signum() == 0 ? null : grossWins.divide(grossLosses,4,RoundingMode.HALF_UP),
                mean(tradeReturns,closed),empty.size(),writeOffs);
        return new Result("KIWOOM_BREAKOUT_1", spec, metrics, List.copyOf(days), all, List.copyOf(empty), hash);
    }

    private static Trade skipped(Pick pick, String reason) { return new Trade(pick,null,null,0,null,null,null,reason); }
    private static BigDecimal bd(long value) { return BigDecimal.valueOf(value); }
    private static BigDecimal mean(BigDecimal value, int count) { return count == 0 ? null : value.divide(bd(count),4,RoundingMode.HALF_UP); }
    private static BigDecimal multiplier(int bps) { return BigDecimal.ONE.add(BigDecimal.valueOf(bps, 4)); }
    private static BigDecimal apply(long price, int bps) { return bd(price).multiply(multiplier(bps)); }
    private static BigDecimal pct(BigDecimal n, BigDecimal d) { return n.movePointRight(2).divide(d, 4, RoundingMode.HALF_UP); }
}
