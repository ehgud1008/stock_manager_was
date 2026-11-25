package com.example.stockanalysis.kiwoom.contract.tr.usa20101;

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
 * 미국주식 현재가 10호가(usa20101) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa20101Response implements KiwoomResult {

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("stk_nm")
    private String stkNm; // 종목명

    @JsonProperty("stk_enm")
    private String stkEnm; // 종목영문명

    @JsonProperty("cur_prc")
    private String curPrc; // 현재가

    @JsonProperty("pred_pre")
    private String predPre; // 전일대비

    @JsonProperty("flu_rt")
    private String fluRt; // 등락률

    @JsonProperty("trde_qty")
    private String trdeQty; // 거래량

    @JsonProperty("trde_prica")
    private String trdePrica; // 거래대금

    @JsonProperty("open_pric")
    private String openPric; // 시가

    @JsonProperty("high_pric")
    private String highPric; // 고가

    @JsonProperty("low_pric")
    private String lowPric; // 저가

    @JsonProperty("bid_tm")
    private String bidTm; // 호가시간

    @JsonProperty("dt")
    private String dt; // 일자

    @JsonProperty("pred_pre_sig")
    private String predPreSig; // 전일대비기호

    @JsonProperty("fpr_sel_bid")
    private String fprSelBid; // 최우선매도호가

    @JsonProperty("fpr_buy_bid")
    private String fprBuyBid; // 최우선매수호가

    @JsonProperty("pre_trde_rt")
    private String preTrdeRt; // 전일거래대비

    @JsonProperty("trde_tern_rt")
    private String trdeTernRt; // 거래회전율

    @JsonProperty("sel_1bid")
    private String sel1bid; // 매도1호가

    @JsonProperty("sel_2bid")
    private String sel2bid; // 매도2호가

    @JsonProperty("sel_3bid")
    private String sel3bid; // 매도3호가

    @JsonProperty("sel_4bid")
    private String sel4bid; // 매도4호가

    @JsonProperty("sel_5bid")
    private String sel5bid; // 매도5호가

    @JsonProperty("sel_6bid")
    private String sel6bid; // 매도6호가

    @JsonProperty("sel_7bid")
    private String sel7bid; // 매도7호가

    @JsonProperty("sel_8bid")
    private String sel8bid; // 매도8호가

    @JsonProperty("sel_9bid")
    private String sel9bid; // 매도9호가

    @JsonProperty("sel_10bid")
    private String sel10bid; // 매도10호가

    @JsonProperty("buy_1bid")
    private String buy1bid; // 매수1호가

    @JsonProperty("buy_2bid")
    private String buy2bid; // 매수2호가

    @JsonProperty("buy_3bid")
    private String buy3bid; // 매수3호가

    @JsonProperty("buy_4bid")
    private String buy4bid; // 매수4호가

    @JsonProperty("buy_5bid")
    private String buy5bid; // 매수5호가

    @JsonProperty("buy_6bid")
    private String buy6bid; // 매수6호가

    @JsonProperty("buy_7bid")
    private String buy7bid; // 매수7호가

    @JsonProperty("buy_8bid")
    private String buy8bid; // 매수8호가

    @JsonProperty("buy_9bid")
    private String buy9bid; // 매수9호가

    @JsonProperty("buy_10bid")
    private String buy10bid; // 매수10호가

    @JsonProperty("sel_1bid_req")
    private String sel1bidReq; // 매도1호가잔량

    @JsonProperty("sel_2bid_req")
    private String sel2bidReq; // 매도2호가잔량

    @JsonProperty("sel_3bid_req")
    private String sel3bidReq; // 매도3호가잔량

    @JsonProperty("sel_4bid_req")
    private String sel4bidReq; // 매도4호가잔량

    @JsonProperty("sel_5bid_req")
    private String sel5bidReq; // 매도5호가잔량

    @JsonProperty("sel_6bid_req")
    private String sel6bidReq; // 매도6호가잔량

    @JsonProperty("sel_7bid_req")
    private String sel7bidReq; // 매도7호가잔량

    @JsonProperty("sel_8bid_req")
    private String sel8bidReq; // 매도8호가잔량

    @JsonProperty("sel_9bid_req")
    private String sel9bidReq; // 매도9호가잔량

    @JsonProperty("sel_10bid_req")
    private String sel10bidReq; // 매도10호가잔량

    @JsonProperty("buy_1bid_req")
    private String buy1bidReq; // 매수1호가잔량

    @JsonProperty("buy_2bid_req")
    private String buy2bidReq; // 매수2호가잔량

    @JsonProperty("buy_3bid_req")
    private String buy3bidReq; // 매수3호가잔량

    @JsonProperty("buy_4bid_req")
    private String buy4bidReq; // 매수4호가잔량

    @JsonProperty("buy_5bid_req")
    private String buy5bidReq; // 매수5호가잔량

    @JsonProperty("buy_6bid_req")
    private String buy6bidReq; // 매수6호가잔량

    @JsonProperty("buy_7bid_req")
    private String buy7bidReq; // 매수7호가잔량

    @JsonProperty("buy_8bid_req")
    private String buy8bidReq; // 매수8호가잔량

    @JsonProperty("buy_9bid_req")
    private String buy9bidReq; // 매수9호가잔량

    @JsonProperty("buy_10bid_req")
    private String buy10bidReq; // 매수10호가잔량

    @JsonProperty("sel_1bid_jub_pre")
    private String sel1bidJubPre; // 매도1호가직전대비

    @JsonProperty("sel_2bid_jub_pre")
    private String sel2bidJubPre; // 매도2호가직전대비

    @JsonProperty("sel_3bid_jub_pre")
    private String sel3bidJubPre; // 매도3호가직전대비

    @JsonProperty("sel_4bid_jub_pre")
    private String sel4bidJubPre; // 매도4호가직전대비

    @JsonProperty("sel_5bid_jub_pre")
    private String sel5bidJubPre; // 매도5호가직전대비

    @JsonProperty("sel_6bid_jub_pre")
    private String sel6bidJubPre; // 매도6호가직전대비

    @JsonProperty("sel_7bid_jub_pre")
    private String sel7bidJubPre; // 매도7호가직전대비

    @JsonProperty("sel_8bid_jub_pre")
    private String sel8bidJubPre; // 매도8호가직전대비

    @JsonProperty("sel_9bid_jub_pre")
    private String sel9bidJubPre; // 매도9호가직전대비

    @JsonProperty("sel_10bid_jub_pre")
    private String sel10bidJubPre; // 매도10호가직전대비

    @JsonProperty("buy_1th_pre_req_pre")
    private String buy1thPreReqPre; // 매수1차선잔량대비

    @JsonProperty("buy_1bid_jub_pre")
    private String buy1bidJubPre; // 매수1호가직전대비

    @JsonProperty("buy_2bid_jub_pre")
    private String buy2bidJubPre; // 매수2호가직전대비

    @JsonProperty("buy_3bid_jub_pre")
    private String buy3bidJubPre; // 매수3호가직전대비

    @JsonProperty("buy_4bid_jub_pre")
    private String buy4bidJubPre; // 매수4호가직전대비

    @JsonProperty("buy_5bid_jub_pre")
    private String buy5bidJubPre; // 매수5호가직전대비

    @JsonProperty("buy_6bid_jub_pre")
    private String buy6bidJubPre; // 매수6호가직전대비

    @JsonProperty("buy_7bid_jub_pre")
    private String buy7bidJubPre; // 매수7호가직전대비

    @JsonProperty("buy_8bid_jub_pre")
    private String buy8bidJubPre; // 매수8호가직전대비

    @JsonProperty("buy_9bid_jub_pre")
    private String buy9bidJubPre; // 매수9호가직전대비

    @JsonProperty("buy_10bid_jub_pre")
    private String buy10bidJubPre; // 매수10호가직전대비

    @JsonProperty("tot_sel_req")
    private String totSelReq; // 총매도잔량

    @JsonProperty("sel_bid_tot_req_jub_pre")
    private String selBidTotReqJubPre; // 매도호가총잔량직전대비

    @JsonProperty("tot_buy_req")
    private String totBuyReq; // 총매수잔량

    @JsonProperty("buy_bid_tot_req_jub_pre")
    private String buyBidTotReqJubPre; // 매수호가총잔량직전대비

    @JsonProperty("netprps_req")
    private String netprpsReq; // 순매수잔량

    @JsonProperty("netslmt_req")
    private String netslmtReq; // 순매도잔량

    @JsonProperty("upl_pric")
    private String uplPric; // 상한가

    @JsonProperty("base_pric")
    private String basePric; // 기준가

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
