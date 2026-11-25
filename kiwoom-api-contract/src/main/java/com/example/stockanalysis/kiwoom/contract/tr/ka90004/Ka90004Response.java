package com.example.stockanalysis.kiwoom.contract.tr.ka90004;

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
 * 종목별프로그램매매현황요청(ka90004) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka90004Response implements KiwoomResult {

    @JsonProperty("tot_1")
    private String tot1; // 매수체결수량합계

    @JsonProperty("tot_2")
    private String tot2; // 매수체결금액합계

    @JsonProperty("tot_3")
    private String tot3; // 매도체결수량합계

    @JsonProperty("tot_4")
    private String tot4; // 매도체결금액합계

    @JsonProperty("tot_5")
    private String tot5; // 순매수대금합계

    @JsonProperty("tot_6")
    private String tot6; // 합계6

    @JsonProperty("stk_prm_trde_prst")
    private List<StkPrmTrdePrstItem> stkPrmTrdePrst; // 종목별프로그램매매현황

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
    public static class StkPrmTrdePrstItem {

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

        @JsonProperty("buy_cntr_qty")
        private String buyCntrQty; // 매수체결수량

        @JsonProperty("buy_cntr_amt")
        private String buyCntrAmt; // 매수체결금액

        @JsonProperty("sel_cntr_qty")
        private String selCntrQty; // 매도체결수량

        @JsonProperty("sel_cntr_amt")
        private String selCntrAmt; // 매도체결금액

        @JsonProperty("netprps_prica")
        private String netprpsPrica; // 순매수대금

        @JsonProperty("all_trde_rt")
        private String allTrdeRt; // 전체거래비율
    }
}
