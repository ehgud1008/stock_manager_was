package com.example.stockanalysis.kiwoom.contract.tr.ka50012;

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
 * 금현물일별추이(ka50012) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka50012Response implements KiwoomResult {

    @JsonProperty("gold_daly_trnsn")
    private List<GoldDalyTrnsnItem> goldDalyTrnsn; // 금현물일별추이

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
    public static class GoldDalyTrnsnItem {

        @JsonProperty("cur_prc")
        private String curPrc; // 종가

        @JsonProperty("pred_pre")
        private String predPre; // 전일 대비

        @JsonProperty("flu_rt")
        private String fluRt; // 등락율

        @JsonProperty("trde_qty")
        private String trdeQty; // 누적 거래량

        @JsonProperty("acc_trde_prica")
        private String accTrdePrica; // 누적 거래대금(백만)

        @JsonProperty("open_pric")
        private String openPric; // 시가

        @JsonProperty("high_pric")
        private String highPric; // 고가

        @JsonProperty("low_pric")
        private String lowPric; // 저가

        @JsonProperty("dt")
        private String dt; // 일자

        @JsonProperty("pre_sig")
        private String preSig; // 전일대비기호

        @JsonProperty("orgn_netprps")
        private String orgnNetprps; // 기관 순매수 수량

        @JsonProperty("for_netprps")
        private String forNetprps; // 외국인 순매수 수량

        @JsonProperty("ind_netprps")
        private String indNetprps; // 순매매량(개인)
    }
}
