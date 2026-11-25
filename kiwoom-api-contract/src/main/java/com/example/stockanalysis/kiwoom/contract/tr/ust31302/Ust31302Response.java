package com.example.stockanalysis.kiwoom.contract.tr.ust31302;

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
 * 환전 신청(ust31302) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust31302Response implements KiwoomResult {

    @JsonProperty("sell_aplc_exrt")
    private String sellAplcExrt; // 매도적용환율

    @JsonProperty("buy_aplc_exrt")
    private String buyAplcExrt; // 매수적용환율

    @JsonProperty("aplc_exrt")
    private String aplcExrt; // 적용환율

    @JsonProperty("entra_prerm")
    private String entraPrerm; // 예수금전잔

    @JsonProperty("ch_uncla_prerm")
    private String chUnclaPrerm; // 현금미수금전잔

    @JsonProperty("etc_loana_prerm")
    private String etcLoanaPrerm; // 기타대여금전잔

    @JsonProperty("entra_nowrm")
    private String entraNowrm; // 예수금금잔

    @JsonProperty("ch_uncla_nowrm")
    private String chUnclaNowrm; // 현금미수금금잔

    @JsonProperty("etc_loana_nowrm")
    private String etcLoanaNowrm; // 기타대여금금잔

    @JsonProperty("krw_exmn_alow_amt")
    private String krwExmnAlowAmt; // 원화환전가능금액

    @JsonProperty("ch_uncl_rpym_amt")
    private String chUnclRpymAmt; // 현금미수변제금

    @JsonProperty("ch_uncl_dlfe")
    private String chUnclDlfe; // 현금미수연체료

    @JsonProperty("etc_loan_npay_rpym_amt")
    private String etcLoanNpayRpymAmt; // 기타대여미납변제금

    @JsonProperty("etc_loan_npay_dlfe")
    private String etcLoanNpayDlfe; // 기타대여미납연체료

    @JsonProperty("fc_entra_prerm")
    private String fcEntraPrerm; // 외화예수금전잔

    @JsonProperty("fc_ch_uncla_prerm")
    private String fcChUnclaPrerm; // 외화현금미수금전잔

    @JsonProperty("fc_etc_loana_prerm")
    private String fcEtcLoanaPrerm; // 외화기타대여금전잔

    @JsonProperty("fc_entra_nowrm")
    private String fcEntraNowrm; // 외화예수금금잔

    @JsonProperty("fc_ch_uncla_nowrm")
    private String fcChUnclaNowrm; // 외화현금미수금금잔

    @JsonProperty("fc_etc_loana_nowrm")
    private String fcEtcLoanaNowrm; // 외화기타대여금금잔

    @JsonProperty("fc_exmn_alow_amt")
    private String fcExmnAlowAmt; // 외화환전가능금액

    @JsonProperty("fc_ch_uncl_rpym_amt")
    private String fcChUnclRpymAmt; // 외화현금미수변제금

    @JsonProperty("fc_ch_uncl_dlfe")
    private String fcChUnclDlfe; // 외화현금미수연체료

    @JsonProperty("fc_etc_loan_npay_rpym_amt")
    private String fcEtcLoanNpayRpymAmt; // 외화기타대여미납변제금

    @JsonProperty("fc_etc_loan_npay_dlfe")
    private String fcEtcLoanNpayDlfe; // 외화기타대여미납연체료

    @JsonProperty("krw_exmn_amt")
    private String krwExmnAmt; // 원화환전금액

    @JsonProperty("sell_fc_amt")
    private String sellFcAmt; // 매도외화금액

    @JsonProperty("buy_fc_amt")
    private String buyFcAmt; // 매수외화금액

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
