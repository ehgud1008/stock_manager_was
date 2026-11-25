package com.example.stockanalysis.kiwoom.contract.tr.ust21610;

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
 * 미국주식 당일매매(ust21610) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21610Response implements KiwoomResult {

    @JsonProperty("tot_sell_amt")
    private String totSellAmt; // 총매도금액

    @JsonProperty("tot_buy_amt")
    private String totBuyAmt; // 총매수금액

    @JsonProperty("tot_cmsn_altx")
    private String totCmsnAltx; // 총수수료제세금

    @JsonProperty("tot_exct_amt")
    private String totExctAmt; // 총정산금액

    @JsonProperty("tot_pl_amt")
    private String totPlAmt; // 총실현손익

    @JsonProperty("tot_pl_rt")
    private String totPlRt; // 총실현손익률

    @JsonProperty("exch_rate")
    private String exchRate; // 기준환율

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
        private String stexNm; // 거래소코드명

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("tdy_avg_buy_uv")
        private String tdyAvgBuyUv; // 금일매수평균가

        @JsonProperty("tdy_buyq")
        private String tdyBuyq; // 금일매수수량

        @JsonProperty("tdy_buy_amt")
        private String tdyBuyAmt; // 금일매입금액

        @JsonProperty("tdy_avg_sell_uv")
        private String tdyAvgSellUv; // 금일매도평균가

        @JsonProperty("tdy_sellq")
        private String tdySellq; // 금일매도수량

        @JsonProperty("tdy_sell_amt")
        private String tdySellAmt; // 금일매도금액

        @JsonProperty("cmsn_altx")
        private String cmsnAltx; // 수수료제세금

        @JsonProperty("pl_amt")
        private String plAmt; // 실현손익

        @JsonProperty("pl_rt")
        private String plRt; // 실현손익률

        @JsonProperty("bf_prch_uv")
        private String bfPrchUv; // 이전매입가

        @JsonProperty("exch_rate")
        private String exchRate; // 기준환율
    }
}
