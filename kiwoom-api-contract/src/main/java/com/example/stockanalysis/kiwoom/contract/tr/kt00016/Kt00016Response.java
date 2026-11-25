package com.example.stockanalysis.kiwoom.contract.tr.kt00016;

import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 일별계좌수익률상세현황요청(kt00016) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt00016Response implements KiwoomResult {

    @JsonProperty("mang_empno")
    private String mangEmpno; // 관리사원번호

    @JsonProperty("mngr_nm")
    private String mngrNm; // 관리자명

    @JsonProperty("dept_nm")
    private String deptNm; // 관리자지점

    @JsonProperty("entr_fr")
    private String entrFr; // 예수금_초

    @JsonProperty("entr_to")
    private String entrTo; // 예수금_말

    @JsonProperty("scrt_evlt_amt_fr")
    private String scrtEvltAmtFr; // 유가증권평가금액_초

    @JsonProperty("scrt_evlt_amt_to")
    private String scrtEvltAmtTo; // 유가증권평가금액_말

    @JsonProperty("ls_grnt_fr")
    private String lsGrntFr; // 대주담보금_초

    @JsonProperty("ls_grnt_to")
    private String lsGrntTo; // 대주담보금_말

    @JsonProperty("crd_loan_fr")
    private String crdLoanFr; // 신용융자금_초

    @JsonProperty("crd_loan_to")
    private String crdLoanTo; // 신용융자금_말

    @JsonProperty("ch_uncla_fr")
    private String chUnclaFr; // 현금미수금_초

    @JsonProperty("ch_uncla_to")
    private String chUnclaTo; // 현금미수금_말

    @JsonProperty("krw_asgna_fr")
    private String krwAsgnaFr; // 원화대용금_초

    @JsonProperty("krw_asgna_to")
    private String krwAsgnaTo; // 원화대용금_말

    @JsonProperty("ls_evlta_fr")
    private String lsEvltaFr; // 대주평가금_초

    @JsonProperty("ls_evlta_to")
    private String lsEvltaTo; // 대주평가금_말

    @JsonProperty("rght_evlta_fr")
    private String rghtEvltaFr; // 권리평가금_초

    @JsonProperty("rght_evlta_to")
    private String rghtEvltaTo; // 권리평가금_말

    @JsonProperty("loan_amt_fr")
    private String loanAmtFr; // 대출금_초

    @JsonProperty("loan_amt_to")
    private String loanAmtTo; // 대출금_말

    @JsonProperty("etc_loana_fr")
    private String etcLoanaFr; // 기타대여금_초

    @JsonProperty("etc_loana_to")
    private String etcLoanaTo; // 기타대여금_말

    @JsonProperty("crd_int_npay_gold_fr")
    private String crdIntNpayGoldFr; // 신용이자미납금_초

    @JsonProperty("crd_int_npay_gold_to")
    private String crdIntNpayGoldTo; // 신용이자미납금_말

    @JsonProperty("crd_int_fr")
    private String crdIntFr; // 신용이자_초

    @JsonProperty("crd_int_to")
    private String crdIntTo; // 신용이자_말

    @JsonProperty("tot_amt_fr")
    private String totAmtFr; // 순자산액계_초

    @JsonProperty("tot_amt_to")
    private String totAmtTo; // 순자산액계_말

    @JsonProperty("invt_bsamt")
    private String invtBsamt; // 투자원금평잔

    @JsonProperty("evltv_prft")
    private String evltvPrft; // 평가손익

    @JsonProperty("prft_rt")
    private String prftRt; // 수익률

    @JsonProperty("tern_rt")
    private String ternRt; // 회전율

    @JsonProperty("termin_tot_trns")
    private String terminTotTrns; // 기간내총입금

    @JsonProperty("termin_tot_pymn")
    private String terminTotPymn; // 기간내총출금

    @JsonProperty("termin_tot_inq")
    private String terminTotInq; // 기간내총입고

    @JsonProperty("termin_tot_outq")
    private String terminTotOutq; // 기간내총출고

    @JsonProperty("futr_repl_sella")
    private String futrReplSella; // 선물대용매도금액

    @JsonProperty("trst_repl_sella")
    private String trstReplSella; // 위탁대용매도금액

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
