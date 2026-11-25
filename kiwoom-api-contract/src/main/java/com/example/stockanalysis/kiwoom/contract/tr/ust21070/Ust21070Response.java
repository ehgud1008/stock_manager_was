package com.example.stockanalysis.kiwoom.contract.tr.ust21070;

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
 * 미국주식 원장잔고확인(ust21070) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21070Response implements KiwoomResult {

    @JsonProperty("crnc_code")
    private String crncCode; // 통화코드

    @JsonProperty("tot_evlt_amt")
    private String totEvltAmt; // 총평가금액

    @JsonProperty("tot_prch_amt")
    private String totPrchAmt; // 총매입금액

    @JsonProperty("tot_pl_amt")
    private String totPlAmt; // 총손익금액

    @JsonProperty("tot_pl_rt")
    private String totPlRt; // 총수익율

    @JsonProperty("tdy_book_amt")
    private String tdyBookAmt; // 당일실현손익매입금액

    @JsonProperty("tdy_pl_amt")
    private String tdyPlAmt; // 당일실현손익

    @JsonProperty("tdy_pl_rt")
    private String tdyPlRt; // 당일실현손익율(%)

    @JsonProperty("tot_evlt_amt_krw")
    private String totEvltAmtKrw; // 총평가금액(원)

    @JsonProperty("tot_prch_amt_krw")
    private String totPrchAmtKrw; // 총매입금액(원)

    @JsonProperty("tot_pl_amt_krw")
    private String totPlAmtKrw; // 총손익금액(원)

    @JsonProperty("tdy_book_amt_krw")
    private String tdyBookAmtKrw; // 당일실현손익매입금액(원)

    @JsonProperty("tdy_pl_amt_krw")
    private String tdyPlAmtKrw; // 당일실현손익(원)

    @JsonProperty("result_list")
    private List<ResultListItem> resultList; // 결과리스트

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
    public static class ResultListItem {

        @JsonProperty("stex_nm")
        private String stexNm; // 거래소명

        @JsonProperty("crnc_code")
        private String crncCode; // 통화코드

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("frgn_stk_nm")
        private String frgnStkNm; // 종목명

        @JsonProperty("qty")
        private String qty; // 결제기준수량

        @JsonProperty("poss_qty")
        private String possQty; // 보유수량

        @JsonProperty("sell_alowq")
        private String sellAlowq; // 매도가능수량

        @JsonProperty("pred_cntr_sellq")
        private String predCntrSellq; // 전일매도수량

        @JsonProperty("pred_cntr_buyq")
        private String predCntrBuyq; // 전일매수수량

        @JsonProperty("tdy_cntr_sellq")
        private String tdyCntrSellq; // 금일매도수량

        @JsonProperty("tdy_cntr_buyq")
        private String tdyCntrBuyq; // 금일매수수량

        @JsonProperty("frgn_stk_book_uv")
        private String frgnStkBookUv; // 매입단가

        @JsonProperty("now_pric")
        private String nowPric; // 현재가

        @JsonProperty("evlt_amt")
        private String evltAmt; // 평가금액

        @JsonProperty("pl_amt")
        private String plAmt; // 손익금액

        @JsonProperty("pl_rt")
        private String plRt; // 손익율(%)

        @JsonProperty("evlt_amt_krw")
        private String evltAmtKrw; // 평가금액(원)

        @JsonProperty("pl_amt_krw")
        private String plAmtKrw; // 손익금액(원)

        @JsonProperty("natn_nm")
        private String natnNm; // 국가명

        @JsonProperty("exch_rate")
        private String exchRate; // 환율

        @JsonProperty("frgn_stk_book_uv_krw")
        private String frgnStkBookUvKrw; // 매입단가(원)

        @JsonProperty("now_pric_krw")
        private String nowPricKrw; // 현재가(원)

        @JsonProperty("frgn_stk_book_amt")
        private String frgnStkBookAmt; // 매입금액

        @JsonProperty("frgn_stk_book_amt_krw")
        private String frgnStkBookAmtKrw; // 매입금액(원)
    }
}
