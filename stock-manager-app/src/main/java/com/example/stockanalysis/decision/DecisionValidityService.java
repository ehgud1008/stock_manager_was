package com.example.stockanalysis.decision;

import com.example.stockanalysis.unified.eod.TradingCalendar;
import com.example.stockanalysis.judgment.NewEntryEvaluator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Freezes a bounded exchange-calendar schedule. No quote, AI call, or saved-job mutation. */
@Component
public class DecisionValidityService {
    public static final String VERSION="decision-validity-v2-trading-sessions";
    public static final int MAX_SESSIONS=3;
    private final TradingCalendar calendar;private final DecisionJson json;
    public DecisionValidityService(TradingCalendar calendar,DecisionJson json){this.calendar=calendar;this.json=json;}
    public ObjectNode freeze(JsonNode policy) {
        ObjectNode out=(ObjectNode)json.tree(Map.of());out.put("version",VERSION);out.put("maxTradingSessions",MAX_SESSIONS);
        out.put("calendarStatus","AVAILABLE");out.put("signalStatus","NONE");out.putNull("signalRemainingAtPrice");
        try {
            LocalDate price=LocalDate.parse(policy.path("priceDate").asText());
            var anchor=calendar.day(price);out.put("calendarVersion",anchor.version());
            if(anchor.status()!=TradingCalendar.Status.OPEN)out.put("calendarStatus",anchor.status()==TradingCalendar.Status.UNKNOWN?"UNKNOWN":"INVALID_PRICE_DATE");
            out.set("sessions",sessions(price,out));
            boolean exit=Set.of("SELL","REDUCE").contains(policy.path("engineAction").asText());
            var signal=policy.path("tradeJudgment").path("newEntry").path("selectedSignal");
            if(exit) {
                signal=com.fasterxml.jackson.databind.node.MissingNode.getInstance();
                String side="SELL".equals(policy.path("engineAction").asText())?"SELL":"TAKE_PROFIT";
                for(var candidate:policy.path("evidence").path("ANALYSIS").path("signalReport").path("signals"))
                    if(side.equals(candidate.path("side").asText()) && Set.of("ACTIVE","CONFIRMED").contains(candidate.path("state").asText())
                            && (!signal.isObject() || candidate.path("validForBars").asInt()-candidate.path("ageBars").asInt()<signal.path("validForBars").asInt()-signal.path("ageBars").asInt()))signal=candidate;
            }
            if(signal.isObject()) {
                if(!signal.path("ageBars").isIntegralNumber() || !signal.path("validForBars").isIntegralNumber()
                        || !signal.path("ageBars").canConvertToInt() || !signal.path("validForBars").canConvertToInt()
                        || signal.path("ageBars").asInt()<0 || signal.path("validForBars").asInt()<1
                        || !price.toString().equals(signal.path("evaluatedOn").asText()))out.put("signalStatus","INVALID");
                else {
                    int age=signal.path("ageBars").asInt();
                    int remaining=signal.path("validForBars").asInt()-age;
                    if(!exit)remaining=Math.min(remaining,NewEntryEvaluator.MAX_SIGNAL_AGE-age+1);
                    out.put("signalId",signal.path("id").asText());out.put("signalRemainingAtPrice",Math.max(0,remaining));
                    out.put("signalStatus",remaining<=0 || !Set.of("ACTIVE","CONFIRMED").contains(signal.path("state").asText())?"EXPIRED":"VALID");
                }
            } else if(!exit && policy.path("entryEligible").asBoolean())out.put("signalStatus","INVALID");
            var research=policy.path("research");
            if(research.path("collectedAt").isTextual()) {
                var collected=Instant.parse(research.path("collectedAt").asText());
                var researchSchedule=(ObjectNode)json.tree(Map.of("calendarStatus","AVAILABLE","calendarVersion",anchor.version()));
                researchSchedule.set("sessions",sessions(collected.atZone(DecisionService.SEOUL).toLocalDate(),researchSchedule));
                out.set("researchSchedule",researchSchedule);
            }
        } catch(RuntimeException e){out.put("calendarStatus","UNKNOWN");}
        return out;
    }
    private JsonNode sessions(LocalDate base,ObjectNode schedule) {
        var dates=json.tree(List.of());var list=(com.fasterxml.jackson.databind.node.ArrayNode)dates;
        var days=schedule.putArray("calendarDays");days.add(dayRecord(calendar.day(base)));
        for(int i=1;i<=40 && list.size()<MAX_SESSIONS;i++) {
            var date=base.plusDays(i);var day=calendar.day(date);
            days.add(dayRecord(day));
            if(day.status()==TradingCalendar.Status.UNKNOWN || !Objects.equals(day.version(),schedule.path("calendarVersion").asText())) {schedule.put("calendarStatus","UNKNOWN");break;}
            if(day.status()==TradingCalendar.Status.OPEN)list.add(json.tree(Map.of("date",date.toString(),"reviewAt",date.atTime(day.closeTime()).atZone(DecisionService.SEOUL).toInstant().toString())));
        }
        if(list.size()!=MAX_SESSIONS)schedule.put("calendarStatus","UNKNOWN");
        return list;
    }
    private JsonNode dayRecord(TradingCalendar.Day day) {
        return json.tree(Map.of("date",day.date().toString(),"status",day.status().name(),"version",day.version()==null?"UNKNOWN":day.version(),
                "boundary",day.status()==TradingCalendar.Status.OPEN && day.closeTime()!=null?day.closeTime().toString():""));
    }
    private void checkCalendar(ObjectNode schedule) {
        if(!"AVAILABLE".equals(schedule.path("calendarStatus").asText()))return;
        for(var recorded:schedule.path("calendarDays"))
            if(!recorded.equals(dayRecord(calendar.day(LocalDate.parse(recorded.path("date").asText())))))schedule.put("calendarStatus","CHANGED");
        for(var session:schedule.path("sessions")) {
            var day=calendar.day(LocalDate.parse(session.path("date").asText()));
            if(day.status()!=TradingCalendar.Status.OPEN || !Objects.equals(day.version(),schedule.path("calendarVersion").asText())
                    || !day.date().atTime(day.closeTime()).atZone(DecisionService.SEOUL).toInstant().toString().equals(session.path("reviewAt").asText()))schedule.put("calendarStatus","CHANGED");
        }
    }
    public JsonNode current(JsonNode policy,Instant now) {
        ObjectNode copy=policy.deepCopy();boolean derived=!policy.path("validity").isObject();
        ObjectNode schedule=derived?freeze(policy):policy.path("validity").deepCopy();
        // A revised calendar cannot silently extend the old plan's deadline.
        checkCalendar(schedule);
        if(schedule.path("researchSchedule") instanceof ObjectNode research)checkCalendar(research);
        copy.set("validity",schedule);ObjectNode result=(ObjectNode)DecisionValidityPolicy.current(copy,now);
        var today=calendar.day(now.atZone(DecisionService.SEOUL).toLocalDate());
        // Manual COLLECT intentionally excludes today's candle. Avoid a refresh loop after today's EOD boundary.
        String refreshMethod=today.status()==TradingCalendar.Status.UNKNOWN?"CALENDAR_CHECK":
                today.status()==TradingCalendar.Status.OPEN && !today.date().atTime(today.closeTime()).atZone(DecisionService.SEOUL).toInstant().isAfter(now)?"EOD_RESULTS":"COLLECT";
        result.put("refreshMethod",refreshMethod);result.put("refreshHref","/unified-analysis");
        result.put("derivedFromLegacy",derived);return result;
    }
}
