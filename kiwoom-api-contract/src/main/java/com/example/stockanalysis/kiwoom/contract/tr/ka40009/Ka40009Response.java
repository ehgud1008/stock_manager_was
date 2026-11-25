package com.example.stockanalysis.kiwoom.contract.tr.ka40009;

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
 * ETF시간대별NAV현황(ka40009) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka40009Response implements KiwoomResult {

    @JsonProperty("etfnavarray")
    private List<EtfnavarrayItem> etfnavarray; // ETFNAV배열

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
    public static class EtfnavarrayItem {

        @JsonProperty("nav")
        private String nav; // NAV

        @JsonProperty("navpred_pre")
        private String navpredPre; // NAV전일대비

        @JsonProperty("navflu_rt")
        private String navfluRt; // NAV등락율

        @JsonProperty("trace_eor_rt")
        private String traceEorRt; // 추적오차율

        @JsonProperty("dispty_rt")
        private String disptyRt; // 괴리율

        @JsonProperty("stkcnt")
        private String stkcnt; // 주식수

        @JsonProperty("base_pric")
        private String basePric; // 기준가

        @JsonProperty("for_rmnd_qty")
        private String forRmndQty; // 외인보유수량

        @JsonProperty("repl_pric")
        private String replPric; // 대용가

        @JsonProperty("conv_pric")
        private String convPric; // 환산가격

        @JsonProperty("drstk")
        private String drstk; // DR/주

        @JsonProperty("wonju_pric")
        private String wonjuPric; // 원주가격
    }
}
