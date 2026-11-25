package com.example.stockanalysis.decision;

import com.example.stockanalysis.shared.exception.BusinessException;
import com.example.stockanalysis.shared.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class DecisionPerformanceStatistics {
    public enum Group { STOCK, POLICY, MODEL }
    public record Totals(int opportunities, int comparable, int comparablePercent, int comparableAmount,
                         BigDecimal meanAiPct, BigDecimal meanEnginePct, BigDecimal meanDifferencePp,
                         BigDecimal aiPnl, BigDecimal enginePnl, BigDecimal differenceAmount) {}
    public record Row(String group, Totals totals) {}
    public record Report(String basis, String representativeRule, LocalDate from, LocalDate to, Group groupBy,
                         List<String> policies, List<String> models, Totals totals, List<Row> rows) {}
    private static final Set<String> TERMINAL = Set.of("CLOSED", "NOT_ENTERED", "OBSERVED");
    private final DecisionOutcomeStore store;
    private final DecisionStore jobs;
    private final DecisionSettings settings;

    public DecisionPerformanceStatistics(DecisionOutcomeStore store, DecisionStore jobs, DecisionSettings settings) {
        this.store=store;
        this.jobs=jobs;
        this.settings=settings;
    }

    public Report get(LocalDate from, LocalDate to, String stock, String policy, String model, Group group) {
        if (!settings.enabled()) throw new BusinessException(ErrorCode.DECISION_DISABLED);
        if (from.isAfter(to) || from.getYear()<1900 || to.getYear()>9998 || ChronoUnit.DAYS.between(from,to)>365
                || (stock!=null && !stock.matches("[0-9]{6}")) || (policy!=null && policy.length()>100)
                || (model!=null && model.length()>160)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,"조회 기간은 최대 366일이며 종목코드는 여섯 자리입니다.");
        }
        jobs.ensureIntegratedSchema();
        var all = store.samples(settings.ownerScope(),from.atStartOfDay(DecisionService.SEOUL).toInstant().toEpochMilli(),
                to.plusDays(1).atStartOfDay(DecisionService.SEOUL).toInstant().toEpochMilli(),stock);
        var total = new Accumulator();
        Map<String,Accumulator> groups = new TreeMap<>();
        for (var sample : all) {
            if ((policy!=null && !policy.equals(sample.policy())) || (model!=null && !model.equals(sample.model()))) continue;
            String key = switch (group) {
                case STOCK -> sample.stock();
                case POLICY -> sample.policy();
                case MODEL -> sample.model();
            };
            total.add(sample);
            groups.computeIfAbsent(key, ignored -> new Accumulator()).add(sample);
        }
        return new Report("FORWARD_SIMULATION","FIRST_COMPLETED_PER_OPPORTUNITY_BEFORE_FILTERS",from,to,group,
                all.stream().map(DecisionOutcomeStore.Sample::policy).distinct().sorted().toList(),
                all.stream().map(DecisionOutcomeStore.Sample::model).distinct().sorted().toList(),
                total.result(),groups.entrySet().stream().map(e->new Row(e.getKey(),e.getValue().result())).toList());
    }

    private static boolean complete(JsonNode outcome, int horizon, String version) {
        if (outcome==null || !TERMINAL.contains(outcome.path("status").asText())) return false;
        return !PositionOutcomeEngine.VERSION.equals(version) || outcome.path("observedTradingDays").asInt()>=horizon;
    }

    private static final class Accumulator {
        private int cases, comparable, pct, amount;
        private BigDecimal aiPct=BigDecimal.ZERO, enginePct=BigDecimal.ZERO, aiAmount=BigDecimal.ZERO, engineAmount=BigDecimal.ZERO;
        void add(DecisionOutcomeStore.Sample sample) {
            cases++;
            var ai=sample.ai();
            var engine=sample.engine();
            if (!complete(ai,sample.horizon(),sample.outcomeVersion())
                    || !complete(engine,sample.horizon(),sample.outcomeVersion())) return;
            var ac=ai.path("startingCapital");
            var ec=engine.path("startingCapital");
            boolean sameCapital=ac.isNumber() && ec.isNumber() && ac.decimalValue().compareTo(ec.decimalValue())==0;
            boolean validPct=(sameCapital || (!ac.isNumber() && !ec.isNumber()))
                    && ai.path("netReturnPct").isNumber() && engine.path("netReturnPct").isNumber();
            boolean validAmount=ai.path("netPnlAmount").isNumber() && engine.path("netPnlAmount").isNumber();
            if (validPct || validAmount) comparable++;
            if (validPct) {
                pct++;
                aiPct=aiPct.add(ai.path("netReturnPct").decimalValue());
                enginePct=enginePct.add(engine.path("netReturnPct").decimalValue());
            }
            if (validAmount) {
                amount++;
                aiAmount=aiAmount.add(ai.path("netPnlAmount").decimalValue());
                engineAmount=engineAmount.add(engine.path("netPnlAmount").decimalValue());
            }
        }
        Totals result() {
            return new Totals(cases,comparable,pct,amount,mean(aiPct),mean(enginePct),mean(aiPct.subtract(enginePct)),
                    amount==0?null:aiAmount,amount==0?null:engineAmount,amount==0?null:aiAmount.subtract(engineAmount));
        }
        BigDecimal mean(BigDecimal sum) {
            return pct==0?null:sum.divide(BigDecimal.valueOf(pct),4,RoundingMode.HALF_UP);
        }
    }
}
