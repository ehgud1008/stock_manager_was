package com.example.stockanalysis.kiwoom.contract.tr.ust21170;

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
 * 미국주식 당일 종목별 실현손익(ust21170) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21170Response implements KiwoomResult {

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

    @JsonProperty("tdy_pl_amt")
    private String tdyPlAmt; // 당일실현손익

    @JsonProperty("prsm_aseta_krw")
    private String prsmAsetaKrw; // 추정자산(원)

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

        @JsonProperty("tdy_pl_amt")
        private String tdyPlAmt; // 당일실현손익금액

        @JsonProperty("evlt_pl_amt")
        private String evltPlAmt; // 평가손익금액

        @JsonProperty("evlt_pl_rt")
        private String evltPlRt; // 평가수익율(%)

        @JsonProperty("prch_uv")
        private String prchUv; // 매입단가

        @JsonProperty("poss_qty")
        private String possQty; // 보유수량

        @JsonProperty("sell_alowq")
        private String sellAlowq; // 매도가능수량

        @JsonProperty("now_pric")
        private String nowPric; // 현재가

        @JsonProperty("prch_amt")
        private String prchAmt; // 매입금액

        @JsonProperty("evlt_amt")
        private String evltAmt; // 평가금액

        @JsonProperty("prch_exrt")
        private String prchExrt; // 매입환율

        @JsonProperty("sell_exrt")
        private String sellExrt; // 매도환율

        @JsonProperty("krw_chg_dfrn_pl_amt")
        private String krwChgDfrnPlAmt; // 환차손익(원)

        @JsonProperty("krw_chg_pl_amt")
        private String krwChgPlAmt; // 환실현손익(원)

        @JsonProperty("evlt_amt_cmsn")
        private String evltAmtCmsn; // 평가금액수수료

        @JsonProperty("evlt_amt_tax")
        private String evltAmtTax; // 평가금액세금

        @JsonProperty("natn_nm")
        private String natnNm; // 국가명
    }
}
