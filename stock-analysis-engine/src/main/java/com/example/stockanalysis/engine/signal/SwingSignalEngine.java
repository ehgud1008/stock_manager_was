package com.example.stockanalysis.engine.signal;

import com.example.stockanalysis.engine.model.*;
import com.example.stockanalysis.engine.factor.internal.RiskStabilityFactorCalculator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import static com.example.stockanalysis.engine.model.TradeSignal.Side.*;
import static com.example.stockanalysis.engine.model.TradeSignal.State.*;

/** Deterministic event replay over the supplied snapshot. No orders, broker holdings, or backtest dependencies. */
public final class SwingSignalEngine {
    public static final String VERSION = "swing-signals-v2";
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int VALID_BARS = 5;
    private record Candidate(String type, TradeSignal.Side side, BigDecimal trigger, BigDecimal invalidation, String reason) {}

    public SignalReport evaluate(AnalysisInput input, TradeScenario currentScenario) {
        String scope = input.position() == null ? "UNKNOWN" : input.position().held() ? "HELD" : "NOT_HELD";
        List<DailyCandle> bars = input.dailyCandles().stream()
                .filter(b -> !b.businessDate().isAfter(input.dataCutoffAt().atZone(SEOUL).toLocalDate()))
                .sorted(Comparator.comparing(DailyCandle::businessDate)).toList();
        List<String> warnings = new ArrayList<>();
        warnings.add("확정은 규칙 충족을 뜻하며 수익 보장이나 자동 주문이 아닙니다.");
        warnings.add(input.confirmedThrough() == null
                ? "일봉 완료는 다음 한국 날짜 00:00 이후로 보수적으로 판정합니다. 당일 봉은 예비 신호입니다."
                : "장 마감 수집 정책으로 확인한 " + input.confirmedThrough() + "까지의 일봉을 확정 입력으로 사용합니다. 제공사 정정 가능성은 남습니다.");
        if (input.position() == null) warnings.add("보유 정보 미확인: 구조적 매도는 보유 시 조건이며 개인 손절·익절은 판정하지 않습니다.");
        if (bars.stream().map(DailyCandle::businessDate).distinct().count() != bars.size())
            return new SignalReport(VERSION, input.dataCutoffAt(), scope, "INSUFFICIENT_DATA", List.of(),
                    List.of("중복 일봉 날짜가 있어 신호 판정을 보류했습니다."));
        Map<String, TradeSignal> latest = new LinkedHashMap<>();
        int lastClosed = -1;
        for (int i = 0; i < bars.size(); i++) if (closed(bars.get(i), input)) lastClosed = i;
        if (bars.size() < 61) {
            warnings.add("패턴 신호에는 최소 61개 일봉이 필요합니다. 보유 가격 접촉은 별도 판정합니다.");
            if (!bars.isEmpty()) addPositionSignals(input, bars, lastClosed, latest, warnings);
            return new SignalReport(VERSION, input.dataCutoffAt(), scope, "INSUFFICIENT_DATA", List.copyOf(latest.values()), warnings);
        }
        double[] e5 = ema(bars, 5), e20 = ema(bars, 20), e40 = ema(bars, 40);
        for (int i = Math.max(60, bars.size() - 40); i < bars.size(); i++) {
            List<Candidate> candidates = candidates(bars, i, e5, e20, e40);
            Set<String> previous = new HashSet<>();
            candidates(bars, i - 1, e5, e20, e40).forEach(c -> previous.add(c.type()));
            for (Candidate c : candidates) {
                if (previous.contains(c.type())) continue; // A maintained condition is not a new signal every day.
                TradeSignal signal = lifecycle(c, bars, i, lastClosed, input, currentScenario);
                latest.put(c.type() + (signal.state() == PRELIMINARY ? ":PRELIMINARY" : ":CLOSED"), signal);
            }
        }
        addPositionSignals(input, bars, lastClosed, latest, warnings);
        boolean sellActive = latest.values().stream().anyMatch(s -> s.side() == SELL && live(s));
        List<TradeSignal> signals = latest.values().stream().map(s -> {
            if (s.side() != BUY || !live(s) || !sellActive) return s;
            return copy(s, "SELL_SIGNAL_CONFLICT");
        }).sorted(Comparator.comparing(TradeSignal::occurredOn).reversed().thenComparing(TradeSignal::type)).toList();
        return new SignalReport(VERSION, input.dataCutoffAt(), scope, lastClosed >= 60 ? "READY" : "INSUFFICIENT_DATA", signals, warnings);
    }

