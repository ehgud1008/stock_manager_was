package com.example.stockanalysis.kiwoom.contract.tr.kt00008;

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
 * 계좌별익일결제예정내역요청(kt00008) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt00008Response implements KiwoomResult {

    @JsonProperty("trde_dt")
    private String trdeDt; // 매매일자

    @JsonProperty("setl_dt")
    private String setlDt; // 결제일자

    @JsonProperty("sell_amt_sum")
    private String sellAmtSum; // 매도정산합

    @JsonProperty("buy_amt_sum")
    private String buyAmtSum; // 매수정산합

    @JsonProperty("acnt_nxdy_setl_frcs_prps_array")
    private List<AcntNxdySetlFrcsPrpsArrayItem> acntNxdySetlFrcsPrpsArray; // 계좌별익일결제예정내역배열

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
    public static class AcntNxdySetlFrcsPrpsArrayItem {

        @JsonProperty("seq")
        private String seq; // 일련번호

        @JsonProperty("stk_cd")
        private String stkCd; // 종목번호

        @JsonProperty("loan_dt")
        private String loanDt; // 대출일

        @JsonProperty("qty")
        private String qty; // 수량

        @JsonProperty("engg_amt")
        private String enggAmt; // 약정금액

        @JsonProperty("cmsn")
        private String cmsn; // 수수료

        @JsonProperty("incm_tax")
        private String incmTax; // 소득세

        @JsonProperty("rstx")
        private String rstx; // 농특세

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("sell_tp")
        private String sellTp; // 매도수구분

        @JsonProperty("unp")
        private String unp; // 단가

        @JsonProperty("exct_amt")
        private String exctAmt; // 정산금액

        @JsonProperty("trde_tax")
        private String trdeTax; // 거래세

        @JsonProperty("resi_tax")
        private String resiTax; // 주민세

        @JsonProperty("crd_tp")
        private String crdTp; // 신용구분
    }
}
