package com.example.stockanalysis.decision;

import com.example.stockanalysis.holdings.*;
import com.example.stockanalysis.stock.application.StockQueryService;
import com.example.stockanalysis.stock.domain.*;
import com.example.stockanalysis.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DecisionRecheckServiceTest {
    DecisionStore store=mock(DecisionStore.class);DecisionSettings settings=mock(DecisionSettings.class);
    HoldingService holdings=mock(HoldingService.class);AccountRiskService accounts=mock(AccountRiskService.class);
    StockQueryService stocks=mock(StockQueryService.class);DecisionValidityService validity=mock(DecisionValidityService.class);
    DecisionJson json=new DecisionJson(new ObjectMapper().registerModule(new JavaTimeModule()));
    Instant now=Instant.parse("2026-11-17T01:00:00Z");
    ObjectNode input;DecisionModels.Job job;AccountRiskModels.Settings account;HoldingModels.Holding holding;
    DecisionRecheckService service;
    @BeforeEach void setup() {
        when(settings.enabled()).thenReturn(true);when(settings.ownerScope()).thenReturn("owner");
        var p=new PositionSizingPolicyTest().policy();
        p.set("stock",json.tree(Map.of("currentPrice",98)));
        holding=new HoldingModels.Holding("005930","test",BigDecimal.ZERO,null,null,LocalDate.parse("2026-11-17"),null,null,2);
        ((ObjectNode)p.path("holding")).set("holding",json.tree(holding));
        account=new AccountRiskModels.Settings(new BigDecimal("100000"),new BigDecimal("10000"),new BigDecimal("500"),new BigDecimal("5"),LocalDate.parse("2026-11-17"),1,now);
        p.set("accountRisk",json.tree(new AccountRiskModels.View("AVAILABLE",account,now,"")));
        input=(ObjectNode)json.tree(Map.of("mode","INTEGRATED_DECISION","policy",p,"versions",Map.of("sizing",PositionSizingPolicy.VERSION)));
        job=new DecisionModels.Job("id","owner","DECISION","005930","snapshot",null,"SUCCEEDED",input,
                json.tree(Map.of("decisionAction","BUY","quantity",47)),"hash","BUY",0L,1L,1,null,null,null,false);
        when(store.find("owner","id")).thenReturn(Optional.of(job));
        when(holdings.single("005930")).thenReturn(new HoldingModels.Snapshot(now,"AVAILABLE",Map.of("005930",holding)));
        when(accounts.get()).thenReturn(new AccountRiskModels.View("AVAILABLE",account,now,""));
        when(validity.current(any(),any())).thenReturn(json.tree(Map.of("status","VALID","requiresResearchRefresh",false)));
        quote("005930","98");
        service=new DecisionRecheckService(store,settings,json,holdings,accounts,stocks,validity,Clock.fixed(now,ZoneOffset.UTC));
    }
    void quote(String code,String price) {
        when(stocks.getStockProfile("005930")).thenReturn(new StockProfile(StockCode.of(code),"test",new BigDecimal(price),null,null,null,null,null,null,null,null,null,null));
    }
    @Test void comparesWithoutMutatingOrWritingAndNeverClaimsExecutionReady() {
        var before=input.deepCopy();var r=service.get("id");
        assertThat(r.path("price").path("entryRangeStatus").asText()).isEqualTo("WITHIN");
        assertThat(r.path("holding").path("comparison").asText()).isEqualTo("UNCHANGED");
        assertThat(r.path("accountRisk").path("comparison").asText()).isEqualTo("UNCHANGED");
        assertThat(r.path("sizing").path("indicativeQuantity").asInt()).isEqualTo(47);
        assertThat(r.path("status").asText()).isEqualTo("PARTIAL");
        assertThat(r.path("price").path("sourceAsOf").isNull()).isTrue();
        assertThat(r.path("tickValidation").path("status").asText()).isEqualTo("UNVERIFIABLE");
        assertThat(input).isEqualTo(before);verify(store).find("owner","id");verifyNoMoreInteractions(store);
        verify(holdings).single("005930");verifyNoMoreInteractions(holdings);
        verify(accounts).get();verifyNoMoreInteractions(accounts);
    }
    @Test void boundaryInclusiveButOutsideRequiresAnalysisAndDoesNotRewritePricePlan() {
        for(String price:List.of("95","100")) {quote("005930",price);assertThat(service.get("id").path("price").path("entryRangeStatus").asText()).isEqualTo("WITHIN");}
        quote("005930","101");var r=service.get("id");assertThat(r.path("requiresAnalysisRefresh").asBoolean()).isTrue();
        assertThat(r.path("sizing").path("entryRangeConfirmed").asBoolean()).isFalse();
        assertThat(input.path("policy").path("plan").path("entryTo").asInt()).isEqualTo(100);
    }
    @Test void unavailableAndWrongSymbolNeverUseSavedPriceOrAssumeZeroBalance() {
        when(stocks.getStockProfile("005930")).thenThrow(new IllegalStateException("private provider detail"));
        when(holdings.single("005930")).thenReturn(HoldingModels.Snapshot.unknown(now));
        var r=service.get("id");assertThat(r.path("price").path("value").isMissingNode()).isTrue();
        assertThat(r.path("sizing").path("status").asText()).isEqualTo("PRICE_UNAVAILABLE");
        assertThat(r.path("holding").path("comparison").asText()).isEqualTo("UNKNOWN");
        assertThat(r.toString()).doesNotContain("private provider detail");
        reset(stocks);quote("000660","98");assertThat(service.get("id").path("price").path("status").asText()).isEqualTo("SYMBOL_MISMATCH");
    }
    @Test void manualChangesAndStaleDatesRequireFreshDecision() {
        var changed=new AccountRiskModels.Settings(account.totalAssets(),BigDecimal.ZERO,account.maxLossPerTrade(),account.maxPositionPercent(),account.balanceDate(),2,now);
        when(accounts.get()).thenReturn(new AccountRiskModels.View("AVAILABLE",changed,now,""));
        var r=service.get("id");assertThat(r.path("accountRisk").path("comparison").asText()).isEqualTo("CHANGED");
        assertThat(r.path("sizing").path("status").asText()).isEqualTo("ZERO_CAPACITY");assertThat(r.path("requiresNewDecision").asBoolean()).isTrue();
        changed=new AccountRiskModels.Settings(account.totalAssets(),account.availableCash(),account.maxLossPerTrade(),account.maxPositionPercent(),account.balanceDate().minusDays(1),2,now);
        when(accounts.get()).thenReturn(new AccountRiskModels.View("AVAILABLE",changed,now,""));
        assertThat(service.get("id").path("sizing").path("status").asText()).isEqualTo("ACCOUNT_RISK_STALE");
    }
    @Test void missingRegistrationLegacyAndPositionSwitchDoNotInventQuantity() {
        when(holdings.single("005930")).thenReturn(HoldingModels.Snapshot.unknown(now));
        assertThat(service.get("id").path("sizing").path("status").asText()).isEqualTo("POSITION_UNKNOWN");
        when(holdings.single("005930")).thenReturn(new HoldingModels.Snapshot(now,"AVAILABLE",Map.of()));
        assertThat(service.get("id").path("sizing").path("status").asText()).isEqualTo("POSITION_UNCONFIRMED");
        ((ObjectNode)input.path("versions")).put("sizing",PositionSizingPolicy.LEGACY_VERSION);
        assertThat(service.get("id").path("sizing").path("status").asText()).isEqualTo("LEGACY_POLICY_REQUIRES_NEW_DECISION");
        ((ObjectNode)input.path("versions")).put("sizing",PositionSizingPolicy.VERSION);
        var held=new HoldingModels.Holding("005930","test",BigDecimal.TEN,new BigDecimal("80"),null,holding.balanceDate(),null,null,3);
        when(holdings.single("005930")).thenReturn(new HoldingModels.Snapshot(now,"AVAILABLE",Map.of("005930",held)));
        assertThat(service.get("id").path("sizing").path("status").asText()).isEqualTo("POSITION_ACTION_CHANGED");
    }
    @Test void ownerLookupRejectsBeforeExternalReads() {
        when(store.find("owner","id")).thenReturn(Optional.empty());
        assertThatThrownBy(()->service.get("id")).isInstanceOf(BusinessException.class);
        verifyNoInteractions(stocks,holdings,accounts);
    }
    @Test void heldOpinionStillWarnsOnRegisteredStopWithoutCreatingSellOpinion() {
        ((ObjectNode)job.result()).put("decisionAction","HOLD");
        var held=new HoldingModels.Holding("005930","test",BigDecimal.TEN,new BigDecimal("80"),null,holding.balanceDate(),new BigDecimal("99"),null,3);
        when(holdings.single("005930")).thenReturn(new HoldingModels.Snapshot(now,"AVAILABLE",Map.of("005930",held)));
        var r=service.get("id");
        assertThat(r.path("price").path("stopReferenceStatus").asText()).isEqualTo("BREACHED");
        assertThat(r.path("requiresAnalysisRefresh").asBoolean()).isTrue();
        assertThat(r.path("sizing").path("status").asText()).isEqualTo("ACTION_NOT_SIZED");
        assertThat(job.result().path("decisionAction").asText()).isEqualTo("HOLD");
    }
    @Test void heldRecheckRevaluesExposureWithQuoteAndKeepsExcessReductionSeparateFromFullExit() {
        var held=new HoldingModels.Holding("005930","test",new BigDecimal("80"),new BigDecimal("80"),null,holding.balanceDate(),null,null,3);
        when(holdings.single("005930")).thenReturn(new HoldingModels.Snapshot(now,"AVAILABLE",Map.of("005930",held)));
        ((ObjectNode)job.result()).put("decisionAction","REDUCE");
        quote("005930","100");var r=service.get("id");
        assertThat(r.path("sizing").path("current").path("existingExposure").decimalValue()).isEqualByComparingTo("8000");
        assertThat(r.path("sizing").path("indicativeQuantity").asInt()).isEqualTo(32);
        when(accounts.get()).thenReturn(new AccountRiskModels.View("AVAILABLE",
                new AccountRiskModels.Settings(account.totalAssets(),BigDecimal.ZERO,BigDecimal.ZERO,account.maxPositionPercent(),account.balanceDate(),2,now),now,""));
        r=service.get("id");
        assertThat(r.path("sizing").path("status").asText()).isEqualTo("FULL_EXIT_REVIEW_REQUIRED");
        assertThat(r.path("sizing").has("indicativeQuantity")).isFalse();
        ((ObjectNode)job.result()).put("decisionAction","SELL");
        assertThat(service.get("id").path("sizing").path("indicativeQuantity").asInt()).isEqualTo(80);
    }
}
