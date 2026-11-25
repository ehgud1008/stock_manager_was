package com.example.stockanalysis.kiwoom.contract.tr.kt00004;

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
 * 계좌평가현황요청(kt00004) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt00004Response implements KiwoomResult {

    @JsonProperty("acnt_nm")
    private String acntNm; // 계좌명

    @JsonProperty("brch_nm")
    private String brchNm; // 지점명

    @JsonProperty("entr")
    private String entr; // 예수금

    @JsonProperty("d2_entra")
    private String d2Entra; // D+2추정예수금

    @JsonProperty("tot_est_amt")
    private String totEstAmt; // 유가잔고평가액

    @JsonProperty("aset_evlt_amt")
    private String asetEvltAmt; // 예탁자산평가액

    @JsonProperty("tot_pur_amt")
    private String totPurAmt; // 총매입금액

    @JsonProperty("prsm_dpst_aset_amt")
    private String prsmDpstAsetAmt; // 추정예탁자산

    @JsonProperty("tot_grnt_sella")
    private String totGrntSella; // 매도담보대출금

    @JsonProperty("tdy_lspft_amt")
    private String tdyLspftAmt; // 당일투자원금

    @JsonProperty("invt_bsamt")
    private String invtBsamt; // 당월투자원금

    @JsonProperty("lspft_amt")
    private String lspftAmt; // 누적투자원금

    @JsonProperty("tdy_lspft")
    private String tdyLspft; // 당일투자손익

    @JsonProperty("lspft2")
    private String lspft2; // 당월투자손익

    @JsonProperty("lspft")
    private String lspft; // 누적투자손익

    @JsonProperty("tdy_lspft_rt")
    private String tdyLspftRt; // 당일손익율

    @JsonProperty("lspft_ratio")
    private String lspftRatio; // 당월손익율

    @JsonProperty("lspft_rt")
    private String lspftRt; // 누적손익율

    @JsonProperty("stk_acnt_evlt_prst")
    private List<StkAcntEvltPrstItem> stkAcntEvltPrst; // 종목별계좌평가현황

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
    public static class StkAcntEvltPrstItem {

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("rmnd_qty")
        private String rmndQty; // 보유수량

        @JsonProperty("avg_prc")
        private String avgPrc; // 평균단가

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("evlt_amt")
        private String evltAmt; // 평가금액

        @JsonProperty("pl_amt")
        private String plAmt; // 손익금액

        @JsonProperty("pl_rt")
        private String plRt; // 손익율

        @JsonProperty("loan_dt")
        private String loanDt; // 대출일

        @JsonProperty("pur_amt")
        private String purAmt; // 매입금액

        @JsonProperty("setl_remn")
        private String setlRemn; // 결제잔고

        @JsonProperty("pred_buyq")
        private String predBuyq; // 전일매수수량

        @JsonProperty("pred_sellq")
        private String predSellq; // 전일매도수량

        @JsonProperty("tdy_buyq")
        private String tdyBuyq; // 금일매수수량

        @JsonProperty("tdy_sellq")
        private String tdySellq; // 금일매도수량
    }
}
