package com.example.stockanalysis.kiwoom.contract.tr.ka50083;

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
 * 금현물월봉차트조회요청(ka50083) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka50083Response implements KiwoomResult {

    @JsonProperty("gds_month_chart_qry")
    private List<GdsMonthChartQryItem> gdsMonthChartQry; // 금현물일봉차트조회

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
    public static class GdsMonthChartQryItem {

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("acc_trde_qty")
        private String accTrdeQty; // 누적 거래량

        @JsonProperty("acc_trde_prica")
        private String accTrdePrica; // 누적 거래대금

        @JsonProperty("open_pric")
        private String openPric; // 시가

        @JsonProperty("high_pric")
        private String highPric; // 고가

        @JsonProperty("low_pric")
        private String lowPric; // 저가

        @JsonProperty("dt")
        private String dt; // 일자
    }
}
