package com.example.stockanalysis.kiwoom.contract.tr.ust21121;

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
 * 해외증권 원장 평가금액현황(ust21121) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21121Response implements KiwoomResult {

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

        @JsonProperty("natn_nm")
        private String natnNm; // 국가명

        @JsonProperty("stex_nm")
        private String stexNm; // 거래소코드명

        @JsonProperty("crnc_code")
        private String crncCode; // 통화코드

        @JsonProperty("crnc_nm")
        private String crncNm; // 통화코드명

        @JsonProperty("evlt_amt")
        private String evltAmt; // 평가금액

        @JsonProperty("chg_evlt_amt")
        private String chgEvltAmt; // 환전평가금액

        @JsonProperty("pl_rt")
        private String plRt; // 평가수익율(%)

        @JsonProperty("pl_amt")
        private String plAmt; // 평가손익

        @JsonProperty("chg_profit_amt")
        private String chgProfitAmt; // 환전평가손익

        @JsonProperty("evlt_amt_wght")
        private String evltAmtWght; // 평가금액비중
    }
}
