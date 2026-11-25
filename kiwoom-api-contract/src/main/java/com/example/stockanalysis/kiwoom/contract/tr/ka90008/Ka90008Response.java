package com.example.stockanalysis.kiwoom.contract.tr.ka90008;

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
 * 종목시간별프로그램매매추이요청(ka90008) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka90008Response implements KiwoomResult {

    @JsonProperty("stk_tm_prm_trde_trnsn")
    private List<StkTmPrmTrdeTrnsnItem> stkTmPrmTrdeTrnsn; // 종목시간별프로그램매매추이

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
    public static class StkTmPrmTrdeTrnsnItem {

        @JsonProperty("tm")
        private String tm; // 시간

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("pre_sig")
        private String preSig; // 대비기호

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("flu_rt")
        private String fluRt; // 등락율

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("prm_sell_amt")
        private String prmSellAmt; // 프로그램매도금액

        @JsonProperty("prm_buy_amt")
        private String prmBuyAmt; // 프로그램매수금액

        @JsonProperty("prm_netprps_amt")
        private String prmNetprpsAmt; // 프로그램순매수금액

        @JsonProperty("prm_netprps_amt_irds")
        private String prmNetprpsAmtIrds; // 프로그램순매수금액증감

        @JsonProperty("prm_sell_qty")
        private String prmSellQty; // 프로그램매도수량

        @JsonProperty("prm_buy_qty")
        private String prmBuyQty; // 프로그램매수수량

        @JsonProperty("prm_netprps_qty")
        private String prmNetprpsQty; // 프로그램순매수수량

        @JsonProperty("prm_netprps_qty_irds")
        private String prmNetprpsQtyIrds; // 프로그램순매수수량증감

        @JsonProperty("base_pric_tm")
        private String basePricTm; // 기준가시간

        @JsonProperty("dbrt_trde_rpy_sum")
        private String dbrtTrdeRpySum; // 대차거래상환주수합

        @JsonProperty("remn_rcvord_sum")
        private String remnRcvordSum; // 잔고수주합

        @JsonProperty("stex_tp")
        private String stexTp; // 거래소구분
    }
}