    private List<Candidate> candidates(List<DailyCandle> b, int i, double[] e5, double[] e20, double[] e40) {
        if (i < 59) return List.of();
        List<Candidate> out = new ArrayList<>();
        DailyCandle now = b.get(i);
        BigDecimal close = now.close(), atr = atr(b, i);
        boolean trend = e20[i] > e40[i] && e20[i] > e20[i - 3];
        BigDecimal resistance = high(b, i - 20, i), support = low(b, i - 20, i);
        boolean volume = n(now.volume()).compareTo(averageVolume(b, i - 20, i).multiply(n(1.5))) >= 0
                && averageVolume(b, i - 20, i).signum() > 0;
        if (trend && close.compareTo(resistance) > 0 && volume && strongClose(now))
            out.add(new Candidate("BREAKOUT", BUY, resistance, resistance.subtract(atr.multiply(n(.5))),
                    "EMA20>EMA40·EMA20 상승, 직전 20봉 저항 종가 돌파, 거래량 1.5배 이상·봉 내 종가 위치 60% 이상"));

        // A real pullback must precede the recovery: an earlier close below EMA5, without breaking the preceding base.
        BigDecimal base = low(b, i - 20, i - 5);
        boolean pullback = false;
        for (int j = i - 5; j < i; j++) pullback |= b.get(j).close().doubleValue() < e5[j];
        boolean intact = true;
        for (int j = i - 5; j <= i; j++) intact &= b.get(j).close().compareTo(base) >= 0;
        BigDecimal recoveryLevel = high(b, i - 3, i);
        if (trend && pullback && intact && close.doubleValue() > e5[i] && e5[i] > e5[i - 1]
                && close.compareTo(recoveryLevel) > 0)
            out.add(new Candidate("PULLBACK_RECOVERY", BUY, recoveryLevel,
                    low(b, i - 5, i).subtract(atr.multiply(n(.25))),
                    "상승 추세 속 실제 조정·기준 지지 유지 후 EMA5 회복 및 직전 3봉 고점 돌파"));

        // Re-test must occur strictly after a confirmed historical breakout, never on the breakout bar itself.
        for (int j = i - 2; j >= Math.max(40, i - 10); j--) {
            BigDecimal level = high(b, j - 20, j);
            boolean original = b.get(j).close().compareTo(level) > 0 && e20[j] > e40[j] && e20[j] > e20[j - 3]
                    && averageVolume(b, j - 20, j).signum() > 0
                    && n(b.get(j).volume()).compareTo(averageVolume(b, j - 20, j).multiply(n(1.2))) >= 0;
            if (!original) continue;
            BigDecimal invalid = level.subtract(atr(b, j).multiply(n(.5)));
            boolean held = true, retested = false;
            for (int k = j + 1; k < i; k++) {
                held &= b.get(k).close().compareTo(invalid) >= 0;
                retested |= b.get(k).low().compareTo(level.add(atr(b, j).multiply(n(.5)))) <= 0;
            }
            if (trend && held && retested && close.compareTo(level) > 0 && close.compareTo(b.get(i - 1).high()) > 0)
                out.add(new Candidate("BREAKOUT_RETEST", BUY, b.get(i - 1).high(), invalid,
                        "확정 돌파 이후 별도 봉에서 재지지 확인, 반등 봉 고점 종가 돌파"));
            break;
        }
        boolean down = e5[i] < e20[i] && e20[i] < e20[i - 3];
        if (down && close.compareTo(support) < 0)
            out.add(new Candidate("TREND_BREAKDOWN", SELL, support, support.add(atr.multiply(n(.5))),
                    "직전 20봉 지지 종가 이탈 및 EMA5<EMA20·EMA20 하락 확인"));
        for (int j = i - 1; j >= Math.max(40, i - 5); j--) {
            BigDecimal level = high(b, j - 20, j);
            boolean validBreakout = b.get(j).close().compareTo(level) > 0 && e20[j] > e40[j]
                    && e20[j] > e20[j - 3] && averageVolume(b, j - 20, j).signum() > 0
                    && n(b.get(j).volume()).compareTo(averageVolume(b, j - 20, j).multiply(n(1.2))) >= 0;
            BigDecimal invalid = level.subtract(atr(b, j).multiply(n(.5)));
            if (validBreakout && close.compareTo(invalid) < 0 && close.doubleValue() < e5[i]) {
                out.add(new Candidate("BREAKOUT_FAILURE", SELL, invalid, level,
                        "최근 확정 돌파의 무효화 가격 아래 종가 마감 및 EMA5 회복 실패"));
                break;
            }
        }
        return out;
    }

