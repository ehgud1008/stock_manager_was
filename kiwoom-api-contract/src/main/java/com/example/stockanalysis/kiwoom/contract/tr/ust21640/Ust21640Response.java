package com.example.stockanalysis.kiwoom.contract.tr.ust21640;

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
 * 미국주식 일별 종목별 실현손익(ust21640) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21640Response implements KiwoomResult {

    @JsonProperty("tot_pl_amt")
    private String totPlAmt; // 총실현손익

    @JsonProperty("tot_pl_amt_krw")
    private String totPlAmtKrw; // 총실현손익(원)

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

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("crnc_code")
        private String crncCode; // 통화코드

        @JsonProperty("cntr_sellq")
        private String cntrSellq; // 체결매도수량

        @JsonProperty("avg_buy_uv")
        private String avgBuyUv; // 매입평균가

        @JsonProperty("cntr_sella")
        private String cntrSella; // 체결매도가

        @JsonProperty("pl_amt")
        private String plAmt; // 실현손익

        @JsonProperty("pl_rt")
        private String plRt; // 실현수익률

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("cmsn")
        private String cmsn; // 수수료

        @JsonProperty("altx")
        private String altx; // 제세금

        @JsonProperty("exch_rate")
        private String exchRate; // 환율

        @JsonProperty("natn_nm")
        private String natnNm; // 국가명
    }
}
