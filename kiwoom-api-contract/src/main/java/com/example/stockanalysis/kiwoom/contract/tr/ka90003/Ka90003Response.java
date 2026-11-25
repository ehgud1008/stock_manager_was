package com.example.stockanalysis.kiwoom.contract.tr.ka90003;

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
 * 프로그램순매수상위50요청(ka90003) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka90003Response implements KiwoomResult {

    @JsonProperty("prm_netprps_upper_50")
    private List<PrmNetprpsUpper50Item> prmNetprpsUpper50; // 프로그램순매수상위50

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
    public static class PrmNetprpsUpper50Item {

        @JsonProperty("rank")
        private String rank; // 순위

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("flu_sig")
        private String fluSig; // 등락기호

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("flu_rt")
        private String fluRt; // 등락율

        @JsonProperty("acc_trde_qty")
        private String accTrdeQty; // 누적거래량

        @JsonProperty("prm_sell_amt")
        private String prmSellAmt; // 프로그램매도금액

        @JsonProperty("prm_buy_amt")
        private String prmBuyAmt; // 프로그램매수금액

        @JsonProperty("prm_netprps_amt")
        private String prmNetprpsAmt; // 프로그램순매수금액
    }
}
