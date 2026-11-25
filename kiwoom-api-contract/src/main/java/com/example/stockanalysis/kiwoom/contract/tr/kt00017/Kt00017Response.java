package com.example.stockanalysis.kiwoom.contract.tr.kt00017;

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
 * 계좌별당일현황요청(kt00017) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt00017Response implements KiwoomResult {

    @JsonProperty("d2_entra")
    private String d2Entra; // D+2추정예수금

    @JsonProperty("crd_int_npay_gold")
    private String crdIntNpayGold; // 신용이자미납금

    @JsonProperty("etc_loana")
    private String etcLoana; // 기타대여금

    @JsonProperty("gnrl_stk_evlt_amt_d2")
    private String gnrlStkEvltAmtD2; // 일반주식평가금액D+2

    @JsonProperty("dpst_grnt_use_amt_d2")
    private String dpstGrntUseAmtD2; // 예탁담보대출금D+2

    @JsonProperty("crd_stk_evlt_amt_d2")
    private String crdStkEvltAmtD2; // 예탁담보주식평가금액D+2

    @JsonProperty("crd_loan_d2")
    private String crdLoanD2; // 신용융자금D+2

    @JsonProperty("crd_loan_evlta_d2")
    private String crdLoanEvltaD2; // 신용융자평가금D+2

    @JsonProperty("crd_ls_grnt_d2")
    private String crdLsGrntD2; // 신용대주담보금D+2

    @JsonProperty("crd_ls_evlta_d2")
    private String crdLsEvltaD2; // 신용대주평가금D+2

    @JsonProperty("ina_amt")
    private String inaAmt; // 입금금액

    @JsonProperty("outa")
    private String outa; // 출금금액

    @JsonProperty("inq_amt")
    private String inqAmt; // 입고금액

    @JsonProperty("outq_amt")
    private String outqAmt; // 출고금액

    @JsonProperty("sell_amt")
    private String sellAmt; // 매도금액

    @JsonProperty("buy_amt")
    private String buyAmt; // 매수금액

    @JsonProperty("cmsn")
    private String cmsn; // 수수료

    @JsonProperty("tax")
    private String tax; // 세금

    @JsonProperty("stk_pur_cptal_loan_amt")
    private String stkPurCptalLoanAmt; // 주식매입자금대출금

    @JsonProperty("rp_evlt_amt")
    private String rpEvltAmt; // RP평가금액

    @JsonProperty("bd_evlt_amt")
    private String bdEvltAmt; // 채권평가금액

    @JsonProperty("elsevlt_amt")
    private String elsevltAmt; // ELS평가금액

    @JsonProperty("crd_int_amt")
    private String crdIntAmt; // 신용이자금액

    @JsonProperty("sel_prica_grnt_loan_int_amt_amt")
    private String selPricaGrntLoanIntAmtAmt; // 매도대금담보대출이자금액

    @JsonProperty("dvida_amt")
    private String dvidaAmt; // 배당금액

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
