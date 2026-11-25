package com.example.stockanalysis.kiwoom.contract.tr.ust21530;

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
 * 미국주식 실현손익(ust21530) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21530Response implements KiwoomResult {

    @JsonProperty("tot_sell_amt")
    private String totSellAmt; // 총매도금액

    @JsonProperty("tot_buy_amt")
    private String totBuyAmt; // 총매수금액

    @JsonProperty("tot_cmsn_tax")
    private String totCmsnTax; // 총수수료제세금

    @JsonProperty("tot_exct_amt")
    private String totExctAmt; // 총정산금액

    @JsonProperty("tot_pl_amt")
    private String totPlAmt; // 총손익금액

    @JsonProperty("tot_pl_rt")
    private String totPlRt; // 총실현수익률(%)

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

        @JsonProperty("sell_dt")
        private String sellDt; // 매도일자

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("frgn_stk_nm")
        private String frgnStkNm; // 종목명

        @JsonProperty("sell_qty")
        private String sellQty; // 청산수량

        @JsonProperty("avg_buy_uv")
        private String avgBuyUv; // 매입평균가

        @JsonProperty("buy_amt")
        private String buyAmt; // 매입금액

        @JsonProperty("avg_sell_uv")
        private String avgSellUv; // 매도평균가

        @JsonProperty("sell_amt")
        private String sellAmt; // 매도금액

        @JsonProperty("cmsn_tax")
        private String cmsnTax; // 수수료제세금

        @JsonProperty("pl_amt")
        private String plAmt; // 손익금액

        @JsonProperty("pl_rt")
        private String plRt; // 실현수익률(%)

        @JsonProperty("prch_exrt")
        private String prchExrt; // 매입환율

        @JsonProperty("sell_exrt")
        private String sellExrt; // 매도환율

        @JsonProperty("krw_chg_dfrn_pl_amt")
        private String krwChgDfrnPlAmt; // 환차손익(원)

        @JsonProperty("krw_chg_pl_amt")
        private String krwChgPlAmt; // 환실현손익(원)

        @JsonProperty("comm_ord_tp")
        private String commOrdTp; // 매체구분

        @JsonProperty("stex_nm")
        private String stexNm; // 거래소명

        @JsonProperty("natn_nm")
        private String natnNm; // 국가명
    }
}
