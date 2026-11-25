package com.example.stockanalysis.kiwoom.contract.tr.ka50080;

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
 * 금현물분봉차트조회요청(ka50080) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka50080Response implements KiwoomResult {

    @JsonProperty("gds_min_chart_qry")
    private List<GdsMinChartQryItem> gdsMinChartQry; // 금현물분봉차트조회

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
    public static class GdsMinChartQryItem {

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("acc_trde_qty")
        private String accTrdeQty; // 누적거래량

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("open_pric")
        private String openPric; // 시가

        @JsonProperty("high_pric")
        private String highPric; // 고가

        @JsonProperty("low_pric")
        private String lowPric; // 저가

        @JsonProperty("cntr_tm")
        private String cntrTm; // 체결시간

        @JsonProperty("dt")
        private String dt; // 일자

        @JsonProperty("pred_pre_sig")
        private String predPreSig; // 전일대비기호
    }
}
