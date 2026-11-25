package com.example.stockanalysis.kiwoom.contract.tr.ka10086;

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
 * 일별주가요청(ka10086) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10086Response implements KiwoomResult {

    @JsonProperty("daly_stkpc")
    private List<DalyStkpcItem> dalyStkpc; // 일별주가

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
    public static class DalyStkpcItem {

        @JsonProperty("date")
        private String date; // 날짜

        @JsonProperty("open_pric")
        private String openPric; // 시가

        @JsonProperty("high_pric")
        private String highPric; // 고가

        @JsonProperty("low_pric")
        private String lowPric; // 저가

        @JsonProperty("close_pric")
        private String closePric; // 종가

        @JsonProperty("pred_rt")
        private String predRt; // 전일비

        @JsonProperty("flu_rt")
        private String fluRt; // 등락률

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("amt_mn")
        private String amtMn; // 금액(백만)

        @JsonProperty("crd_rt")
        private String crdRt; // 신용비

        @JsonProperty("ind")
        private String ind; // 개인

        @JsonProperty("orgn")
        private String orgn; // 기관

        @JsonProperty("for_qty")
        private String forQty; // 외인수량

        @JsonProperty("frgn")
        private String frgn; // 외국계

        @JsonProperty("prm")
        private String prm; // 프로그램

        @JsonProperty("for_rt")
        private String forRt; // 외인비

        @JsonProperty("for_poss")
        private String forPoss; // 외인보유

        @JsonProperty("for_wght")
        private String forWght; // 외인비중

        @JsonProperty("for_netprps")
        private String forNetprps; // 외인순매수

        @JsonProperty("orgn_netprps")
        private String orgnNetprps; // 기관순매수

        @JsonProperty("ind_netprps")
        private String indNetprps; // 개인순매수

        @JsonProperty("crd_remn_rt")
        private String crdRemnRt; // 신용잔고율
    }
}
