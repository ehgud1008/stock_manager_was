package com.example.stockanalysis.kiwoom.contract.tr.ka10047;

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
 * 체결강도추이일별요청(ka10047) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10047Response implements KiwoomResult {

    @JsonProperty("cntr_str_daly")
    private List<CntrStrDalyItem> cntrStrDaly; // 체결강도일별

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
    public static class CntrStrDalyItem {

        @JsonProperty("dt")
        private String dt; // 일자

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("pred_pre_sig")
        private String predPreSig; // 전일대비기호

        @JsonProperty("flu_rt")
        private String fluRt; // 등락율

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("acc_trde_prica")
        private String accTrdePrica; // 누적거래대금

        @JsonProperty("acc_trde_qty")
        private String accTrdeQty; // 누적거래량

        @JsonProperty("cntr_str")
        private String cntrStr; // 체결강도

        @JsonProperty("cntr_str_5min")
        private String cntrStr5min; // 체결강도5일

        @JsonProperty("cntr_str_20min")
        private String cntrStr20min; // 체결강도20일

        @JsonProperty("cntr_str_60min")
        private String cntrStr60min; // 체결강도60일
    }
}
