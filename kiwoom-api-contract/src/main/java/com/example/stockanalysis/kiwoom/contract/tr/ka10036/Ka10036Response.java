package com.example.stockanalysis.kiwoom.contract.tr.ka10036;

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
 * 외인한도소진율증가상위(ka10036) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10036Response implements KiwoomResult {

    @JsonProperty("for_limit_exh_rt_incrs_upper")
    private List<ForLimitExhRtIncrsUpperItem> forLimitExhRtIncrsUpper; // 외인한도소진율증가상위

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
    public static class ForLimitExhRtIncrsUpperItem {

        @JsonProperty("rank")
        private String rank; // 순위

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

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("poss_stkcnt")
        private String possStkcnt; // 보유주식수

        @JsonProperty("gain_pos_stkcnt")
        private String gainPosStkcnt; // 취득가능주식수

        @JsonProperty("base_limit_exh_rt")
        private String baseLimitExhRt; // 기준한도소진율

        @JsonProperty("limit_exh_rt")
        private String limitExhRt; // 한도소진율

        @JsonProperty("exh_rt_incrs")
        private String exhRtIncrs; // 소진율증가
    }
}
