package com.example.stockanalysis.kiwoom.contract.tr.ka10063;

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
 * 장중투자자별매매요청(ka10063) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10063Response implements KiwoomResult {

    @JsonProperty("opmr_invsr_trde")
    private List<OpmrInvsrTrdeItem> opmrInvsrTrde; // 장중투자자별매매

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
    public static class OpmrInvsrTrdeItem {

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("pre_sig")
        private String preSig; // 대비기호

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("flu_rt")
        private String fluRt; // 등락율

        @JsonProperty("acc_trde_qty")
        private String accTrdeQty; // 누적거래량

        @JsonProperty("netprps_amt")
        private String netprpsAmt; // 순매수금액

        @JsonProperty("prev_netprps_amt")
        private String prevNetprpsAmt; // 이전순매수금액

        @JsonProperty("buy_amt")
        private String buyAmt; // 매수금액

        @JsonProperty("netprps_amt_irds")
        private String netprpsAmtIrds; // 순매수금액증감

        @JsonProperty("buy_amt_irds")
        private String buyAmtIrds; // 매수금액증감

        @JsonProperty("sell_amt")
        private String sellAmt; // 매도금액

        @JsonProperty("sell_amt_irds")
        private String sellAmtIrds; // 매도금액증감

        @JsonProperty("netprps_qty")
        private String netprpsQty; // 순매수수량

        @JsonProperty("prev_pot_netprps_qty")
        private String prevPotNetprpsQty; // 이전시점순매수수량

        @JsonProperty("netprps_irds")
        private String netprpsIrds; // 순매수증감

        @JsonProperty("buy_qty")
        private String buyQty; // 매수수량

        @JsonProperty("buy_qty_irds")
        private String buyQtyIrds; // 매수수량증감

        @JsonProperty("sell_qty")
        private String sellQty; // 매도수량

        @JsonProperty("sell_qty_irds")
        private String sellQtyIrds; // 매도수량증감
    }
}
