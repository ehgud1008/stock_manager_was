package com.example.stockanalysis.kiwoom.contract.tr.kt50020;

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
 * 금현물 잔고확인(kt50020) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt50020Response implements KiwoomResult {

    @JsonProperty("tot_entr")
    private String totEntr; // 예수금

    @JsonProperty("net_entr")
    private String netEntr; // 추정예수금

    @JsonProperty("tot_est_amt")
    private String totEstAmt; // 잔고평가액

    @JsonProperty("net_amt")
    private String netAmt; // 예탁자산평가액

    @JsonProperty("tot_book_amt2")
    private String totBookAmt2; // 총매입금액

    @JsonProperty("tot_dep_amt")
    private String totDepAmt; // 추정예탁자산

    @JsonProperty("paym_alowa")
    private String paymAlowa; // 출금가능금액

    @JsonProperty("pl_amt")
    private String plAmt; // 실현손익

    @JsonProperty("gold_acnt_evlt_prst")
    private List<GoldAcntEvltPrstItem> goldAcntEvltPrst; // 금현물계좌평가현황

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
    public static class GoldAcntEvltPrstItem {

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("real_qty")
        private String realQty; // 보유수량

        @JsonProperty("avg_prc")
        private String avgPrc; // 평균단가

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("est_amt")
        private String estAmt; // 평가금액

        @JsonProperty("est_lspft")
        private String estLspft; // 손익금액

        @JsonProperty("est_ratio")
        private String estRatio; // 손익율

        @JsonProperty("cmsn")
        private String cmsn; // 수수료

        @JsonProperty("vlad_tax")
        private String vladTax; // 부가가치세

        @JsonProperty("book_amt2")
        private String bookAmt2; // 매입금액

        @JsonProperty("pl_prch_prc")
        private String plPrchPrc; // 손익분기매입가

        @JsonProperty("qty")
        private String qty; // 결제잔고

        @JsonProperty("buy_qty")
        private String buyQty; // 매수수량

        @JsonProperty("sell_qty")
        private String sellQty; // 매도수량

        @JsonProperty("able_qty")
        private String ableQty; // 가능수량
    }
}
