package com.example.stockanalysis.kiwoom.contract.tr.ka10045;

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
 * 종목별기관매매추이요청(ka10045) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10045Response implements KiwoomResult {

    @JsonProperty("orgn_prsm_avg_pric")
    private String orgnPrsmAvgPric; // 기관추정평균가

    @JsonProperty("for_prsm_avg_pric")
    private String forPrsmAvgPric; // 외인추정평균가

    @JsonProperty("stk_orgn_trde_trnsn")
    private List<StkOrgnTrdeTrnsnItem> stkOrgnTrdeTrnsn; // 종목별기관매매추이

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
    public static class StkOrgnTrdeTrnsnItem {

        @JsonProperty("dt")
        private String dt; // 일자

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

        @JsonProperty("orgn_dt_acc")
        private String orgnDtAcc; // 기관기간누적

        @JsonProperty("orgn_daly_nettrde_qty")
        private String orgnDalyNettrdeQty; // 기관일별순매매수량

        @JsonProperty("for_dt_acc")
        private String forDtAcc; // 외인기간누적

        @JsonProperty("for_daly_nettrde_qty")
        private String forDalyNettrdeQty; // 외인일별순매매수량

        @JsonProperty("limit_exh_rt")
        private String limitExhRt; // 한도소진율
    }
}
