package com.example.stockanalysis.kiwoom.contract.tr.ka10064;

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
 * 장중투자자별매매차트요청(ka10064) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10064Response implements KiwoomResult {

    @JsonProperty("opmr_invsr_trde_chart")
    private List<OpmrInvsrTrdeChartItem> opmrInvsrTrdeChart; // 장중투자자별매매차트

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
    public static class OpmrInvsrTrdeChartItem {

        @JsonProperty("tm")
        private String tm; // 시간

        @JsonProperty("frgnr_invsr")
        private String frgnrInvsr; // 외국인투자자

        @JsonProperty("orgn")
        private String orgn; // 기관계

        @JsonProperty("invtrt")
        private String invtrt; // 투신

        @JsonProperty("insrnc")
        private String insrnc; // 보험

        @JsonProperty("bank")
        private String bank; // 은행

        @JsonProperty("penfnd_etc")
        private String penfndEtc; // 연기금등

        @JsonProperty("etc_corp")
        private String etcCorp; // 기타법인

        @JsonProperty("natn")
        private String natn; // 국가
    }
}
