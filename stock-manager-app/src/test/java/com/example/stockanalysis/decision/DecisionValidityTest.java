package com.example.stockanalysis.decision;

import com.example.stockanalysis.unified.eod.TradingCalendar;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DecisionValidityTest {
    DecisionJson json;TradingCalendar calendar;DecisionValidityService service;Set<LocalDate> holidays;AtomicReference<String> version;
    @BeforeEach void setup(){
        json=new DecisionJson(new com.fasterxml.jackson.databind.ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()));
        calendar=mock(TradingCalendar.class);holidays=new HashSet<>();version=new AtomicReference<>("v1");
        when(calendar.day(any())).thenAnswer(i->{LocalDate d=i.getArgument(0);boolean closed=holidays.contains(d)||Set.of(DayOfWeek.SATURDAY,DayOfWeek.SUNDAY).contains(d.getDayOfWeek());
            return new TradingCalendar.Day(d,closed?TradingCalendar.Status.CLOSED:TradingCalendar.Status.OPEN,LocalTime.of(20,0),version.get());});
        service=new DecisionValidityService(calendar,json);
    }
    ObjectNode policy(){return (ObjectNode)json.tree(Map.of("priceDate","2026-11-13","analysisAsOf","2026-11-13T20:00:00","entryEligible",false,
            "collectionQuality","COMPLETE","engineAction","WAIT","holding",Map.of("positionStatus","NOT_HELD","action","WATCH"),"research",Map.of("status","MISSING")));}
    ObjectNode frozen(ObjectNode policy){policy.set("validity",service.freeze(policy));return policy;}
    JsonNode at(JsonNode policy,String date){return service.current(policy,Instant.parse(date));}
    void signal(ObjectNode p,int age,int valid){p.set("tradeJudgment",json.tree(Map.of("newEntry",Map.of("selectedSignal",Map.of("id","signal-a","ageBars",age,"validForBars",valid,"state","ACTIVE","evaluatedOn","2026-11-13")))));}
    @Test void weekendsDoNotConsumeSessionsAndTheCutoffIsExactlyKoreanEodBoundary(){
        var p=frozen(policy());
        assertThat(at(p,"2026-11-15T12:00:00Z").path("elapsedTradingDays").asInt()).isZero();
        assertThat(at(p,"2026-11-16T10:59:59Z").path("status").asText()).isEqualTo("VALID");
        var review=at(p,"2026-11-16T11:00:00Z");
        assertThat(review.path("status").asText()).isEqualTo("REVIEW_REQUIRED");
        assertThat(review.path("reasonCodes").toString()).contains("NEW_TRADING_SESSION_COMPLETED");
        assertThat(review.path("validUntil").asText()).isEqualTo("2026-11-18T11:00:00Z");
        assertThat(at(p,"2026-11-18T11:00:00Z").path("status").asText()).isEqualTo("EXPIRED");
        assertThat(at(p,"2026-11-16T10:59:59Z").path("refreshMethod").asText()).isEqualTo("COLLECT");
        assertThat(at(p,"2026-11-16T11:00:00Z").path("refreshMethod").asText()).isEqualTo("EOD_RESULTS");
        assertThat(at(p,"2026-11-15T12:00:00Z").path("refreshMethod").asText()).isEqualTo("COLLECT");
    }
    @Test void holidaysAndSpecialSessionBoundariesAreHonored(){
        holidays.add(LocalDate.of(2026,11,16));
        when(calendar.day(LocalDate.of(2026,11,17))).thenReturn(new TradingCalendar.Day(LocalDate.of(2026,11,17),TradingCalendar.Status.OPEN,LocalTime.of(21,0),"v1"));
        var p=frozen(policy());
        assertThat(at(p,"2026-11-17T11:59:59Z").path("status").asText()).isEqualTo("VALID");
        assertThat(at(p,"2026-11-17T12:00:00Z").path("status").asText()).isEqualTo("REVIEW_REQUIRED");
        assertThat(at(p,"2026-11-17T12:00:00Z").path("validUntil").asText()).isEqualTo("2026-11-19T11:00:00Z");
    }
    @Test void signalRemainingBarsShortenTheDeadlineAndNeverExtendTheAnalysisCap(){
        var p=policy();signal(p,1,2);frozen(p);
        assertThat(at(p,"2026-11-13T12:00:00Z").path("signalRemainingTradingDays").asInt()).isEqualTo(1);
        assertThat(at(p,"2026-11-13T12:00:00Z").path("validUntil").asText()).isEqualTo("2026-11-16T11:00:00Z");
        var result=at(p,"2026-11-16T11:00:00Z");assertThat(result.path("status").asText()).isEqualTo("EXPIRED");
        assertThat(result.path("reasonCodes").toString()).contains("SIGNAL_EXPIRED");
        p=policy();signal(p,0,10);frozen(p);
        assertThat(at(p,"2026-11-13T12:00:00Z").path("validUntil").asText()).isEqualTo("2026-11-18T11:00:00Z");
    }
    @Test void invalidSignalAndUnknownCalendarCannotImplyValidity(){
        var p=policy();p.put("entryEligible",true);frozen(p);
        assertThat(at(p,"2026-11-13T12:00:00Z").path("status").asText()).isEqualTo("UNVERIFIABLE");
        when(calendar.day(LocalDate.of(2026,11,17))).thenReturn(new TradingCalendar.Day(LocalDate.of(2026,11,17),TradingCalendar.Status.UNKNOWN,null,"v1"));
        p=frozen(policy());var result=at(p,"2026-11-13T12:00:00Z");
        assertThat(result.path("status").asText()).isEqualTo("UNVERIFIABLE");assertThat(result.path("entryBlocked").asBoolean()).isTrue();
    }
    @Test void exitJudgmentsUseExitSignalRatherThanAnUnrelatedBuySignal(){
        var p=policy();p.put("engineAction","SELL");signal(p,0,10);
        p.set("evidence",json.tree(Map.of("ANALYSIS",Map.of("signalReport",Map.of("signals",List.of(Map.of(
            "id","sell-a","side","SELL","state","ACTIVE","evaluatedOn","2026-11-13","ageBars",1,"validForBars",2)))))));
        frozen(p);assertThat(at(p,"2026-11-16T11:00:00Z").path("status").asText()).isEqualTo("EXPIRED");
        assertThat(p.path("validity").path("signalId").asText()).isEqualTo("sell-a");
    }
    @Test void changedCalendarDoesNotSilentlyMoveTheFrozenDeadline(){
        var p=frozen(policy());String hash=json.hash(p);version.set("v2");
        var result=at(p,"2026-11-13T12:00:00Z");
        assertThat(result.path("status").asText()).isEqualTo("UNVERIFIABLE");
        assertThat(result.path("reasonCodes").toString()).contains("CALENDAR_CHANGED");assertThat(json.hash(p)).isEqualTo(hash);
    }
    @Test void changingARecordedHolidayWithoutBumpingTheCalendarVersionStillRequiresReview(){
        holidays.add(LocalDate.of(2026,11,16));var p=frozen(policy());holidays.clear();
        var result=at(p,"2026-11-16T11:00:00Z");
        assertThat(result.path("status").asText()).isEqualTo("UNVERIFIABLE");
        assertThat(result.path("reasonCodes").toString()).contains("CALENDAR_CHANGED");
    }
    @Test void futureAnalysisAndInvalidDatesFailClosed(){
        var p=policy();p.put("analysisAsOf","2026-11-13T23:00:00");frozen(p);
        assertThat(at(p,"2026-11-13T12:00:00Z").path("reasonCodes").toString()).contains("ANALYSIS_FUTURE");
        p=policy();p.put("priceDate","invalid");frozen(p);
        assertThat(at(p,"2026-11-13T12:00:00Z").path("status").asText()).isEqualTo("UNVERIFIABLE");
    }
    @Test void researchUsesItsOwnCollectionDateAndDoesNotReclassifyMissingEvidenceAsNeutral(){
        var p=policy();p.set("research",json.tree(Map.of("status","VALID","collectedAt","2026-11-12T12:00:00Z")));frozen(p);
        var result=at(p,"2026-11-17T11:00:00Z");
        assertThat(result.path("researchStatus").asText()).isEqualTo("EXPIRED");
        assertThat(result.path("requiresResearchRefresh").asBoolean()).isTrue();
        assertThat(at(frozen(policy()),"2026-11-13T12:00:00Z").path("researchStatus").asText()).isEqualTo("MISSING");
    }
    @Test void legacyReadIsDerivedAndNeverWritesIntoTheOldPolicy(){
        var p=policy();String hash=json.hash(p);var result=at(p,"2026-11-16T11:00:00Z");
        assertThat(result.path("derivedFromLegacy").asBoolean()).isTrue();assertThat(json.hash(p)).isEqualTo(hash);
    }
    @Test void queuedAndLateResponsesCannotAuthorizeOldSignals(){
        var p=frozen(policy());var input=json.tree(Map.of("policy",p));
        var result=new DecisionArbitrator(json).finish(input,json.tree(Map.of("action","BUY")),null,Instant.parse("2026-11-16T11:00:00Z").toEpochMilli());
        assertThat(result.path("decisionAction").asText()).isEqualTo("DEFER");
        assertThat(result.path("validityAtCompletion").path("status").asText()).isEqualTo("REVIEW_REQUIRED");
        ((ObjectNode)p.path("holding")).put("positionStatus","HELD").put("action","EXIT_TRIGGERED");
        result=new DecisionArbitrator(json).finish(json.tree(Map.of("policy",p)),null,"AI_INVALID_RESPONSE",Instant.parse("2026-11-18T11:00:00Z").toEpochMilli());
        assertThat(result.path("decisionAction").asText()).isEqualTo("DEFER");assertThat(result.path("engineAction").asText()).isEqualTo("WAIT");
    }
}
