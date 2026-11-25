package com.example.stockanalysis.kiwoom.contract.tr.ust21620;

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
 * 미국주식 당일매매정리(ust21620) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21620Response implements KiwoomResult {

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

        @JsonProperty("slby_tp_nm")
        private String slbyTpNm; // 매도수구분명

        @JsonProperty("crnc_code")
        private String crncCode; // 통화코드

        @JsonProperty("cntr_uv")
        private String cntrUv; // 체결단가

        @JsonProperty("cntr_qty")
        private String cntrQty; // 체결수량

        @JsonProperty("engg_amt")
        private String enggAmt; // 약정금액

        @JsonProperty("cmsn")
        private String cmsn; // 수수료

        @JsonProperty("altx")
        private String altx; // 제세금

        @JsonProperty("exct_amt")
        private String exctAmt; // 정산금액

        @JsonProperty("exch_rate")
        private String exchRate; // 환율

        @JsonProperty("natn_nm")
        private String natnNm; // 국가명
    }
}
