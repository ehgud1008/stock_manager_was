package com.example.stockanalysis.kiwoom.contract.tr.kt50032;

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
 * 금현물 거래내역조회(kt50032) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt50032Response implements KiwoomResult {

    @JsonProperty("acnt_print")
    private String acntPrint; // 계좌번호

    @JsonProperty("gold_trde_hist")
    private List<GoldTrdeHistItem> goldTrdeHist; // 금현물거래내역

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
    public static class GoldTrdeHistItem {

        @JsonProperty("deal_dt")
        private String dealDt; // 거래일자

        @JsonProperty("deal_no")
        private String dealNo; // 거래번호

        @JsonProperty("rmrk_nm")
        private String rmrkNm; // 적요명

        @JsonProperty("deal_qty")
        private String dealQty; // 거래수량

        @JsonProperty("gold_spot_vat")
        private String goldSpotVat; // 금현물부가가치세

        @JsonProperty("exct_amt")
        private String exctAmt; // 정산금액

        @JsonProperty("dly_sum")
        private String dlySum; // 연체합

        @JsonProperty("entra_remn")
        private String entraRemn; // 예수금잔고

        @JsonProperty("mdia_nm")
        private String mdiaNm; // 메체구분명

        @JsonProperty("orig_deal_no")
        private String origDealNo; // 원거래번호

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("uv_exrt")
        private String uvExrt; // 거래단가

        @JsonProperty("cmsn")
        private String cmsn; // 수수료

        @JsonProperty("uncl_ocr")
        private String unclOcr; // 미수(원/g)

        @JsonProperty("rpym_sum")
        private String rpymSum; // 변제합

        @JsonProperty("spot_remn")
        private String spotRemn; // 현물잔고

        @JsonProperty("proc_time")
        private String procTime; // 처리시간

        @JsonProperty("rcpy_no")
        private String rcpyNo; // 출납번호

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("deal_amt")
        private String dealAmt; // 거래금액

        @JsonProperty("tax_tot_amt")
        private String taxTotAmt; // 소득/주민세

        @JsonProperty("cntr_dt")
        private String cntrDt; // 체결일

        @JsonProperty("proc_brch_nm")
        private String procBrchNm; // 처리점

        @JsonProperty("prcsr")
        private String prcsr; // 처리자
    }
}
