package com.example.stockanalysis.kiwoom.contract.tr.ka40004;

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
 * ETF전체시세요청(ka40004) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka40004Response implements KiwoomResult {

    @JsonProperty("etfall_mrpr")
    private List<EtfallMrprItem> etfallMrpr; // ETF전체시세

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
    public static class EtfallMrprItem {

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_cls")
        private String stkCls; // 종목분류

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("close_pric")
        private String closePric; // 종가

        @JsonProperty("pre_sig")
        private String preSig; // 대비기호

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("pre_rt")
        private String preRt; // 대비율

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("nav")
        private String nav; // NAV

        @JsonProperty("trace_eor_rt")
        private String traceEorRt; // 추적오차율

        @JsonProperty("txbs")
        private String txbs; // 과표기준

        @JsonProperty("dvid_bf_base")
        private String dvidBfBase; // 배당전기준

        @JsonProperty("pred_dvida")
        private String predDvida; // 전일배당금

        @JsonProperty("trace_idex_nm")
        private String traceIdexNm; // 추적지수명

        @JsonProperty("drng")
        private String drng; // 배수

        @JsonProperty("trace_idex_cd")
        private String traceIdexCd; // 추적지수코드

        @JsonProperty("trace_idex")
        private String traceIdex; // 추적지수

        @JsonProperty("trace_flu_rt")
        private String traceFluRt; // 추적등락율
    }
}
