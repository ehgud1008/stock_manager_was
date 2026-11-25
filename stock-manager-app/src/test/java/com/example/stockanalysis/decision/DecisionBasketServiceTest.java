package com.example.stockanalysis.decision;

import com.example.stockanalysis.holdings.HoldingModels;
import com.example.stockanalysis.holdings.HoldingService;
import com.example.stockanalysis.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DecisionBasketServiceTest {
    private final Instant now = Instant.parse("2026-11-18T01:00:00Z");
    private final LocalDate today = LocalDate.parse("2026-11-18");
    private final DecisionJson json = new DecisionJson(new ObjectMapper().registerModule(new JavaTimeModule()));
    private final DecisionStore store = mock(DecisionStore.class);
    private final AccountRiskService accounts = mock(AccountRiskService.class);
    private final HoldingService holdings = mock(HoldingService.class);
    private final DecisionValidityService validity = mock(DecisionValidityService.class);
    private final DecisionSettings settings = new DecisionSettings(true, "owner", "", "", 100, 30, 600000, 128000, 12000);
    private final DecisionBasketService service = new DecisionBasketService(store, settings, accounts, holdings, validity,
            Clock.fixed(now, ZoneOffset.UTC));

    @Test void bothIndividuallyAffordablePurchasesCanExceedSharedCashAndRisk() {
        candidate("first", "005930", "BUY", 0, "600.11", "35.10", "0", 2);
        candidate("second", "000660", "ADD", 5, "500.22", "24.20", "45.20", 2);
        cash(today, 2);
        var result = preview("100.00", "first", "second");
        assertThat(result.purchaseAmount()).isEqualByComparingTo("1100.33");
        assertThat(result.additionalRisk()).isEqualByComparingTo("59.30");
        assertThat(result.existingRisk()).isEqualByComparingTo("45.20");
        assertThat(result.totalRisk()).isEqualByComparingTo("104.50");
        assertThat(result.cash().status()).isEqualTo("EXCEEDED");
        assertThat(result.cash().remaining()).isEqualByComparingTo("-100.33");
        assertThat(result.risk().status()).isEqualTo("EXCEEDED");
        assertThat(preview("104.50", "first", "second").risk().status()).isEqualTo("WITHIN_LIMIT");
        verify(store, times(2)).find("owner", "first");
        verify(store, times(2)).find("owner", "second");
        verifyNoMoreInteractions(store);
    }

    @Test void excludedRowsNeverMakeAnIncompleteSelectionPass() {
        candidate("first", "005930", "BUY", 0, "600", "50", "0", 2);
        candidate("changed", "000660", "ADD", 5, "500", "24", "45", 1);
        cash(today, 2);
        var result = preview("100", "first", "changed");
        assertThat(result.items()).extracting(DecisionBasketService.Line::status)
                .containsExactly("INCLUDED", "ACCOUNT_CHANGED");
        assertThat(result.cash().status()).isEqualTo("INCOMPLETE");
        assertThat(result.cash().remaining()).isNull();
        assertThat(result.purchaseAmount()).isEqualByComparingTo("600");
        assertThat(preview("49", "first", "changed").risk().status()).isEqualTo("EXCEEDED");
        when(holdings.single("005930")).thenReturn(new HoldingModels.Snapshot(now, "AVAILABLE", Map.of()));
        assertThat(preview("100", "first").items().get(0).status()).isEqualTo("HOLDING_UNCONFIRMED");
    }

    @Test void staleCashAndDuplicateSymbolsAreHandledSeparately() {
        candidate("first", "005930", "BUY", 0, "600", "50", "0", 2);
        candidate("revision", "005930", "BUY", 0, "500", "24", "0", 2);
        cash(today.minusDays(1), 2);
        assertThat(preview("100", "first").cash().status()).isEqualTo("UNAVAILABLE");
        assertThatThrownBy(() -> preview("100", "first", "first")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> preview("100", "first", "revision")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> preview("100", "unknown")).isInstanceOf(BusinessException.class);
        when(validity.current(any(), eq(now))).thenReturn(json.tree(Map.of("status", "EXPIRED")));
        assertThat(preview("100", "first").items().get(0).status()).isEqualTo("ANALYSIS_REFRESH");
    }

    @Test void candidateQueryUsesExistingSchemaAndScopesOwnerAndAiFinalRole() {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:basket_" + UUID.randomUUID() + ";MODE=Oracle;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(
                new FileSystemResource(Path.of("..", "database", "oracle", "ddl", "12_trade_decision.sql")),
                new FileSystemResource(Path.of("..", "database", "oracle", "ddl", "26_integrated_decision.sql"))).execute(ds);
        var jdbc = new JdbcTemplate(ds);
        insertJob(jdbc, "snapshot", "owner", "SNAPSHOT", null, null);
        insertJob(jdbc, "included", "owner", "DECISION", "snapshot", "BUY");
        insertJob(jdbc, "engine-only", "owner", "DECISION", "snapshot", "BUY");
        insertJob(jdbc, "exit", "owner", "DECISION", "snapshot", "SELL");
        insertJob(jdbc, "other", "other-owner", "DECISION", "snapshot", "BUY");
        insertPlan(jdbc, "included", "owner", "AI_FINAL");
        insertPlan(jdbc, "engine-only", "owner", "ENGINE");
        insertPlan(jdbc, "exit", "owner", "AI_FINAL");
        insertPlan(jdbc, "other", "other-owner", "AI_FINAL");
        assertThat(new DecisionStore(ds, json).basketCandidates("owner", 0, 20))
                .extracting(DecisionBasketService.Candidate::id).containsExactly("included");
    }

    private void insertJob(JdbcTemplate jdbc, String id, String owner, String kind, String snapshot, String action) {
        jdbc.update("""
                INSERT INTO td_job(job_id,owner_scope,kind,stock_code,content_key,snapshot_id,status,input_json,created_epoch,next_epoch,completed_epoch,final_action)
                VALUES(?,?,?,'005930',?,?,'SUCCEEDED','{}',1,1,1,?)
                """, id, owner, kind, id, snapshot, action);
    }

    private void insertPlan(JdbcTemplate jdbc, String id, String owner, String role) {
        jdbc.update("""
                INSERT INTO td_decision_plan(plan_id,owner_scope,job_id,snapshot_id,opportunity_key,plan_role,horizon_trading_days,policy_version,plan_json,created_epoch)
                VALUES(?,?,?,'snapshot','opportunity',?,20,'policy','{}',1)
                """, id, owner, id, role);
    }

    private void candidate(String id, String code, String action, int held, String purchase, String risk, String existing, int revision) {
        var holding = new HoldingModels.Holding(code, code, BigDecimal.valueOf(held), BigDecimal.TEN,
                null, today, null, null, 3);
        var input = json.tree(Map.of("mode", "INTEGRATED_DECISION", "policy", Map.of(
                "accountRisk", Map.of("settings", Map.of("revision", revision)), "holding", Map.of("holding", holding))));
        var result = json.tree(Map.of("decisionAction", action, "quantity", 10, "riskSizing", Map.of(
                "status", "READY", "estimatedPurchaseAmount", new BigDecimal(purchase),
                "estimatedLossAtStop", new BigDecimal(risk), "existingStopRisk", new BigDecimal(existing))));
        var job = new DecisionModels.Job(id, "owner", "DECISION", code, "snapshot", null, "SUCCEEDED",
                input, result, "hash", "BUY", 1, 2L, 1, null, null, null, false);
        when(store.find("owner", id)).thenReturn(Optional.of(job));
        when(holdings.single(code)).thenReturn(new HoldingModels.Snapshot(now, "AVAILABLE", Map.of(code, holding)));
        when(validity.current(any(), eq(now))).thenReturn(json.tree(Map.of("status", "VALID", "requiresResearchRefresh", false)));
    }

    private void cash(LocalDate date, int revision) {
        var account = new AccountRiskModels.Settings(new BigDecimal("10000"), new BigDecimal("1000"),
                new BigDecimal("100"), new BigDecimal("20"), date, revision, now);
        when(accounts.get()).thenReturn(new AccountRiskModels.View("AVAILABLE", account, now, ""));
    }

    private DecisionBasketService.Preview preview(String limit, String... ids) {
        return service.preview(new DecisionBasketService.Request(List.of(ids), new BigDecimal(limit)));
    }
}
