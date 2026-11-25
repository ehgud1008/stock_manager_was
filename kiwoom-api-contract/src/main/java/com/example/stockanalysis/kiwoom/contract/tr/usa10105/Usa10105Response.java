package com.example.stockanalysis.kiwoom.contract.tr.usa10105;

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
 * 미국 ETF 카테고리 리스트(usa10105) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa10105Response implements KiwoomResult {

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

        @JsonProperty("gubun")
        private String gubun; // 구분

        @JsonProperty("cate1")
        private String cate1; // 카테고리1차

        @JsonProperty("cate1nam")
        private String cate1nam; // 카테고리1차명

        @JsonProperty("cate2")
        private String cate2; // 카테고리2차

        @JsonProperty("cate2nam")
        private String cate2nam; // 카테고리2차명
    }
}