    private TradeSignal lifecycle(Candidate c, List<DailyCandle> b, int start, int lastClosed,
                                  AnalysisInput input, TradeScenario scenario) {
        boolean complete = closed(b.get(start), input);
        int age = Math.max(0, lastClosed - start);
        TradeSignal.State state = complete ? age == 0 ? CONFIRMED : ACTIVE : PRELIMINARY;
        LocalDate ended = null;
        if (complete) for (int j = start + 1; j <= Math.min(lastClosed, start + VALID_BARS); j++) {
            boolean invalid = c.side() == BUY ? b.get(j).close().compareTo(c.invalidation()) < 0
                    : b.get(j).close().compareTo(c.invalidation()) > 0;
            if (invalid) { state = INVALIDATED; ended = b.get(j).businessDate(); break; }
        }
        if (complete && state != INVALIDATED && age >= VALID_BARS) { state = EXPIRED; ended = b.get(start + VALID_BARS).businessDate(); }
        BigDecimal entryFrom = c.side() == BUY ? b.get(start).close().subtract(atr(b, start).multiply(n(.25))).max(n(.01)) : null;
        BigDecimal entryTo = c.side() == BUY ? b.get(start).close().add(atr(b, start).multiply(n(.5))) : null;
        String entry = "NOT_APPLICABLE";
        if (c.side() == BUY) {
            entry = !complete ? "AWAIT_CLOSE" : state == INVALIDATED || state == EXPIRED ? "INACTIVE"
                    : scenario == null ? "SCENARIO_UNAVAILABLE"
                    : input.stock().currentPrice().compareTo(entryTo) > 0 ? "CHASE_BLOCKED"
                    : input.stock().currentPrice().compareTo(entryFrom) < 0 ? "OUTSIDE_ENTRY_ZONE" : "IN_RANGE";
        }
        return new TradeSignal(c.type() + ":" + b.get(start).businessDate(), c.type(), c.side(), state,
                b.get(start).businessDate(), b.get(b.size() - 1).businessDate(), age, VALID_BARS,
                c.trigger(), c.invalidation(), entryFrom, entryTo, entry, ended, List.of(c.reason()));
    }

