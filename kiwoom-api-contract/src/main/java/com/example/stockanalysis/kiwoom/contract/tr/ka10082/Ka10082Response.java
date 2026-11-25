package com.example.stockanalysis.kiwoom.contract.tr.ka10082;

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
 * 주식주봉차트조회요청(ka10082) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10082Response implements KiwoomResult {

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("stk_stk_pole_chart_qry")
    private List<StkStkPoleChartQryItem> stkStkPoleChartQry; // 주식주봉차트조회

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
    public static class StkStkPoleChartQryItem {

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("trde_prica")
        private String trdePrica; // 거래대금

        @JsonProperty("dt")
        private String dt; // 일자

        @JsonProperty("open_pric")
        private String openPric; // 시가

        @JsonProperty("high_pric")
        private String highPric; // 고가

        @JsonProperty("low_pric")
        private String lowPric; // 저가

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("pred_pre_sig")
        private String predPreSig; // 전일대비기호

        @JsonProperty("trde_tern_rt")
        private String trdeTernRt; // 거래회전율
    }
}
