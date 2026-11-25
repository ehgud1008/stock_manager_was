package com.example.stockanalysis.holdings;

import java.math.*;
import java.time.*;
import java.util.*;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.example.stockanalysis.judgment.TradeJudgment;

public final class HoldingModels {
    private HoldingModels() {}
    public record Holding(String stockCode, String stockName, BigDecimal quantity, BigDecimal averageBuyPrice,
                          LocalDate purchasedOn, LocalDate balanceDate, BigDecimal stopPrice, BigDecimal targetPrice, long revision) {}
    public record Edit(@Schema(description = "종목명", example = "삼성전자") @NotBlank @Size(max=100) String stockName,
                       @Schema(description = "보유 수량 (정수). 0이면 명시적 미보유", example = "10")
                       @NotNull @DecimalMin("0") @Digits(integer=10,fraction=0) BigDecimal quantity,
                       @Schema(description = "평균 매수단가 (원). 수량이 양수이면 필수", example = "70000")
                       @DecimalMin(value="0",inclusive=false) @Digits(integer=12,fraction=2) BigDecimal averageBuyPrice,
                       @Schema(description = "선택 매수일. 잔고 기준일 이후 불가") LocalDate purchasedOn,
                       @Schema(description = "잔고 기준일. 미래 날짜 불가", example = "2026-11-13") @NotNull LocalDate balanceDate,
                       @Schema(description = "선택 저장 손절가 (원)")
                       @DecimalMin(value="0",inclusive=false) @Digits(integer=12,fraction=2) BigDecimal stopPrice,
                       @Schema(description = "선택 저장 목표가 (원)")
                       @DecimalMin(value="0",inclusive=false) @Digits(integer=12,fraction=2) BigDecimal targetPrice,
                       @Schema(description = "신규 등록은 0, 수정은 조회한 revision. 불일치 시 HTTP 409", example = "0")
                       @Min(0) long expectedRevision) {}
    public record Snapshot(Instant checkedAt, String status, Map<String,Holding> items) {
        public Snapshot { items=Map.copyOf(items); }
        public static Snapshot unknown(Instant now) {return new Snapshot(now,"UNAVAILABLE",Map.of());}
    }
    public record Evaluation(String positionStatus, String availability, Instant checkedAt, Holding holding,
                             LocalDate priceDate, BigDecimal referencePrice, BigDecimal investedAmount,
                             BigDecimal marketValue, BigDecimal pnlAmount, BigDecimal pnlPercent,
                             BigDecimal stopPrice, BigDecimal stopPnlAmount, BigDecimal additionalRiskAmount,
                             BigDecimal targetPrice, BigDecimal targetPnlAmount, boolean stopBreached,
                             String action, List<String> reasons) {}
    public static Evaluation evaluate(Snapshot snapshot,String code,BigDecimal price,LocalDate date,
                                      BigDecimal technicalStop,List<BigDecimal> targets,TradeJudgment judgment) {
        if(snapshot==null) return null;
        Holding h=snapshot.items().get(code);
        String position="UNAVAILABLE".equals(snapshot.status())?"UNKNOWN":h==null?"NOT_HELD":h.quantity().signum()>0?"HELD":"NOT_HELD";
        var reasons=new ArrayList<String>();
        String availability=snapshot.status();
        boolean compatible=price!=null && price.signum()>0 && date!=null && !date.isAfter(snapshot.checkedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDate());
        if(price==null || date==null) {availability="PRICE_UNAVAILABLE";reasons.add("분석 기준가격을 확인할 수 없어 손익과 판단을 보류합니다.");}
        else if(!compatible) {availability="PRICE_AFTER_SNAPSHOT";reasons.add("미래 가격 기준으로는 매매판단을 계산할 수 없습니다.");}
        if(h==null) reasons.add("UNAVAILABLE".equals(snapshot.status())?"보유정보 조회 실패. 미보유로 간주하지 않습니다.":"내 보유종목에 없는 종목이므로 미보유 기준으로 판단합니다.");
        BigDecimal invested=null,value=null,pnl=null,pct=null,stop=null,stopPnl=null,risk=null,target=null,targetPnl=null;
        boolean breached=false;String action="DEFER";
        if(compatible && !"UNKNOWN".equals(position) && judgment!=null) {
            var decision="HELD".equals(position)?judgment.held():judgment.notHeld();
            if(decision!=null){action=decision.action().name();reasons.addAll(decision.reasons());}
        }
        if(compatible && "HELD".equals(position) && price!=null && price.signum()>0) {
            invested=h.averageBuyPrice().multiply(h.quantity());value=price.multiply(h.quantity());pnl=value.subtract(invested);
            pct=pnl.multiply(BigDecimal.valueOf(100)).divide(invested,2,RoundingMode.HALF_UP);
            stop=technicalStop;
            target=targets==null||targets.isEmpty()?null:targets.get(0);
            if(stop!=null) {stopPnl=stop.subtract(h.averageBuyPrice()).multiply(h.quantity());breached=price.compareTo(stop)<=0;
                risk=price.subtract(stop).max(BigDecimal.ZERO).multiply(h.quantity());
                if(breached){if(!Set.of("EXIT_TRIGGERED","SELL_PRIORITY").contains(action))action="REVIEW_HOLDING";reasons.add("분석 가격이 손절 참고선 이하입니다. 실제 체결 또는 과거 접촉 판정은 아닙니다.");}}
            if(!breached && "HOLD".equals(action) && judgment!=null && judgment.newEntry()!=null
                    && judgment.newEntry().status()==com.example.stockanalysis.judgment.NewEntryAssessment.Status.ELIGIBLE) {
                action=price.compareTo(h.averageBuyPrice())<0?"ADD_AVERAGING_DOWN":price.compareTo(h.averageBuyPrice())>0?"ADD_PYRAMID":"ADD_REVIEW";
                reasons.addAll(judgment.newEntry().reasons());
                reasons.add("추세 유지와 공통 신규 진입 조건이 함께 충족되어 추가매수를 검토합니다. 손익 여부만으로 결정한 판단이 아닙니다.");
                reasons.add("전체 자산·가용 현금·종목 비중이 없어 추가매수 수량은 산정하지 않습니다.");
            }
            if("HOLD".equals(action)) reasons.add("보유 추세는 유지되지만 추가매수 조건은 충족되지 않았습니다.");
            if(target!=null) targetPnl=target.subtract(h.averageBuyPrice()).multiply(h.quantity());
            reasons.add("수수료·세금·체결 차이 제외. 현재 등록 잔고를 표시된 분석 기준가격으로 평가합니다. 과거 실제 잔고나 실시간 체결 가격을 뜻하지 않습니다.");
        }
        return new Evaluation(position,availability,snapshot.checkedAt(),h,date,price,invested,value,pnl,pct,stop,stopPnl,risk,target,targetPnl,breached,action,List.copyOf(reasons));
    }
}
