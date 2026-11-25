package com.example.stockanalysis.kiwoom.contract.tr.usa10104;

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
 * 미국 ETF,ETN 리스트(usa10104) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa10104Response implements KiwoomResult {

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

        @JsonProperty("code")
        private String code; // 종목코드

        @JsonProperty("cate1")
        private String cate1; // 카테고리1

        @JsonProperty("cate2")
        private String cate2; // 카테고리2

        @JsonProperty("etn")
        private String etn; // ETN여부

        @JsonProperty("mkgb")
        private String mkgb; // 거래소명

        @JsonProperty("upnm")
        private String upnm; // 업종명
    }
}
