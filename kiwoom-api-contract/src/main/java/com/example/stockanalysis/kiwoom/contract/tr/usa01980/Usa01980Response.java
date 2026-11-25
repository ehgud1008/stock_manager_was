package com.example.stockanalysis.kiwoom.contract.tr.usa01980;

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
 * 미국주식 실시간 종목 조회 순위(usa01980) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa01980Response implements KiwoomResult {

    @JsonProperty("base_date")
    private String baseDate; // 기준날짜

    @JsonProperty("base_time")
    private String baseTime; // 기준시간

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

        @JsonProperty("rank")
        private String rank; // 종목순위

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("sign")
        private String sign; // 등락부호

        @JsonProperty("chg_val")
        private String chgVal; // 직전대비 순위변화값

        @JsonProperty("curr_pric")
        private String currPric; // 현재가

        @JsonProperty("sign_for_gjga")
        private String signForGjga; // 기준가대비 부호

        @JsonProperty("diff_rate_for_gjga")
        private String diffRateForGjga; // 기준가대비 등락률

        @JsonProperty("sign_for_prev")
        private String signForPrev; // 직전기준대비 부호

        @JsonProperty("diff_rate_for_prev")
        private String diffRateForPrev; // 직전기준대비 등락률

        @JsonProperty("stex_tp")
        private String stexTp; // 거래소구분
    }
}
