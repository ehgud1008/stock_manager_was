package com.example.stockanalysis.kiwoom.contract.tr.ka52301;

import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 금현물투자자현황(ka52301) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka52301Response implements KiwoomResult {

    @JsonProperty("inve_trad_stat")
    private List<InveTradStatItem> inveTradStat; // 금현물투자자현황

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지


    @Getter
    @Setter
    @Builder
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InveTradStatItem {

        @JsonProperty("all_dfrt_trst_sell_qty")
        private String allDfrtTrstSellQty; // 투자자별 매도 수량(천)

        @JsonProperty("sell_qty_irds")
        private String sellQtyIrds; // 투자자별 매도 수량 증감(천)

        @JsonProperty("all_dfrt_trst_sell_amt")
        private String allDfrtTrstSellAmt; // 투자자별 매도 금액(억)

        @JsonProperty("sell_amt_irds")
        private String sellAmtIrds; // 투자자별 매도 금액 증감(억)

        @JsonProperty("all_dfrt_trst_buy_qty")
        private String allDfrtTrstBuyQty; // 투자자별 매수 수량(천)

        @JsonProperty("buy_qty_irds")
        private String buyQtyIrds; // 투자자별 매수 수량 증감(천)

        @JsonProperty("all_dfrt_trst_buy_amt")
        private String allDfrtTrstBuyAmt; // 투자자별 매수 금액(억)

        @JsonProperty("buy_amt_irds")
        private String buyAmtIrds; // 투자자별 매수 금액 증감(억)

        @JsonProperty("all_dfrt_trst_netprps_qty")
        private String allDfrtTrstNetprpsQty; // 투자자별 순매수 수량(천)

        @JsonProperty("netprps_qty_irds")
        private String netprpsQtyIrds; // 투자자별 순매수 수량 증감(천)

        @JsonProperty("all_dfrt_trst_netprps_amt")
        private String allDfrtTrstNetprpsAmt; // 투자자별 순매수 금액(억)

        @JsonProperty("netprps_amt_irds")
        private String netprpsAmtIrds; // 투자자별 순매수 금액 증감(억)

        @JsonProperty("sell_uv")
        private String sellUv; // 투자자별 매도 단가

        @JsonProperty("buy_uv")
        private String buyUv; // 투자자별 매수 단가

        @JsonProperty("stk_nm")
        private String stkNm; // 투자자 구분명

        @JsonProperty("acc_netprps_amt")
        private String accNetprpsAmt; // 누적 순매수 금액(억)

        @JsonProperty("acc_netprps_qty")
        private String accNetprpsQty; // 누적 순매수 수량(천)

        @JsonProperty("stk_cd")
        private String stkCd; // 투자자 코드
    }
}
