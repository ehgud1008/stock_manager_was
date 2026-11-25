package com.example.stockanalysis.decision;

import com.example.stockanalysis.holdings.HoldingService;
import com.example.stockanalysis.shared.exception.BusinessException;
import com.example.stockanalysis.shared.exception.ErrorCode;
import jakarta.validation.constraints.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class TakeProfitPlanService {
    public record Request(@NotEmpty @Size(max=20) List<@NotNull @DecimalMin("0") @DecimalMax("100")
            @Digits(integer=3,fraction=2) BigDecimal> percentages) {}
    public record Target(BigDecimal price, BigDecimal percent, BigDecimal quantity) {}
    public record Preview(String purpose, String stockCode, String source, long holdingRevision, LocalDate balanceDate,
                          Instant checkedAt, BigDecimal heldQuantity, BigDecimal plannedQuantity, BigDecimal remaining,
                          List<Target> targets) {}
    private final DecisionStore jobs;
    private final DecisionSettings settings;
    private final HoldingService holdings;
    private final DecisionValidityService validity;
    private final Clock clock;

    public TakeProfitPlanService(DecisionStore jobs, DecisionSettings settings, HoldingService holdings,
                                 DecisionValidityService validity, Clock clock) {
        this.jobs=jobs;
        this.settings=settings;
        this.holdings=holdings;
        this.validity=validity;
        this.clock=clock;
    }

    public Preview preview(String id, Request request) {
        if (!settings.enabled()) throw new BusinessException(ErrorCode.DECISION_DISABLED);
        var job=jobs.find(settings.ownerScope(),id).orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!"DECISION".equals(job.kind()) || !"SUCCEEDED".equals(job.status())
                || !"INTEGRATED_DECISION".equals(job.input().path("mode").asText())) throw invalid("완료된 통합 판단이 필요합니다.");
        var policy=job.input().path("policy");
        var plan=policy.path("plan");
        if (!new DecisionValidityPolicy().validPlan(plan) || !plan.path("targets").isArray()) throw invalid("저장된 목표가를 확인할 수 없습니다.");
        if (plan.path("targets").size()!=request.percentages().size()) throw invalid("모든 목표의 축소 비율을 입력하세요. 사용하지 않을 목표에는 0을 입력하세요.");
        Instant now=clock.instant();
        if (!"VALID".equals(validity.current(policy,now).path("status").asText())) throw invalid("저장 목표가의 분석을 갱신한 뒤 검토하세요.");
        var balance=holdings.single(job.stockCode());
        var position=balance.items().get(job.stockCode());
        if (!"AVAILABLE".equals(balance.status()) || position==null || position.quantity().signum()<=0
                || position.quantity().stripTrailingZeros().scale()>0
                || !LocalDate.ofInstant(now,DecisionService.SEOUL).equals(position.balanceDate()))
            throw invalid("오늘 기준으로 등록된 정수 보유 수량이 필요합니다.");
        var prices=new ArrayList<BigDecimal>();
        plan.path("targets").forEach(target->prices.add(target.decimalValue()));
        var targets=allocate(position.quantity(),prices,request.percentages());
        BigDecimal total=targets.stream().map(Target::quantity).reduce(BigDecimal.ZERO,BigDecimal::add);
        return new Preview("USER_TAKE_PROFIT",job.stockCode(),"MANUAL_REGISTERED",position.revision(),position.balanceDate(),
                balance.checkedAt(),position.quantity(),total,position.quantity().subtract(total),targets);
    }

    static List<Target> allocate(BigDecimal held, List<BigDecimal> prices, List<BigDecimal> percentages) {
        BigDecimal cumulative=BigDecimal.ZERO;
        BigDecimal previous=BigDecimal.ZERO;
        var targets=new ArrayList<Target>();
        for (int i=0;i<prices.size();i++) {
            BigDecimal percent=percentages.get(i);
            cumulative=cumulative.add(percent);
            if (percent.signum()<0 || cumulative.compareTo(BigDecimal.valueOf(100))>0) throw invalid("축소 비율 합계는 0% 이상 100% 이하여야 합니다.");
            BigDecimal current=held.multiply(cumulative).movePointLeft(2).setScale(0,RoundingMode.DOWN);
            targets.add(new Target(prices.get(i),percent,current.subtract(previous)));
            previous=current;
        }
        return List.copyOf(targets);
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_REQUEST,message);
    }
}
