package com.example.stockanalysis.kiwoom.contract.tr.ka10131;

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
 * 기관외국인연속매매현황요청(ka10131) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10131Response implements KiwoomResult {

    @JsonProperty("orgn_frgnr_cont_trde_prst")
    private List<OrgnFrgnrContTrdePrstItem> orgnFrgnrContTrdePrst; // 기관외국인연속매매현황

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
    public static class OrgnFrgnrContTrdePrstItem {

        @JsonProperty("rank")
        private String rank; // 순위

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("prid_stkpc_flu_rt")
        private String pridStkpcFluRt; // 기간중주가등락률

        @JsonProperty("orgn_nettrde_amt")
        private String orgnNettrdeAmt; // 기관순매매금액

        @JsonProperty("orgn_nettrde_qty")
        private String orgnNettrdeQty; // 기관순매매량

        @JsonProperty("orgn_cont_netprps_dys")
        private String orgnContNetprpsDys; // 기관계연속순매수일수

        @JsonProperty("orgn_cont_netprps_qty")
        private String orgnContNetprpsQty; // 기관계연속순매수량

        @JsonProperty("orgn_cont_netprps_amt")
        private String orgnContNetprpsAmt; // 기관계연속순매수금액

        @JsonProperty("frgnr_nettrde_qty")
        private String frgnrNettrdeQty; // 외국인순매매량

        @JsonProperty("frgnr_nettrde_amt")
        private String frgnrNettrdeAmt; // 외국인순매매액

        @JsonProperty("frgnr_cont_netprps_dys")
        private String frgnrContNetprpsDys; // 외국인연속순매수일수

        @JsonProperty("frgnr_cont_netprps_qty")
        private String frgnrContNetprpsQty; // 외국인연속순매수량

        @JsonProperty("frgnr_cont_netprps_amt")
        private String frgnrContNetprpsAmt; // 외국인연속순매수금액

        @JsonProperty("nettrde_qty")
        private String nettrdeQty; // 순매매량

        @JsonProperty("nettrde_amt")
        private String nettrdeAmt; // 순매매액

        @JsonProperty("tot_cont_netprps_dys")
        private String totContNetprpsDys; // 합계연속순매수일수

        @JsonProperty("tot_cont_nettrde_qty")
        private String totContNettrdeQty; // 합계연속순매매수량

        @JsonProperty("tot_cont_netprps_amt")
        private String totContNetprpsAmt; // 합계연속순매수금액
    }
}
