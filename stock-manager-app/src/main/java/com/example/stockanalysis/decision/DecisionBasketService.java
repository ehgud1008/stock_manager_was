package com.example.stockanalysis.decision;

import com.example.stockanalysis.holdings.HoldingService;
import com.example.stockanalysis.shared.exception.BusinessException;
import com.example.stockanalysis.shared.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class DecisionBasketService {
    static final int MAX_SELECTION = 20;
    private static final int PAGE_SIZE = 20;
    private static final Set<String> PURCHASE_ACTIONS = Set.of("BUY", "ADD");

    public record Request(
            @NotEmpty @Size(max=MAX_SELECTION) List<@NotBlank @Pattern(regexp="[A-Za-z0-9-]{1,36}") String> decisionIds,
            @NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal totalRiskLimit) {}
    public record Candidate(String id, String stockCode, String snapshotId, long completedAt) {}
    public record Candidates(List<Candidate> items, boolean hasMore) {}
    public record Line(String id, String stockCode, String snapshotId, String action, String status,
                       BigDecimal quantity, BigDecimal purchaseAmount, BigDecimal additionalRisk, BigDecimal existingRisk) {}
    public record Limit(BigDecimal amount, BigDecimal remaining, String status) {}
    public record Preview(String scope, Instant checkedAt, LocalDate balanceDate, Long accountRevision,
                          String cashStatus, List<Line> items, int includedCount,
                          BigDecimal purchaseAmount, BigDecimal additionalRisk, BigDecimal existingRisk,
                          BigDecimal totalRisk, Limit cash, Limit risk) {}

    private final DecisionStore store;
    private final DecisionSettings settings;
    private final AccountRiskService accounts;
    private final HoldingService holdings;
    private final DecisionValidityService validity;
    private final Clock clock;

    public DecisionBasketService(DecisionStore store, DecisionSettings settings, AccountRiskService accounts,
            HoldingService holdings, DecisionValidityService validity, Clock clock) {
        this.store = store;
        this.settings = settings;
        this.accounts = accounts;
        this.holdings = holdings;
        this.validity = validity;
        this.clock = clock;
    }

    public Candidates candidates(int page) {
        requireEnabled();
        if (page < 0) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        var rows = store.basketCandidates(settings.ownerScope(), page, PAGE_SIZE);
        return new Candidates(rows.stream().limit(PAGE_SIZE).toList(), rows.size() > PAGE_SIZE);
    }

    public Preview preview(Request request) {
        requireEnabled();
        var ids = request.decisionIds();
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "같은 판단을 두 번 선택할 수 없습니다.");
        }
        var jobs = ids.stream().map(id -> store.find(settings.ownerScope(), id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND))).toList();
        Set<String> symbols = new HashSet<>();
        for (var job : jobs) {
            if (!"DECISION".equals(job.kind()) || !"SUCCEEDED".equals(job.status())
                    || !"INTEGRATED_DECISION".equals(job.input().path("mode").asText()) || job.result() == null
                    || !PURCHASE_ACTIONS.contains(job.result().path("decisionAction").asText())) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "완료된 통합 매수·추가매수 판단만 선택할 수 있습니다.");
            }
            if (!symbols.add(job.stockCode())) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "같은 종목의 판단은 하나만 선택하세요.");
            }
        }
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, DecisionService.SEOUL);
        var accountView = accounts.get();
        var account = accountView.settings();
        String cashStatus = account == null ? accountView.status()
                : today.equals(account.balanceDate()) ? "AVAILABLE" : "STALE";
        boolean currentCash = "AVAILABLE".equals(cashStatus);
        var items = jobs.stream().map(job -> line(job, account, today, now)).toList();
        var included = items.stream().filter(line -> "INCLUDED".equals(line.status())).toList();
        BigDecimal purchase = sum(included, Line::purchaseAmount);
        BigDecimal additional = sum(included, Line::additionalRisk);
        BigDecimal existing = sum(included, Line::existingRisk);
        BigDecimal total = additional.add(existing);
        boolean complete = included.size() == items.size();
        return new Preview("SELECTED_PURCHASE_REVIEW", now, account == null ? null : account.balanceDate(),
                account == null ? null : account.revision(), cashStatus, items, included.size(), purchase,
                additional, existing, total, limit(purchase, currentCash ? account.availableCash() : null, complete),
                limit(total, request.totalRiskLimit(), complete));
    }

    private Line line(DecisionModels.Job job, AccountRiskModels.Settings account, LocalDate today, Instant now) {
        var result = job.result();
        var policy = job.input().path("policy");
        var sizing = result.path("riskSizing");
        String action = result.path("decisionAction").asText();
        BigDecimal quantity = amount(result.path("quantity"));
        BigDecimal purchase = amount(sizing.path("estimatedPurchaseAmount"));
        BigDecimal additional = amount(sizing.path("estimatedLossAtStop"));
        BigDecimal existing = "ADD".equals(action) ? amount(sizing.path("existingStopRisk")) : BigDecimal.ZERO;
        String status;
        if (!"READY".equals(sizing.path("status").asText()) || quantity == null || quantity.signum() <= 0
                || quantity.stripTrailingZeros().scale() > 0 || purchase == null || purchase.signum() <= 0
                || additional == null || additional.signum() <= 0 || existing == null) {
            status = "SIZING_UNAVAILABLE";
        } else {
            status = reviewStatus(job, policy, account, today, now);
        }
        boolean included = "INCLUDED".equals(status);
        return new Line(job.id(), job.stockCode(), job.snapshotId(), action, status, quantity,
                included ? purchase : null, included ? additional : null, included ? existing : null);
    }

    private String reviewStatus(DecisionModels.Job job, JsonNode policy, AccountRiskModels.Settings account,
            LocalDate today, Instant now) {
        var timing = validity.current(policy, now);
        if (!"VALID".equals(timing.path("status").asText())) return "ANALYSIS_REFRESH";
        if (timing.path("requiresResearchRefresh").asBoolean(true)) return "RESEARCH_REFRESH";
        var savedAccount = policy.path("accountRisk").path("settings");
        if (account != null && (!savedAccount.path("revision").isIntegralNumber()
                || savedAccount.path("revision").asLong() != account.revision())) return "ACCOUNT_CHANGED";

        var balance = holdings.single(job.stockCode());
        if (!"AVAILABLE".equals(balance.status())) return "HOLDING_UNAVAILABLE";
        var current = balance.items().get(job.stockCode());
        if (current == null || !today.equals(current.balanceDate())) return "HOLDING_UNCONFIRMED";
        var saved = policy.path("holding").path("holding");
        var savedQuantity = amount(saved.path("quantity"));
        if (!saved.path("revision").isIntegralNumber() || saved.path("revision").asLong() != current.revision()
                || savedQuantity == null || savedQuantity.compareTo(current.quantity()) != 0
                || !today.toString().equals(saved.path("balanceDate").asText())) return "HOLDING_CHANGED";
        return "INCLUDED";
    }

    private static BigDecimal amount(JsonNode value) {
        return value.isNumber() && value.decimalValue().signum() >= 0 ? value.decimalValue() : null;
    }

    private static BigDecimal sum(List<Line> items, java.util.function.Function<Line, BigDecimal> field) {
        return items.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Limit limit(BigDecimal subtotal, BigDecimal budget, boolean complete) {
        if (budget == null) return new Limit(null, null, "UNAVAILABLE");
        BigDecimal remaining = budget.subtract(subtotal);
        String status = remaining.signum() < 0 ? "EXCEEDED" : complete ? "WITHIN_LIMIT" : "INCOMPLETE";
        return new Limit(budget, complete || remaining.signum() < 0 ? remaining : null, status);
    }

    private void requireEnabled() {
        if (!settings.enabled()) throw new BusinessException(ErrorCode.DECISION_DISABLED);
    }
}
