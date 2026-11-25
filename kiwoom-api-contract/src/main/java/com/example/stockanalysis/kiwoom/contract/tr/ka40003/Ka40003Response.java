package com.example.stockanalysis.kiwoom.contract.tr.ka40003;

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
 * ETF일별추이요청(ka40003) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka40003Response implements KiwoomResult {

    @JsonProperty("etfdaly_trnsn")
    private List<EtfdalyTrnsnItem> etfdalyTrnsn; // ETF일별추이

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
    public static class EtfdalyTrnsnItem {

        @JsonProperty("cntr_dt")
        private String cntrDt; // 체결일자

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

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

        @JsonProperty("acc_trde_prica")
        private String accTrdePrica; // 누적거래대금

        @JsonProperty("navidex_dispty_rt")
        private String navidexDisptyRt; // NAV/지수괴리율

        @JsonProperty("navetfdispty_rt")
        private String navetfdisptyRt; // NAV/ETF괴리율

        @JsonProperty("trace_eor_rt")
        private String traceEorRt; // 추적오차율

        @JsonProperty("trace_cur_prc")
        private String traceCurPrc; // 추적현재가

        @JsonProperty("trace_pred_pre")
        private String tracePredPre; // 추적전일대비

        @JsonProperty("trace_pre_sig")
        private String tracePreSig; // 추적대비기호
    }
}
