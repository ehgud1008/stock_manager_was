package com.example.stockanalysis.kiwoom.contract.tr.ka40006;

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
 * ETF시간대별추이요청(ka40006) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka40006Response implements KiwoomResult {

    @JsonProperty("stk_nm")
    private String stkNm; // 종목명

    @JsonProperty("etfobjt_idex_nm")
    private String etfobjtIdexNm; // ETF대상지수명

    @JsonProperty("wonju_pric")
    private String wonjuPric; // 원주가격

    @JsonProperty("etftxon_type")
    private String etftxonType; // ETF과세유형

    @JsonProperty("etntxon_type")
    private String etntxonType; // ETN과세유형

    @JsonProperty("etftisl_trnsn")
    private List<EtftislTrnsnItem> etftislTrnsn; // ETF시간대별추이

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
    public static class EtftislTrnsnItem {

        @JsonProperty("tm")
        private String tm; // 시간

        @JsonProperty("close_pric")
        private String closePric; // 종가

        @JsonProperty("pre_sig")
        private String preSig; // 대비기호

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("flu_rt")
        private String fluRt; // 등락율

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("nav")
        private String nav; // NAV

        @JsonProperty("trde_prica")
        private String trdePrica; // 거래대금

        @JsonProperty("navidex")
        private String navidex; // NAV지수

        @JsonProperty("navetf")
        private String navetf; // 괴리율

        @JsonProperty("trace")
        private String trace; // 추적오차율

        @JsonProperty("trace_idex")
        private String traceIdex; // 추적지수

        @JsonProperty("trace_idex_pred_pre")
        private String traceIdexPredPre; // 추적지수전일대비

        @JsonProperty("trace_idex_pred_pre_sig")
        private String traceIdexPredPreSig; // 추적지수전일대비기호
    }
}
