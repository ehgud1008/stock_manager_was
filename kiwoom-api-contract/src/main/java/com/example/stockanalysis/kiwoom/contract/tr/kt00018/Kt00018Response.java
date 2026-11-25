package com.example.stockanalysis.kiwoom.contract.tr.kt00018;

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
 * 계좌평가잔고내역요청(kt00018) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt00018Response implements KiwoomResult {

    @JsonProperty("tot_pur_amt")
    private String totPurAmt; // 총매입금액

    @JsonProperty("tot_evlt_amt")
    private String totEvltAmt; // 총평가금액

    @JsonProperty("tot_evlt_pl")
    private String totEvltPl; // 총평가손익금액

    @JsonProperty("tot_prft_rt")
    private String totPrftRt; // 총수익률(%)

    @JsonProperty("prsm_dpst_aset_amt")
    private String prsmDpstAsetAmt; // 추정예탁자산

    @JsonProperty("tot_loan_amt")
    private String totLoanAmt; // 총대출금

    @JsonProperty("tot_crd_loan_amt")
    private String totCrdLoanAmt; // 총융자금액

    @JsonProperty("tot_crd_ls_amt")
    private String totCrdLsAmt; // 총대주금액

    @JsonProperty("acnt_evlt_remn_indv_tot")
    private List<AcntEvltRemnIndvTotItem> acntEvltRemnIndvTot; // 계좌평가잔고개별합산

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
    public static class AcntEvltRemnIndvTotItem {

        @JsonProperty("stk_cd")
        private String stkCd; // 종목번호

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("evltv_prft")
        private String evltvPrft; // 평가손익

        @JsonProperty("prft_rt")
        private String prftRt; // 수익률(%)

        @JsonProperty("pur_pric")
        private String purPric; // 매입가

        @JsonProperty("pred_close_pric")
        private String predClosePric; // 전일종가

        @JsonProperty("rmnd_qty")
        private String rmndQty; // 보유수량

        @JsonProperty("trde_able_qty")
        private String trdeAbleQty; // 매매가능수량

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("pred_buyq")
        private String predBuyq; // 전일매수수량

        @JsonProperty("pred_sellq")
        private String predSellq; // 전일매도수량

        @JsonProperty("tdy_buyq")
        private String tdyBuyq; // 금일매수수량

        @JsonProperty("tdy_sellq")
        private String tdySellq; // 금일매도수량

        @JsonProperty("pur_amt")
        private String purAmt; // 매입금액

        @JsonProperty("pur_cmsn")
        private String purCmsn; // 매입수수료

        @JsonProperty("evlt_amt")
        private String evltAmt; // 평가금액

        @JsonProperty("sell_cmsn")
        private String sellCmsn; // 평가수수료

        @JsonProperty("tax")
        private String tax; // 세금

        @JsonProperty("sum_cmsn")
        private String sumCmsn; // 수수료합

        @JsonProperty("poss_rt")
        private String possRt; // 보유비중(%)

        @JsonProperty("crd_tp")
        private String crdTp; // 신용구분

        @JsonProperty("crd_tp_nm")
        private String crdTpNm; // 신용구분명

        @JsonProperty("crd_loan_dt")
        private String crdLoanDt; // 대출일
    }
}
