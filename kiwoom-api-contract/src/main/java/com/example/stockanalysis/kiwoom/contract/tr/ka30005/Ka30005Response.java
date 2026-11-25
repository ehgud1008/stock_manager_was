package com.example.stockanalysis.kiwoom.contract.tr.ka30005;

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
 * ELW조건검색요청(ka30005) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka30005Response implements KiwoomResult {

    @JsonProperty("elwcnd_qry")
    private List<ElwcndQryItem> elwcndQry; // ELW조건검색

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
    public static class ElwcndQryItem {

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("isscomp_nm")
        private String isscompNm; // 발행사명

        @JsonProperty("sqnc")
        private String sqnc; // 회차

        @JsonProperty("base_aset_nm")
        private String baseAsetNm; // 기초자산명

        @JsonProperty("rght_tp")
        private String rghtTp; // 권리구분

        @JsonProperty("expr_dt")
        private String exprDt; // 만기일

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("pre_tp")
        private String preTp; // 대비구분

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("flu_rt")
        private String fluRt; // 등락율

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("trde_qty_pre")
        private String trdeQtyPre; // 거래량대비

        @JsonProperty("trde_prica")
        private String trdePrica; // 거래대금

        @JsonProperty("pred_trde_qty")
        private String predTrdeQty; // 전일거래량

        @JsonProperty("sel_bid")
        private String selBid; // 매도호가

        @JsonProperty("buy_bid")
        private String buyBid; // 매수호가

        @JsonProperty("prty")
        private String prty; // 패리티

        @JsonProperty("gear_rt")
        private String gearRt; // 기어링비율

        @JsonProperty("pl_qutr_rt")
        private String plQutrRt; // 손익분기율

        @JsonProperty("cfp")
        private String cfp; // 자본지지점

        @JsonProperty("theory_pric")
        private String theoryPric; // 이론가

        @JsonProperty("innr_vltl")
        private String innrVltl; // 내재변동성

        @JsonProperty("delta")
        private String delta; // 델타

        @JsonProperty("lvrg")
        private String lvrg; // 레버리지

        @JsonProperty("exec_pric")
        private String execPric; // 행사가격

        @JsonProperty("cnvt_rt")
        private String cnvtRt; // 전환비율

        @JsonProperty("lpposs_rt")
        private String lppossRt; // LP보유비율

        @JsonProperty("pl_qutr_pt")
        private String plQutrPt; // 손익분기점

        @JsonProperty("fin_trde_dt")
        private String finTrdeDt; // 최종거래일

        @JsonProperty("flo_dt")
        private String floDt; // 상장일

        @JsonProperty("lpinitlast_suply_dt")
        private String lpinitlastSuplyDt; // LP초종공급일

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("srvive_dys")
        private String srviveDys; // 잔존일수

        @JsonProperty("dispty_rt")
        private String disptyRt; // 괴리율

        @JsonProperty("lpmmcm_nm")
        private String lpmmcmNm; // LP회원사명

        @JsonProperty("lpmmcm_nm_1")
        private String lpmmcmNm1; // LP회원사명1

        @JsonProperty("lpmmcm_nm_2")
        private String lpmmcmNm2; // LP회원사명2

        @JsonProperty("xraymont_cntr_qty_arng_trde_tp")
        private String xraymontCntrQtyArngTrdeTp; // Xray순간체결량정리매매구분

        @JsonProperty("xraymont_cntr_qty_profa_100tp")
        private String xraymontCntrQtyProfa100tp; // Xray순간체결량증거금100구분
    }
}
