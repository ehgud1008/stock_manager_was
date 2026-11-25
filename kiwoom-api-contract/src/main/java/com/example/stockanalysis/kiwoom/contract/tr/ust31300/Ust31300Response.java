package com.example.stockanalysis.kiwoom.contract.tr.ust31300;

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
 * 환전 예상 금액 조회(ust31300) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust31300Response implements KiwoomResult {

    @JsonProperty("sell_aplc_exrt")
    private String sellAplcExrt; // 매도적용환율

    @JsonProperty("buy_aplc_exrt")
    private String buyAplcExrt; // 매수적용환율

    @JsonProperty("aplc_exrt")
    private String aplcExrt; // 적용환율

    @JsonProperty("sell_crnc_entra_nowrm")
    private String sellCrncEntraNowrm; // 매도통화예수금금잔

    @JsonProperty("sell_crnc_ch_uncla_nowrm")
    private String sellCrncChUnclaNowrm; // 매도통화현금미수금금잔

    @JsonProperty("sell_crnc_etc_loana_nowrm")
    private String sellCrncEtcLoanaNowrm; // 매도통화기타대여금금잔

    @JsonProperty("sell_crnc_exmn_alow_amt")
    private String sellCrncExmnAlowAmt; // 매도통화환전가능금액

    @JsonProperty("buy_crnc_entra_nowrm")
    private String buyCrncEntraNowrm; // 매수통화예수금금잔

    @JsonProperty("buy_crnc_ch_uncla_nowrm")
    private String buyCrncChUnclaNowrm; // 매수통화현금미수금금잔

    @JsonProperty("buy_crnc_etc_loana_nowrm")
    private String buyCrncEtcLoanaNowrm; // 매수통화기타대여금금잔

    @JsonProperty("buy_crnc_exmn_alow_amt")
    private String buyCrncExmnAlowAmt; // 매수통화환전가능금액

    @JsonProperty("krw_uncl_amt")
    private String krwUnclAmt; // 원화미수금액

    @JsonProperty("sell_expc_amt")
    private String sellExpcAmt; // 매도예상금액

    @JsonProperty("buy_expc_amt")
    private String buyExpcAmt; // 매수예상금액

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
