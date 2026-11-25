package com.example.stockanalysis.kiwoom.contract.tr.usa26411;

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
 * 미국주식 연도별 업종별 종목등락률(usa26411) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa26411Response implements KiwoomResult {

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

        @JsonProperty("stex_tp")
        private String stexTp; // 거래소구분

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("stk_enm")
        private String stkEnm; // 종목영문명

        @JsonProperty("m01_prft_rt")
        private String m01PrftRt; // 1월 수익률

        @JsonProperty("m02_prft_rt")
        private String m02PrftRt; // 2월 수익률

        @JsonProperty("m03_prft_rt")
        private String m03PrftRt; // 3월 수익률

        @JsonProperty("m04_prft_rt")
        private String m04PrftRt; // 4월 수익률

        @JsonProperty("m05_prft_rt")
        private String m05PrftRt; // 5월 수익률

        @JsonProperty("m06_prft_rt")
        private String m06PrftRt; // 6월 수익률

        @JsonProperty("m07_prft_rt")
        private String m07PrftRt; // 7월 수익률

        @JsonProperty("m08_prft_rt")
        private String m08PrftRt; // 8월 수익률

        @JsonProperty("m09_prft_rt")
        private String m09PrftRt; // 9월 수익률

        @JsonProperty("m10_prft_rt")
        private String m10PrftRt; // 10월 수익률

        @JsonProperty("m11_prft_rt")
        private String m11PrftRt; // 11월 수익률

        @JsonProperty("m12_prft_rt")
        private String m12PrftRt; // 12월 수익률
    }
}
