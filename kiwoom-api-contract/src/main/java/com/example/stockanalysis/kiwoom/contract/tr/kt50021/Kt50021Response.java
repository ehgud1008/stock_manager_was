package com.example.stockanalysis.kiwoom.contract.tr.kt50021;

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
 * 금현물 예수금(kt50021) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt50021Response implements KiwoomResult {

    @JsonProperty("entra")
    private String entra; // 예수금

    @JsonProperty("profa_ch")
    private String profaCh; // 증거금현금

    @JsonProperty("chck_ina_amt")
    private String chckInaAmt; // 수표입금액

    @JsonProperty("etc_loan")
    private String etcLoan; // 기타대여금

    @JsonProperty("etc_loan_dlfe")
    private String etcLoanDlfe; // 기타대여금연체료

    @JsonProperty("etc_loan_tot")
    private String etcLoanTot; // 기타대여금합계

    @JsonProperty("prsm_entra")
    private String prsmEntra; // 추정예수금

    @JsonProperty("buy_exct_amt")
    private String buyExctAmt; // 매수정산금

    @JsonProperty("sell_exct_amt")
    private String sellExctAmt; // 매도정산금

    @JsonProperty("sell_buy_exct_amt")
    private String sellBuyExctAmt; // 매도매수정산금

    @JsonProperty("dly_amt")
    private String dlyAmt; // 미수변제소요금

    @JsonProperty("prsm_pymn_alow_amt")
    private String prsmPymnAlowAmt; // 추정출금가능금액

    @JsonProperty("pymn_alow_amt")
    private String pymnAlowAmt; // 출금가능금액

    @JsonProperty("ord_alow_amt")
    private String ordAlowAmt; // 주문가능금액

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
