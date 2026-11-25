package com.example.stockanalysis.kiwoom.contract.tr.usa10099;

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
 * 미국주식 종목리스트(usa10099) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa10099Response implements KiwoomResult {

    @JsonProperty("list")
    private List<ListItem> list; // 결과리스트

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
    public static class ListItem {

        @JsonProperty("stex_tp")
        private String stexTp; // 거래소구분

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("stk_enm")
        private String stkEnm; // 종목영문명

        @JsonProperty("mkgb")
        private String mkgb; // 거래소명

        @JsonProperty("upgb")
        private String upgb; // 업종명

        @JsonProperty("isEtf")
        private String isEtf; // ETF 여부
    }
}
