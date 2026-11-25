package com.example.stockanalysis.kiwoom.contract.tr.kt00001;

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
 * 예수금상세현황요청(kt00001) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt00001Response implements KiwoomResult {

    @JsonProperty("entr")
    private String entr; // 예수금

    @JsonProperty("profa_ch")
    private String profaCh; // 주식증거금현금

    @JsonProperty("bncr_profa_ch")
    private String bncrProfaCh; // 수익증권증거금현금

    @JsonProperty("nxdy_bncr_sell_exct")
    private String nxdyBncrSellExct; // 익일수익증권매도정산대금

    @JsonProperty("fc_stk_krw_repl_set_amt")
    private String fcStkKrwReplSetAmt; // 해외주식원화대용설정금

    @JsonProperty("crd_grnta_ch")
    private String crdGrntaCh; // 신용보증금현금

    @JsonProperty("crd_grnt_ch")
    private String crdGrntCh; // 신용담보금현금

    @JsonProperty("add_grnt_ch")
    private String addGrntCh; // 추가담보금현금

    @JsonProperty("etc_profa")
    private String etcProfa; // 기타증거금

    @JsonProperty("uncl_stk_amt")
    private String unclStkAmt; // 미수확보금

    @JsonProperty("shrts_prica")
    private String shrtsPrica; // 공매도대금

    @JsonProperty("crd_set_grnta")
    private String crdSetGrnta; // 신용설정평가금

    @JsonProperty("chck_ina_amt")
    private String chckInaAmt; // 수표입금액

    @JsonProperty("etc_chck_ina_amt")
    private String etcChckInaAmt; // 기타수표입금액

    @JsonProperty("crd_grnt_ruse")
    private String crdGrntRuse; // 신용담보재사용

    @JsonProperty("knx_asset_evltv")
    private String knxAssetEvltv; // 코넥스기본예탁금

    @JsonProperty("elwdpst_evlta")
    private String elwdpstEvlta; // ELW예탁평가금

    @JsonProperty("crd_ls_rght_frcs_amt")
    private String crdLsRghtFrcsAmt; // 신용대주권리예정금액

    @JsonProperty("lvlh_join_amt")
    private String lvlhJoinAmt; // 생계형가입금액

    @JsonProperty("lvlh_trns_alowa")
    private String lvlhTrnsAlowa; // 생계형입금가능금액

    @JsonProperty("repl_amt")
    private String replAmt; // 대용금평가금액(합계)

    @JsonProperty("remn_repl_evlta")
    private String remnReplEvlta; // 잔고대용평가금액

    @JsonProperty("trst_remn_repl_evlta")
    private String trstRemnReplEvlta; // 위탁대용잔고평가금액

    @JsonProperty("bncr_remn_repl_evlta")
    private String bncrRemnReplEvlta; // 수익증권대용평가금액

    @JsonProperty("profa_repl")
    private String profaRepl; // 위탁증거금대용

    @JsonProperty("crd_grnta_repl")
    private String crdGrntaRepl; // 신용보증금대용

    @JsonProperty("crd_grnt_repl")
    private String crdGrntRepl; // 신용담보금대용

    @JsonProperty("add_grnt_repl")
    private String addGrntRepl; // 추가담보금대용

    @JsonProperty("rght_repl_amt")
    private String rghtReplAmt; // 권리대용금

    @JsonProperty("pymn_alow_amt")
    private String pymnAlowAmt; // 출금가능금액

    @JsonProperty("wrap_pymn_alow_amt")
    private String wrapPymnAlowAmt; // 랩출금가능금액

    @JsonProperty("ord_alow_amt")
    private String ordAlowAmt; // 주문가능금액

    @JsonProperty("bncr_buy_alowa")
    private String bncrBuyAlowa; // 수익증권매수가능금액

    @JsonProperty("20stk_ord_alow_amt")
    private String _20stkOrdAlowAmt; // 20%종목주문가능금액

    @JsonProperty("30stk_ord_alow_amt")
    private String _30stkOrdAlowAmt; // 30%종목주문가능금액

    @JsonProperty("40stk_ord_alow_amt")
    private String _40stkOrdAlowAmt; // 40%종목주문가능금액

    @JsonProperty("100stk_ord_alow_amt")
    private String _100stkOrdAlowAmt; // 100%종목주문가능금액

    @JsonProperty("ch_uncla")
    private String chUncla; // 현금미수금

    @JsonProperty("ch_uncla_dlfe")
    private String chUnclaDlfe; // 현금미수연체료

    @JsonProperty("ch_uncla_tot")
    private String chUnclaTot; // 현금미수금합계

    @JsonProperty("crd_int_npay")
    private String crdIntNpay; // 신용이자미납

    @JsonProperty("int_npay_amt_dlfe")
    private String intNpayAmtDlfe; // 신용이자미납연체료

    @JsonProperty("int_npay_amt_tot")
    private String intNpayAmtTot; // 신용이자미납합계

    @JsonProperty("etc_loana")
    private String etcLoana; // 기타대여금

    @JsonProperty("etc_loana_dlfe")
    private String etcLoanaDlfe; // 기타대여금연체료

    @JsonProperty("etc_loan_tot")
    private String etcLoanTot; // 기타대여금합계

    @JsonProperty("nrpy_loan")
    private String nrpyLoan; // 미상환융자금

    @JsonProperty("loan_sum")
    private String loanSum; // 융자금합계

    @JsonProperty("ls_sum")
    private String lsSum; // 대주금합계

    @JsonProperty("crd_grnt_rt")
    private String crdGrntRt; // 신용담보비율

    @JsonProperty("mdstrm_usfe")
    private String mdstrmUsfe; // 중도이용료

    @JsonProperty("min_ord_alow_yn")
    private String minOrdAlowYn; // 최소주문가능금액

    @JsonProperty("loan_remn_evlt_amt")
    private String loanRemnEvltAmt; // 대출총평가금액

    @JsonProperty("dpst_grntl_remn")
    private String dpstGrntlRemn; // 예탁담보대출잔고

    @JsonProperty("sell_grntl_remn")
    private String sellGrntlRemn; // 매도담보대출잔고

    @JsonProperty("d1_entra")
    private String d1Entra; // d+1추정예수금

    @JsonProperty("d1_slby_exct_amt")
    private String d1SlbyExctAmt; // d+1매도매수정산금

    @JsonProperty("d1_buy_exct_amt")
    private String d1BuyExctAmt; // d+1매수정산금

    @JsonProperty("d1_out_rep_mor")
    private String d1OutRepMor; // d+1미수변제소요금

    @JsonProperty("d1_sel_exct_amt")
    private String d1SelExctAmt; // d+1매도정산금

    @JsonProperty("d1_pymn_alow_amt")
    private String d1PymnAlowAmt; // d+1출금가능금액

    @JsonProperty("d2_entra")
    private String d2Entra; // d+2추정예수금

    @JsonProperty("d2_slby_exct_amt")
    private String d2SlbyExctAmt; // d+2매도매수정산금

    @JsonProperty("d2_buy_exct_amt")
    private String d2BuyExctAmt; // d+2매수정산금

    @JsonProperty("d2_out_rep_mor")
    private String d2OutRepMor; // d+2미수변제소요금

    @JsonProperty("d2_sel_exct_amt")
    private String d2SelExctAmt; // d+2매도정산금

    @JsonProperty("d2_pymn_alow_amt")
    private String d2PymnAlowAmt; // d+2출금가능금액

    @JsonProperty("50stk_ord_alow_amt")
    private String _50stkOrdAlowAmt; // 50%종목주문가능금액

    @JsonProperty("60stk_ord_alow_amt")
    private String _60stkOrdAlowAmt; // 60%종목주문가능금액

    @JsonProperty("stk_entr_prst")
    private List<StkEntrPrstItem> stkEntrPrst; // 종목별예수금

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
    public static class StkEntrPrstItem {

        @JsonProperty("crnc_cd")
        private String crncCd; // 통화코드

        @JsonProperty("fx_entr")
        private String fxEntr; // 외화예수금

        @JsonProperty("fc_krw_repl_evlta")
        private String fcKrwReplEvlta; // 원화대용평가금

        @JsonProperty("fc_trst_profa")
        private String fcTrstProfa; // 해외주식증거금

        @JsonProperty("pymn_alow_amt")
        private String pymnAlowAmt; // 출금가능금액

        @JsonProperty("pymn_alow_amt_entr")
        private String pymnAlowAmtEntr; // 출금가능금액(예수금)

        @JsonProperty("ord_alow_amt_entr")
        private String ordAlowAmtEntr; // 주문가능금액(예수금)

        @JsonProperty("fc_uncla")
        private String fcUncla; // 외화미수(합계)

        @JsonProperty("fc_ch_uncla")
        private String fcChUncla; // 외화현금미수금

        @JsonProperty("dly_amt")
        private String dlyAmt; // 연체료

        @JsonProperty("d1_fx_entr")
        private String d1FxEntr; // d+1외화예수금

        @JsonProperty("d2_fx_entr")
        private String d2FxEntr; // d+2외화예수금

        @JsonProperty("d3_fx_entr")
        private String d3FxEntr; // d+3외화예수금

        @JsonProperty("d4_fx_entr")
        private String d4FxEntr; // d+4외화예수금
    }
}
