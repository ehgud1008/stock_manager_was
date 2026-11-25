package com.example.stockanalysis.kiwoom.contract.tr.ka10007;

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
 * 시세표성정보요청(ka10007) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10007Response implements KiwoomResult {

    @JsonProperty("stk_nm")
    private String stkNm; // 종목명

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("date")
    private String date; // 날짜

    @JsonProperty("tm")
    private String tm; // 시간

    @JsonProperty("pred_close_pric")
    private String predClosePric; // 전일종가

    @JsonProperty("pred_trde_qty")
    private String predTrdeQty; // 전일거래량

    @JsonProperty("upl_pric")
    private String uplPric; // 상한가

    @JsonProperty("lst_pric")
    private String lstPric; // 하한가

    @JsonProperty("pred_trde_prica")
    private String predTrdePrica; // 전일거래대금

    @JsonProperty("flo_stkcnt")
    private String floStkcnt; // 상장주식수

    @JsonProperty("cur_prc")
    private String curPrc; // 현재가

    @JsonProperty("smbol")
    private String smbol; // 부호

    @JsonProperty("flu_rt")
    private String fluRt; // 등락률

    @JsonProperty("pred_rt")
    private String predRt; // 전일비

    @JsonProperty("open_pric")
    private String openPric; // 시가

    @JsonProperty("high_pric")
    private String highPric; // 고가

    @JsonProperty("low_pric")
    private String lowPric; // 저가

    @JsonProperty("cntr_qty")
    private String cntrQty; // 체결량

    @JsonProperty("trde_qty")
    private String trdeQty; // 거래량

    @JsonProperty("trde_prica")
    private String trdePrica; // 거래대금

    @JsonProperty("exp_cntr_pric")
    private String expCntrPric; // 예상체결가

    @JsonProperty("exp_cntr_qty")
    private String expCntrQty; // 예상체결량

    @JsonProperty("exp_sel_pri_bid")
    private String expSelPriBid; // 예상매도우선호가

    @JsonProperty("exp_buy_pri_bid")
    private String expBuyPriBid; // 예상매수우선호가

    @JsonProperty("trde_strt_dt")
    private String trdeStrtDt; // 거래시작일

    @JsonProperty("exec_pric")
    private String execPric; // 행사가격

    @JsonProperty("hgst_pric")
    private String hgstPric; // 최고가

    @JsonProperty("lwst_pric")
    private String lwstPric; // 최저가

    @JsonProperty("hgst_pric_dt")
    private String hgstPricDt; // 최고가일

    @JsonProperty("lwst_pric_dt")
    private String lwstPricDt; // 최저가일

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

    @JsonProperty("sel_1bid_cnt")
    private String sel1bidCnt; // 매도1호가건수

    @JsonProperty("sel_2bid_cnt")
    private String sel2bidCnt; // 매도2호가건수

    @JsonProperty("sel_3bid_cnt")
    private String sel3bidCnt; // 매도3호가건수

    @JsonProperty("sel_4bid_cnt")
    private String sel4bidCnt; // 매도4호가건수

    @JsonProperty("sel_5bid_cnt")
    private String sel5bidCnt; // 매도5호가건수

    @JsonProperty("buy_1bid_cnt")
    private String buy1bidCnt; // 매수1호가건수

    @JsonProperty("buy_2bid_cnt")
    private String buy2bidCnt; // 매수2호가건수

    @JsonProperty("buy_3bid_cnt")
    private String buy3bidCnt; // 매수3호가건수

    @JsonProperty("buy_4bid_cnt")
    private String buy4bidCnt; // 매수4호가건수

    @JsonProperty("buy_5bid_cnt")
    private String buy5bidCnt; // 매수5호가건수

    @JsonProperty("lpsel_1bid_req")
    private String lpsel1bidReq; // LP매도1호가잔량

    @JsonProperty("lpsel_2bid_req")
    private String lpsel2bidReq; // LP매도2호가잔량

    @JsonProperty("lpsel_3bid_req")
    private String lpsel3bidReq; // LP매도3호가잔량

    @JsonProperty("lpsel_4bid_req")
    private String lpsel4bidReq; // LP매도4호가잔량

    @JsonProperty("lpsel_5bid_req")
    private String lpsel5bidReq; // LP매도5호가잔량

    @JsonProperty("lpsel_6bid_req")
    private String lpsel6bidReq; // LP매도6호가잔량

    @JsonProperty("lpsel_7bid_req")
    private String lpsel7bidReq; // LP매도7호가잔량

    @JsonProperty("lpsel_8bid_req")
    private String lpsel8bidReq; // LP매도8호가잔량

    @JsonProperty("lpsel_9bid_req")
    private String lpsel9bidReq; // LP매도9호가잔량

    @JsonProperty("lpsel_10bid_req")
    private String lpsel10bidReq; // LP매도10호가잔량

    @JsonProperty("lpbuy_1bid_req")
    private String lpbuy1bidReq; // LP매수1호가잔량

    @JsonProperty("lpbuy_2bid_req")
    private String lpbuy2bidReq; // LP매수2호가잔량

    @JsonProperty("lpbuy_3bid_req")
    private String lpbuy3bidReq; // LP매수3호가잔량

    @JsonProperty("lpbuy_4bid_req")
    private String lpbuy4bidReq; // LP매수4호가잔량

    @JsonProperty("lpbuy_5bid_req")
    private String lpbuy5bidReq; // LP매수5호가잔량

    @JsonProperty("lpbuy_6bid_req")
    private String lpbuy6bidReq; // LP매수6호가잔량

    @JsonProperty("lpbuy_7bid_req")
    private String lpbuy7bidReq; // LP매수7호가잔량

    @JsonProperty("lpbuy_8bid_req")
    private String lpbuy8bidReq; // LP매수8호가잔량

    @JsonProperty("lpbuy_9bid_req")
    private String lpbuy9bidReq; // LP매수9호가잔량

    @JsonProperty("lpbuy_10bid_req")
    private String lpbuy10bidReq; // LP매수10호가잔량

    @JsonProperty("tot_buy_req")
    private String totBuyReq; // 총매수잔량

    @JsonProperty("tot_sel_req")
    private String totSelReq; // 총매도잔량

    @JsonProperty("tot_buy_cnt")
    private String totBuyCnt; // 총매수건수

    @JsonProperty("tot_sel_cnt")
    private String totSelCnt; // 총매도건수

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
