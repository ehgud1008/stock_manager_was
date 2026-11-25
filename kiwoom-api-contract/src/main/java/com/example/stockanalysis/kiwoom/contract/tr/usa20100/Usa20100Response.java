package com.example.stockanalysis.kiwoom.contract.tr.usa20100;

import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 현재가 종목정보(usa20100) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa20100Response implements KiwoomResult {

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("stk_nm")
    private String stkNm; // 종목명

    @JsonProperty("stk_enm")
    private String stkEnm; // 종목영문명

    @JsonProperty("52wk_hgst_pric")
    private String _52wkHgstPric; // 52주 최고가

    @JsonProperty("52wk_hgst_pric_pre_rt")
    private String _52wkHgstPricPreRt; // 52주 최고가 대비율

    @JsonProperty("52wk_hgst_pric_dt")
    private String _52wkHgstPricDt; // 52주 최고가일

    @JsonProperty("52wk_lwst_pric")
    private String _52wkLwstPric; // 52주 최저가

    @JsonProperty("52wk_lwst_pric_pre_rt")
    private String _52wkLwstPricPreRt; // 52주 최저가 대비율

    @JsonProperty("52wk_lwst_pric_dt")
    private String _52wkLwstPricDt; // 52주 최저가일

    @JsonProperty("stk_cnt")
    private String stkCnt; // 주식수

    @JsonProperty("mac")
    private String mac; // 시가총액

    @JsonProperty("setl_mm")
    private String setlMm; // 결산월

    @JsonProperty("lg_inds_cd")
    private String lgIndsCd; // 대업종구분

    @JsonProperty("sm_inds_cd")
    private String smIndsCd; // 소업종구분

    @JsonProperty("cur_prc")
    private String curPrc; // 현재가

    @JsonProperty("pred_pre_sig")
    private String predPreSig; // 전일대비기호

    @JsonProperty("pred_pre")
    private String predPre; // 전일대비

    @JsonProperty("flu_rt")
    private String fluRt; // 등락률

    @JsonProperty("acc_trde_qty")
    private String accTrdeQty; // 누적거래량

    @JsonProperty("oyr_hgst")
    private String oyrHgst; // 연중최고가

    @JsonProperty("oyr_hgst_dt")
    private String oyrHgstDt; // 연중최고가일

    @JsonProperty("oyr_hgst_pre_rt")
    private String oyrHgstPreRt; // 연중최고가 대비율

    @JsonProperty("oyr_lwst")
    private String oyrLwst; // 연중최저가

    @JsonProperty("oyr_lwst_dt")
    private String oyrLwstDt; // 연중최저가일

    @JsonProperty("oyr_lwst_pre_rt")
    private String oyrLwstPreRt; // 연중최저가 대비율

    @JsonProperty("pre_open_pric")
    private String preOpenPric; // 전일시가

    @JsonProperty("pre_high_pric")
    private String preHighPric; // 전일고가

    @JsonProperty("pre_low_pric")
    private String preLowPric; // 전일저가

    @JsonProperty("base_close_pric")
    private String baseClosePric; // 전일종가

    @JsonProperty("upl_pric")
    private String uplPric; // 상한가

    @JsonProperty("lst_pric")
    private String lstPric; // 하한가

    @JsonProperty("trde_qty_unit")
    private String trdeQtyUnit; // 매매수량단위

    @JsonProperty("uncert_lv")
    private String uncertLv; // 불확실성

    @JsonProperty("comp_adv_tp")
    private String compAdvTp; // 경쟁우위

    @JsonProperty("curr_unit")
    private String currUnit; // 통화단위

    @JsonProperty("open_pric")
    private String openPric; // 시가

    @JsonProperty("high_pric")
    private String highPric; // 고가

    @JsonProperty("low_pric")
    private String lowPric; // 저가

    @JsonProperty("trd_susp_tp")
    private String trdSuspTp; // 거래정지여부

    @JsonProperty("base_exrt")
    private String baseExrt; // 환율

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
