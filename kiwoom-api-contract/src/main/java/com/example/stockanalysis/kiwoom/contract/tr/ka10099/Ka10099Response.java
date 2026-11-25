package com.example.stockanalysis.kiwoom.contract.tr.ka10099;

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
 * 종목정보 리스트(ka10099) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10099Response implements KiwoomResult {

    @JsonProperty("list")
    private List<ListItem> list; // 종목리스트

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

        @JsonProperty("code")
        private String code; // 종목코드

        @JsonProperty("name")
        private String name; // 종목명

        @JsonProperty("listCount")
        private String listCount; // 상장주식수

        @JsonProperty("auditInfo")
        private String auditInfo; // 감리구분

        @JsonProperty("regDay")
        private String regDay; // 상장일

        @JsonProperty("lastPrice")
        private String lastPrice; // 전일종가

        @JsonProperty("state")
        private String state; // 종목상태

        @JsonProperty("marketCode")
        private String marketCode; // 시장구분코드

        @JsonProperty("marketName")
        private String marketName; // 시장명

        @JsonProperty("upName")
        private String upName; // 업종명

        @JsonProperty("upSizeName")
        private String upSizeName; // 회사크기분류

        @JsonProperty("companyClassName")
        private String companyClassName; // 회사분류

        @JsonProperty("orderWarning")
        private String orderWarning; // 투자유의종목여부

        @JsonProperty("nxtEnable")
        private String nxtEnable; // NXT가능여부
    }
}
