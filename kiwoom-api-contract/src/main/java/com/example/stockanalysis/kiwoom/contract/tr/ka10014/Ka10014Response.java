package com.example.stockanalysis.kiwoom.contract.tr.ka10014;

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
 * 공매도추이요청(ka10014) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10014Response implements KiwoomResult {

    @JsonProperty("shrts_trnsn")
    private List<ShrtsTrnsnItem> shrtsTrnsn; // 공매도추이

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
    public static class ShrtsTrnsnItem {

        @JsonProperty("dt")
        private String dt; // 일자

        @JsonProperty("close_pric")
        private String closePric; // 종가

        @JsonProperty("pred_pre_sig")
        private String predPreSig; // 전일대비기호

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("flu_rt")
        private String fluRt; // 등락율

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("shrts_qty")
        private String shrtsQty; // 공매도량

        @JsonProperty("ovr_shrts_qty")
        private String ovrShrtsQty; // 누적공매도량

        @JsonProperty("trde_wght")
        private String trdeWght; // 매매비중

        @JsonProperty("shrts_trde_prica")
        private String shrtsTrdePrica; // 공매도거래대금

        @JsonProperty("shrts_avg_pric")
        private String shrtsAvgPric; // 공매도평균가
    }
}
