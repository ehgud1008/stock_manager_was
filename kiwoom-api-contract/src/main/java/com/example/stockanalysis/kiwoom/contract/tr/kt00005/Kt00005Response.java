package com.example.stockanalysis.kiwoom.contract.tr.kt00005;

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
 * 체결잔고요청(kt00005) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt00005Response implements KiwoomResult {

    @JsonProperty("entr")
    private String entr; // 예수금

    @JsonProperty("entr_d1")
    private String entrD1; // 예수금D+1

    @JsonProperty("entr_d2")
    private String entrD2; // 예수금D+2

    @JsonProperty("pymn_alow_amt")
    private String pymnAlowAmt; // 출금가능금액

    @JsonProperty("uncl_stk_amt")
    private String unclStkAmt; // 미수확보금

    @JsonProperty("repl_amt")
    private String replAmt; // 대용금

    @JsonProperty("rght_repl_amt")
    private String rghtReplAmt; // 권리대용금

    @JsonProperty("ord_alowa")
    private String ordAlowa; // 주문가능현금

    @JsonProperty("ch_uncla")
    private String chUncla; // 현금미수금

    @JsonProperty("crd_int_npay_gold")
    private String crdIntNpayGold; // 신용이자미납금

    @JsonProperty("etc_loana")
    private String etcLoana; // 기타대여금

    @JsonProperty("nrpy_loan")
    private String nrpyLoan; // 미상환융자금

    @JsonProperty("profa_ch")
    private String profaCh; // 증거금현금

    @JsonProperty("repl_profa")
    private String replProfa; // 증거금대용

    @JsonProperty("stk_buy_tot_amt")
    private String stkBuyTotAmt; // 주식매수총액

    @JsonProperty("evlt_amt_tot")
    private String evltAmtTot; // 평가금액합계

    @JsonProperty("tot_pl_tot")
    private String totPlTot; // 총손익합계

    @JsonProperty("tot_pl_rt")
    private String totPlRt; // 총손익률

    @JsonProperty("tot_re_buy_alowa")
    private String totReBuyAlowa; // 총재매수가능금액

    @JsonProperty("20ord_alow_amt")
    private String _20ordAlowAmt; // 20%주문가능금액

    @JsonProperty("30ord_alow_amt")
    private String _30ordAlowAmt; // 30%주문가능금액

    @JsonProperty("40ord_alow_amt")
    private String _40ordAlowAmt; // 40%주문가능금액

    @JsonProperty("50ord_alow_amt")
    private String _50ordAlowAmt; // 50%주문가능금액

    @JsonProperty("60ord_alow_amt")
    private String _60ordAlowAmt; // 60%주문가능금액

    @JsonProperty("100ord_alow_amt")
    private String _100ordAlowAmt; // 100%주문가능금액

    @JsonProperty("crd_loan_tot")
    private String crdLoanTot; // 신용융자합계

    @JsonProperty("crd_loan_ls_tot")
    private String crdLoanLsTot; // 신용융자대주합계

    @JsonProperty("crd_grnt_rt")
    private String crdGrntRt; // 신용담보비율

    @JsonProperty("dpst_grnt_use_amt_amt")
    private String dpstGrntUseAmtAmt; // 예탁담보대출금액

    @JsonProperty("grnt_loan_amt")
    private String grntLoanAmt; // 매도담보대출금액

    @JsonProperty("stk_cntr_remn")
    private List<StkCntrRemnItem> stkCntrRemn; // 종목별체결잔고

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
    public static class StkCntrRemnItem {

        @JsonProperty("crd_tp")
        private String crdTp; // 신용구분

        @JsonProperty("loan_dt")
        private String loanDt; // 대출일

        @JsonProperty("expr_dt")
        private String exprDt; // 만기일

        @JsonProperty("stk_cd")
        private String stkCd; // 종목번호

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("setl_remn")
        private String setlRemn; // 결제잔고

        @JsonProperty("cur_qty")
        private String curQty; // 현재잔고

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("buy_uv")
        private String buyUv; // 매입단가

        @JsonProperty("pur_amt")
        private String purAmt; // 매입금액

        @JsonProperty("evlt_amt")
        private String evltAmt; // 평가금액

        @JsonProperty("evltv_prft")
        private String evltvPrft; // 평가손익

        @JsonProperty("pl_rt")
        private String plRt; // 손익률
    }
}
