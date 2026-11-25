package com.example.stockanalysis.kiwoom.contract.tr.ka90009;

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
 * 외국인기관매매상위요청(ka90009) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka90009Response implements KiwoomResult {

    @JsonProperty("frgnr_orgn_trde_upper")
    private List<FrgnrOrgnTrdeUpperItem> frgnrOrgnTrdeUpper; // 외국인기관매매상위

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
    public static class FrgnrOrgnTrdeUpperItem {

        @JsonProperty("for_netslmt_stk_cd")
        private String forNetslmtStkCd; // 외인순매도종목코드

        @JsonProperty("for_netslmt_stk_nm")
        private String forNetslmtStkNm; // 외인순매도종목명

        @JsonProperty("for_netslmt_amt")
        private String forNetslmtAmt; // 외인순매도금액

        @JsonProperty("for_netslmt_qty")
        private String forNetslmtQty; // 외인순매도수량

        @JsonProperty("for_netprps_stk_cd")
        private String forNetprpsStkCd; // 외인순매수종목코드

        @JsonProperty("for_netprps_stk_nm")
        private String forNetprpsStkNm; // 외인순매수종목명

        @JsonProperty("for_netprps_amt")
        private String forNetprpsAmt; // 외인순매수금액

        @JsonProperty("for_netprps_qty")
        private String forNetprpsQty; // 외인순매수수량

        @JsonProperty("orgn_netslmt_stk_cd")
        private String orgnNetslmtStkCd; // 기관순매도종목코드

        @JsonProperty("orgn_netslmt_stk_nm")
        private String orgnNetslmtStkNm; // 기관순매도종목명

        @JsonProperty("orgn_netslmt_amt")
        private String orgnNetslmtAmt; // 기관순매도금액

        @JsonProperty("orgn_netslmt_qty")
        private String orgnNetslmtQty; // 기관순매도수량

        @JsonProperty("orgn_netprps_stk_cd")
        private String orgnNetprpsStkCd; // 기관순매수종목코드

        @JsonProperty("orgn_netprps_stk_nm")
        private String orgnNetprpsStkNm; // 기관순매수종목명

        @JsonProperty("orgn_netprps_amt")
        private String orgnNetprpsAmt; // 기관순매수금액

        @JsonProperty("orgn_netprps_qty")
        private String orgnNetprpsQty; // 기관순매수수량
    }
}
