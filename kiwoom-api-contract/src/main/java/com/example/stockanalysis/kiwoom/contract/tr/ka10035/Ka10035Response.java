package com.example.stockanalysis.kiwoom.contract.tr.ka10035;

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
 * 외인연속순매매상위요청(ka10035) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10035Response implements KiwoomResult {

    @JsonProperty("for_cont_nettrde_upper")
    private List<ForContNettrdeUpperItem> forContNettrdeUpper; // 외인연속순매매상위

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
    public static class ForContNettrdeUpperItem {

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("pred_pre_sig")
        private String predPreSig; // 전일대비기호

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("dm1")
        private String dm1; // D-1

        @JsonProperty("dm2")
        private String dm2; // D-2

        @JsonProperty("dm3")
        private String dm3; // D-3

        @JsonProperty("tot")
        private String tot; // 합계

        @JsonProperty("limit_exh_rt")
        private String limitExhRt; // 한도소진율

        @JsonProperty("pred_pre_1")
        private String predPre1; // 전일대비1

        @JsonProperty("pred_pre_2")
        private String predPre2; // 전일대비2

        @JsonProperty("pred_pre_3")
        private String predPre3; // 전일대비3
    }
}
