package com.example.stockanalysis.kiwoom.contract.tr.usa23000;

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
 * 미국주식 업종별 기간별 수익률 조회(usa23000) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa23000Response implements KiwoomResult {

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

        @JsonProperty("inds_cd")
        private String indsCd; // 업종코드

        @JsonProperty("inds_nm")
        private String indsNm; // 업종명

        @JsonProperty("perf_1d")
        private String perf1d; // 1일 수익률

        @JsonProperty("perf_5d")
        private String perf5d; // 5일 수익률

        @JsonProperty("perf_1m")
        private String perf1m; // 1개월 수익률

        @JsonProperty("perf_3m")
        private String perf3m; // 3개월 수익률

        @JsonProperty("perf_6m")
        private String perf6m; // 6개월 수익률

        @JsonProperty("perf_ytd")
        private String perfYtd; // 연중 수익률

        @JsonProperty("perf_1y")
        private String perf1y; // 1년 수익률
    }
}
