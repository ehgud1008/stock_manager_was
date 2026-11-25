package com.example.stockanalysis.decision;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import io.swagger.v3.oas.annotations.media.Schema;

public final class AccountRiskModels {
    private AccountRiskModels() {}
    public record Edit(
            @Schema(description="계좌 총평가액 (원), 현금 포함") @NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal totalAssets,
            @Schema(description="매수에 사용할 수 있는 현금 (원)") @NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal availableCash,
            @Schema(description="한 거래의 계획상 최대 허용 손실액 (원)") @NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal maxLossPerTrade,
            @Schema(description="종목별 최대 비중 (%), 0이면 신규 매수 중지") @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal maxPositionPercent,
            @NotNull LocalDate balanceDate,
            @Schema(description="신규는 0, 수정은 조회한 revision") @Min(0) long expectedRevision) {}
    public record Settings(BigDecimal totalAssets,BigDecimal availableCash,BigDecimal maxLossPerTrade,
                           BigDecimal maxPositionPercent,LocalDate balanceDate,long revision,Instant updatedAt) {}
    public record View(String status,Settings settings,Instant checkedAt,String message) {}
}
