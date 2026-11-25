package com.example.stockanalysis.judgment;

import com.example.stockanalysis.engine.model.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static com.example.stockanalysis.judgment.TradeJudgment.Action.*;

/** Pure, bounded post-processing. It never collects data, recalculates indicators or places orders. */
public final class TradeJudgmentEngine {
    public static final String VERSION = "trade-judgment-v2/" + NewEntryEvaluator.VERSION;
    private static final BigDecimal HOLD_TREND = new BigDecimal("60");
    private static final BigDecimal STRONG_REWARD_RISK = new BigDecimal("2");
    public record Input(AnalysisResult analysis, BigDecimal price, LocalDate priceDate, Instant asOf,
                        String collectionQuality) {
        public Input { Objects.requireNonNull(analysis); Objects.requireNonNull(asOf); }
    }

    public TradeJudgment evaluate(Input in) {
        var a = in.analysis();
        var assessment = new NewEntryEvaluator().evaluate(a, in.price(), in.priceDate(), in.asOf(), in.collectionQuality());
        var report = a.signalReport();
        String scope = report == null ? "UNKNOWN" : report.positionScope();
        var limitations = new ArrayList<>(List.of("규칙 충족 수준이며 상승 확률이나 체결 결과가 아닙니다. 비용·계좌 비중은 반영하지 않습니다.",
                "분석 시점 스냅샷입니다. 다음 거래 시 가격과 신호를 다시 확인하세요."));
        if (!"HELD".equals(scope)) limitations.add("보유 시 판단은 조건부입니다. 개인 손절·익절은 보유 정보 입력 시에만 반영합니다.");
        var defer = decision(DEFER, "판단에 필요한 분석 자료를 확인할 수 없습니다.", "자료를 보완한 뒤 새 분석 실행");
        if (!NewEntryEvaluator.validContext(a, in.price(), in.priceDate(), in.asOf(), in.collectionQuality())) {
            return result(in, scope, decision(DEFER, "가격·신호 기준 시점 또는 데이터 유효성을 확인해야 합니다.", "동일 기준의 최신 자료로 재분석"), defer, limitations, assessment);
        }
        var live = report.signals().stream().filter(s -> s.state() == TradeSignal.State.CONFIRMED || s.state() == TradeSignal.State.ACTIVE).toList();
        // Personal events belong only to the supplied holding. They must not contaminate the hypothetical new entry.
        boolean stop = "HELD".equals(scope) && live.stream().anyMatch(s -> "POSITION_STOP".equals(s.type()));
        boolean target = "HELD".equals(scope) && live.stream().anyMatch(s -> "TARGET_REACHED".equals(s.type()));
        boolean sell = live.stream().anyMatch(s -> s.side() == TradeSignal.Side.SELL && !"POSITION_STOP".equals(s.type()));
        TradeJudgment.Decision holder = stop ? decision(EXIT_TRIGGERED, "입력한 보유 포지션의 손절 조건이 충족됐습니다. 체결 여부는 확인되지 않았습니다.", "실제 잔고·체결 확인 후 보유 정보 갱신")
                : sell ? decision(SELL_PRIORITY, "유효한 추세 훼손 또는 돌파 실패 신호가 있습니다.", "매도 신호 무효화 가격 회복 또는 신호 만료 시 재평가")
                : target ? decision(TAKE_PROFIT_REVIEW, "입력한 목표가 접촉이 관측됐습니다. 전량 매도나 하락 전망을 뜻하지 않습니다.", "정한 익절 계획과 실제 체결·잔고 확인") : null;
        if (!"COMPLETE".equals(in.collectionQuality())) limitations.add("수집 자료가 완전하지 않아 신규 매수·보유 유지 판단을 보류합니다. 확인된 청산 사건은 별도 표시합니다.");
        if (!"READY".equals(report.completeness())) limitations.add("패턴 신호 표본이 부족합니다.");
        boolean ready = "READY".equals(report.completeness()) && a.totalScore() != null
                && "COMPLETE".equals(in.collectionQuality());
        BigDecimal trend = a.factors().stream().filter(f -> "TREND".equals(f.factorCode())).map(FactorResult::score)
                .filter(Objects::nonNull).findFirst().orElse(null);
        if (holder == null) holder = !ready || trend == null ? defer
                : trend.compareTo(HOLD_TREND) >= 0 ? decision(HOLD, "추세 점수가 유지 기준 60점 이상이며 유효한 청산 신호가 없습니다. 추가 매수 판단은 별도입니다.", "손절·매도 신호 발생 또는 추세 점수 60점 미만 시 재평가")
                : decision(REVIEW_HOLDING, "추세가 보유 유지 기준에 미달합니다. 청산 신호가 없어 매도로 단정하지 않습니다.", "추세 회복 또는 청산 신호 발생 시 재평가");
        var action = switch (assessment.status()) {
            case DEFER -> DEFER;
            case AVOID -> AVOID;
            case WATCH -> WATCH;
            case ELIGIBLE -> assessment.grossRewardRisk().compareTo(STRONG_REWARD_RISK) >= 0
                    ? STRONG_BUY_CANDIDATE : BUY_CANDIDATE;
        };
        var reasons = new ArrayList<>(assessment.reasons());
        if (action == STRONG_BUY_CANDIDATE) reasons.add("유효 진입 구간 상단 기준 비용 차감 전 1차 손익비가 2 이상입니다.");
        var entry = new TradeJudgment.Decision(action, reasons,
                List.of("자료 보완 또는 신호·유효 진입 구간·매도 충돌 조건 변경 시 새 분석으로 재평가"));
        return result(in, scope, entry, holder, limitations, assessment);
    }
    private static TradeJudgment.Decision decision(TradeJudgment.Action action, String reason, String condition) {
        return new TradeJudgment.Decision(action, List.of(reason), List.of(condition));
    }
    private static TradeJudgment result(Input in, String scope, TradeJudgment.Decision entry, TradeJudgment.Decision held, List<String> limitations, NewEntryAssessment assessment) {
        return new TradeJudgment(VERSION, in.analysis().engineVersion(), in.asOf(), in.priceDate(), scope, entry, held, limitations, assessment);
    }
}