    private void addPositionSignals(AnalysisInput input, List<DailyCandle> bars, int lastClosed,
                                     Map<String, TradeSignal> signals, List<String> warnings) {
        PositionContext p = input.position();
        if (p == null || !p.held()) return;
        if (p.openedAt().atZone(SEOUL).toLocalDate().isBefore(bars.get(0).businessDate()))
            warnings.add("진입일 이후 일부 일봉이 조회 범위 밖입니다. 조회 이전의 손절·목표가 접촉은 확인하지 못했습니다.");
        if (p.stopPrice() == null) warnings.add("보유 포지션의 손절가가 없어 개인 손절 신호는 판정하지 않습니다.");
        boolean stopTriggered = false;
        Set<BigDecimal> reachedTargets = new HashSet<>();
        for (int i = 0; i < bars.size(); i++) {
            DailyCandle bar = bars.get(i);
            // Daily extremes cannot tell whether a same-day touch occurred before or after the position was opened.
            if (!bar.businessDate().isAfter(p.openedAt().atZone(SEOUL).toLocalDate())) continue;
            boolean complete = i <= lastClosed;
            boolean hit = p.stopPrice() != null && (p.stopPolicy() == PositionContext.StopPolicy.TOUCH
                    ? bar.low().compareTo(p.stopPrice()) <= 0 : bar.close().compareTo(p.stopPrice()) <= 0);
            if (hit && !stopTriggered) {
                // A TOUCH on a day's recorded low is an observed historical event, not an execution guarantee.
                TradeSignal.State state = complete || p.stopPolicy() == PositionContext.StopPolicy.TOUCH ? CONFIRMED : PRELIMINARY;
                signals.put("POSITION_STOP", new TradeSignal("POSITION_STOP:" + bar.businessDate(), "POSITION_STOP", SELL,
                        state, bar.businessDate(), bars.get(bars.size() - 1).businessDate(), Math.max(0, lastClosed - i), 0,
                        p.stopPrice(), null, null, null, "NOT_APPLICABLE", null,
                        List.of("사용자 지정 손절가 " + p.stopPrice() + " / 정책 " + p.stopPolicy() + "; 체결 여부는 확인하지 않음")));
                if (state == CONFIRMED) stopTriggered = true;
            }
            if (stopTriggered) break; // Stop dominates same-bar target ambiguity; do not assume profitable execution first.
            for (BigDecimal target : p.targets()) if (!reachedTargets.contains(target) && bar.high().compareTo(target) >= 0) {
                signals.put("TARGET:" + target, new TradeSignal("TARGET:" + target + ":" + bar.businessDate(), "TARGET_REACHED", TAKE_PROFIT,
                        CONFIRMED, bar.businessDate(), bars.get(bars.size() - 1).businessDate(), Math.max(0, lastClosed - i), 0,
                        target, null, null, null, "NOT_APPLICABLE", null, List.of("사용자 지정 목표가 접촉 관측; 하락 신호·체결 확인과 구분")));
                reachedTargets.add(target);
            }
        }
        warnings.add("진입 당일 일봉의 고가·저가는 진입 전후를 구분할 수 없어 개인 손절·익절 판정에서 제외합니다.");
    }

    private boolean live(TradeSignal s) { return s.state() == CONFIRMED || s.state() == ACTIVE; }
    private boolean strongClose(DailyCandle bar) {
        BigDecimal range=bar.high().subtract(bar.low());
        return range.signum()>0 && bar.close().subtract(bar.low()).compareTo(range.multiply(n(.6)))>=0;
    }
    private TradeSignal copy(TradeSignal s, String entry) {
        return new TradeSignal(s.id(), s.type(), s.side(), s.state(), s.occurredOn(), s.evaluatedOn(), s.ageBars(),
                s.validForBars(), s.triggerPrice(), s.invalidationPrice(), s.entryFrom(), s.entryTo(), entry, s.endedOn(), s.reasons());
    }
    private boolean closed(DailyCandle b, AnalysisInput input) {
        if (input.confirmedThrough() != null) return !b.businessDate().isAfter(input.confirmedThrough());
        return !b.businessDate().plusDays(1).atStartOfDay(SEOUL).toInstant().isAfter(input.dataCutoffAt());
    }
    private BigDecimal atr(List<DailyCandle> b, int i) { return RiskStabilityFactorCalculator.averageTrueRange(b.subList(0, i + 1), 14).max(n(.01)); }
    private BigDecimal high(List<DailyCandle> b, int from, int to) { return b.subList(from, to).stream().map(DailyCandle::high).max(BigDecimal::compareTo).orElseThrow(); }
    private BigDecimal low(List<DailyCandle> b, int from, int to) { return b.subList(from, to).stream().map(DailyCandle::low).min(BigDecimal::compareTo).orElseThrow(); }
    private BigDecimal averageVolume(List<DailyCandle> b, int from, int to) {
        return b.subList(from, to).stream().map(c -> n(c.volume())).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(n(to - from), 8, RoundingMode.HALF_UP);
    }
    private double[] ema(List<DailyCandle> b, int period) {
        double[] values = new double[b.size()]; values[0] = b.get(0).close().doubleValue();
        double alpha = 2.0 / (period + 1);
        for (int i = 1; i < b.size(); i++) values[i] = alpha * b.get(i).close().doubleValue() + (1 - alpha) * values[i - 1];
        return values;
    }
    private static BigDecimal n(double value) { return BigDecimal.valueOf(value); }
    private static BigDecimal n(long value) { return BigDecimal.valueOf(value); }
}
