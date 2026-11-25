package com.example.stockanalysis.kiwoom.contract.tr.ka10095;

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
 * 지정종목 정보요청(ka10095) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10095Response implements KiwoomResult {

    @JsonProperty("atn_stk_infr")
    private List<AtnStkInfrItem> atnStkInfr; // 관심종목정보

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
    public static class AtnStkInfrItem {

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("base_pric")
        private String basePric; // 기준가

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("pred_pre_sig")
        private String predPreSig; // 전일대비기호

        @JsonProperty("flu_rt")
        private String fluRt; // 등락율

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("trde_prica")
        private String trdePrica; // 거래대금

        @JsonProperty("cntr_qty")
        private String cntrQty; // 체결량

        @JsonProperty("cntr_str")
        private String cntrStr; // 체결강도

        @JsonProperty("pred_trde_qty_pre")
        private String predTrdeQtyPre; // 전일거래량대비

        @JsonProperty("sel_bid")
        private String selBid; // 매도호가

        @JsonProperty("buy_bid")
        private String buyBid; // 매수호가

        @JsonProperty("sel_1th_bid")
        private String sel1thBid; // 매도1차호가

        @JsonProperty("sel_2th_bid")
        private String sel2thBid; // 매도2차호가

        @JsonProperty("sel_3th_bid")
        private String sel3thBid; // 매도3차호가

        @JsonProperty("sel_4th_bid")
        private String sel4thBid; // 매도4차호가

        @JsonProperty("sel_5th_bid")
        private String sel5thBid; // 매도5차호가

        @JsonProperty("buy_1th_bid")
        private String buy1thBid; // 매수1차호가

        @JsonProperty("buy_2th_bid")
        private String buy2thBid; // 매수2차호가

        @JsonProperty("buy_3th_bid")
        private String buy3thBid; // 매수3차호가

        @JsonProperty("buy_4th_bid")
        private String buy4thBid; // 매수4차호가

        @JsonProperty("buy_5th_bid")
        private String buy5thBid; // 매수5차호가

        @JsonProperty("upl_pric")
        private String uplPric; // 상한가

        @JsonProperty("lst_pric")
        private String lstPric; // 하한가

        @JsonProperty("open_pric")
        private String openPric; // 시가

        @JsonProperty("high_pric")
        private String highPric; // 고가

        @JsonProperty("low_pric")
        private String lowPric; // 저가

        @JsonProperty("close_pric")
        private String closePric; // 종가

        @JsonProperty("cntr_tm")
        private String cntrTm; // 체결시간

        @JsonProperty("exp_cntr_pric")
        private String expCntrPric; // 예상체결가

        @JsonProperty("exp_cntr_qty")
        private String expCntrQty; // 예상체결량

        @JsonProperty("cap")
        private String cap; // 자본금

        @JsonProperty("fav")
        private String fav; // 액면가

        @JsonProperty("mac")
        private String mac; // 시가총액

        @JsonProperty("stkcnt")
        private String stkcnt; // 주식수

        @JsonProperty("bid_tm")
        private String bidTm; // 호가시간

        @JsonProperty("dt")
        private String dt; // 일자

        @JsonProperty("pri_sel_req")
        private String priSelReq; // 우선매도잔량

        @JsonProperty("pri_buy_req")
        private String priBuyReq; // 우선매수잔량

        @JsonProperty("pri_sel_cnt")
        private String priSelCnt; // 우선매도건수

        @JsonProperty("pri_buy_cnt")
        private String priBuyCnt; // 우선매수건수

        @JsonProperty("tot_sel_req")
        private String totSelReq; // 총매도잔량

        @JsonProperty("tot_buy_req")
        private String totBuyReq; // 총매수잔량

        @JsonProperty("tot_sel_cnt")
        private String totSelCnt; // 총매도건수

        @JsonProperty("tot_buy_cnt")
        private String totBuyCnt; // 총매수건수

        @JsonProperty("prty")
        private String prty; // 패리티

        @JsonProperty("gear")
        private String gear; // 기어링

        @JsonProperty("pl_qutr")
        private String plQutr; // 손익분기

        @JsonProperty("cap_support")
        private String capSupport; // 자본지지

        @JsonProperty("elwexec_pric")
        private String elwexecPric; // ELW행사가

        @JsonProperty("cnvt_rt")
        private String cnvtRt; // 전환비율

        @JsonProperty("elwexpr_dt")
        private String elwexprDt; // ELW만기일

        @JsonProperty("cntr_engg")
        private String cntrEngg; // 미결제약정

        @JsonProperty("cntr_pred_pre")
        private String cntrPredPre; // 미결제전일대비

        @JsonProperty("theory_pric")
        private String theoryPric; // 이론가

        @JsonProperty("innr_vltl")
        private String innrVltl; // 내재변동성

        @JsonProperty("delta")
        private String delta; // 델타

        @JsonProperty("gam")
        private String gam; // 감마

        @JsonProperty("theta")
        private String theta; // 쎄타

        @JsonProperty("vega")
        private String vega; // 베가

        @JsonProperty("law")
        private String law; // 로
    }
}
