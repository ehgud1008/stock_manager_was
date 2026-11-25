package com.example.stockanalysis.kiwoom.contract.tr.ka10058;

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
 * 투자자별일별매매종목요청(ka10058) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10058Response implements KiwoomResult {

    @JsonProperty("invsr_daly_trde_stk")
    private List<InvsrDalyTrdeStkItem> invsrDalyTrdeStk; // 투자자별일별매매종목

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
    public static class InvsrDalyTrdeStkItem {

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("netslmt_qty")
        private String netslmtQty; // 순매도수량

        @JsonProperty("netslmt_amt")
        private String netslmtAmt; // 순매도금액

        @JsonProperty("prsm_avg_pric")
        private String prsmAvgPric; // 추정평균가

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("pre_sig")
        private String preSig; // 대비기호

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("avg_pric_pre")
        private String avgPricPre; // 평균가대비

        @JsonProperty("pre_rt")
        private String preRt; // 대비율

        @JsonProperty("dt_trde_qty")
        private String dtTrdeQty; // 기간거래량
    }
}
