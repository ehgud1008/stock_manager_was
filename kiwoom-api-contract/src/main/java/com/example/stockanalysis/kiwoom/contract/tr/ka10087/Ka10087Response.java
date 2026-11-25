package com.example.stockanalysis.kiwoom.contract.tr.ka10087;

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
 * 시간외단일가요청(ka10087) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10087Response implements KiwoomResult {

    @JsonProperty("bid_req_base_tm")
    private String bidReqBaseTm; // 호가잔량기준시간

    @JsonProperty("ovt_sigpric_sel_bid_jub_pre_5")
    private String ovtSigpricSelBidJubPre5; // 시간외단일가_매도호가직전대비5

    @JsonProperty("ovt_sigpric_sel_bid_jub_pre_4")
    private String ovtSigpricSelBidJubPre4; // 시간외단일가_매도호가직전대비4

    @JsonProperty("ovt_sigpric_sel_bid_jub_pre_3")
    private String ovtSigpricSelBidJubPre3; // 시간외단일가_매도호가직전대비3

    @JsonProperty("ovt_sigpric_sel_bid_jub_pre_2")
    private String ovtSigpricSelBidJubPre2; // 시간외단일가_매도호가직전대비2

    @JsonProperty("ovt_sigpric_sel_bid_jub_pre_1")
    private String ovtSigpricSelBidJubPre1; // 시간외단일가_매도호가직전대비1

    @JsonProperty("ovt_sigpric_sel_bid_qty_5")
    private String ovtSigpricSelBidQty5; // 시간외단일가_매도호가수량5

    @JsonProperty("ovt_sigpric_sel_bid_qty_4")
    private String ovtSigpricSelBidQty4; // 시간외단일가_매도호가수량4

    @JsonProperty("ovt_sigpric_sel_bid_qty_3")
    private String ovtSigpricSelBidQty3; // 시간외단일가_매도호가수량3

    @JsonProperty("ovt_sigpric_sel_bid_qty_2")
    private String ovtSigpricSelBidQty2; // 시간외단일가_매도호가수량2

    @JsonProperty("ovt_sigpric_sel_bid_qty_1")
    private String ovtSigpricSelBidQty1; // 시간외단일가_매도호가수량1

    @JsonProperty("ovt_sigpric_sel_bid_5")
    private String ovtSigpricSelBid5; // 시간외단일가_매도호가5

    @JsonProperty("ovt_sigpric_sel_bid_4")
    private String ovtSigpricSelBid4; // 시간외단일가_매도호가4

    @JsonProperty("ovt_sigpric_sel_bid_3")
    private String ovtSigpricSelBid3; // 시간외단일가_매도호가3

    @JsonProperty("ovt_sigpric_sel_bid_2")
    private String ovtSigpricSelBid2; // 시간외단일가_매도호가2

    @JsonProperty("ovt_sigpric_sel_bid_1")
    private String ovtSigpricSelBid1; // 시간외단일가_매도호가1

    @JsonProperty("ovt_sigpric_buy_bid_1")
    private String ovtSigpricBuyBid1; // 시간외단일가_매수호가1

    @JsonProperty("ovt_sigpric_buy_bid_2")
    private String ovtSigpricBuyBid2; // 시간외단일가_매수호가2

    @JsonProperty("ovt_sigpric_buy_bid_3")
    private String ovtSigpricBuyBid3; // 시간외단일가_매수호가3

    @JsonProperty("ovt_sigpric_buy_bid_4")
    private String ovtSigpricBuyBid4; // 시간외단일가_매수호가4

    @JsonProperty("ovt_sigpric_buy_bid_5")
    private String ovtSigpricBuyBid5; // 시간외단일가_매수호가5

    @JsonProperty("ovt_sigpric_buy_bid_qty_1")
    private String ovtSigpricBuyBidQty1; // 시간외단일가_매수호가수량1

    @JsonProperty("ovt_sigpric_buy_bid_qty_2")
    private String ovtSigpricBuyBidQty2; // 시간외단일가_매수호가수량2

    @JsonProperty("ovt_sigpric_buy_bid_qty_3")
    private String ovtSigpricBuyBidQty3; // 시간외단일가_매수호가수량3

    @JsonProperty("ovt_sigpric_buy_bid_qty_4")
    private String ovtSigpricBuyBidQty4; // 시간외단일가_매수호가수량4

    @JsonProperty("ovt_sigpric_buy_bid_qty_5")
    private String ovtSigpricBuyBidQty5; // 시간외단일가_매수호가수량5

    @JsonProperty("ovt_sigpric_buy_bid_jub_pre_1")
    private String ovtSigpricBuyBidJubPre1; // 시간외단일가_매수호가직전대비1

    @JsonProperty("ovt_sigpric_buy_bid_jub_pre_2")
    private String ovtSigpricBuyBidJubPre2; // 시간외단일가_매수호가직전대비2

    @JsonProperty("ovt_sigpric_buy_bid_jub_pre_3")
    private String ovtSigpricBuyBidJubPre3; // 시간외단일가_매수호가직전대비3

    @JsonProperty("ovt_sigpric_buy_bid_jub_pre_4")
    private String ovtSigpricBuyBidJubPre4; // 시간외단일가_매수호가직전대비4

    @JsonProperty("ovt_sigpric_buy_bid_jub_pre_5")
    private String ovtSigpricBuyBidJubPre5; // 시간외단일가_매수호가직전대비5

    @JsonProperty("ovt_sigpric_sel_bid_tot_req")
    private String ovtSigpricSelBidTotReq; // 시간외단일가_매도호가총잔량

    @JsonProperty("ovt_sigpric_buy_bid_tot_req")
    private String ovtSigpricBuyBidTotReq; // 시간외단일가_매수호가총잔량

    @JsonProperty("sel_bid_tot_req_jub_pre")
    private String selBidTotReqJubPre; // 매도호가총잔량직전대비

    @JsonProperty("sel_bid_tot_req")
    private String selBidTotReq; // 매도호가총잔량

    @JsonProperty("buy_bid_tot_req")
    private String buyBidTotReq; // 매수호가총잔량

    @JsonProperty("buy_bid_tot_req_jub_pre")
    private String buyBidTotReqJubPre; // 매수호가총잔량직전대비

    @JsonProperty("ovt_sel_bid_tot_req_jub_pre")
    private String ovtSelBidTotReqJubPre; // 시간외매도호가총잔량직전대비

    @JsonProperty("ovt_sel_bid_tot_req")
    private String ovtSelBidTotReq; // 시간외매도호가총잔량

    @JsonProperty("ovt_buy_bid_tot_req")
    private String ovtBuyBidTotReq; // 시간외매수호가총잔량

    @JsonProperty("ovt_buy_bid_tot_req_jub_pre")
    private String ovtBuyBidTotReqJubPre; // 시간외매수호가총잔량직전대비

    @JsonProperty("ovt_sigpric_cur_prc")
    private String ovtSigpricCurPrc; // 시간외단일가_현재가

    @JsonProperty("ovt_sigpric_pred_pre_sig")
    private String ovtSigpricPredPreSig; // 시간외단일가_전일대비기호

    @JsonProperty("ovt_sigpric_pred_pre")
    private String ovtSigpricPredPre; // 시간외단일가_전일대비

    @JsonProperty("ovt_sigpric_flu_rt")
    private String ovtSigpricFluRt; // 시간외단일가_등락률

    @JsonProperty("ovt_sigpric_acc_trde_qty")
    private String ovtSigpricAccTrdeQty; // 시간외단일가_누적거래량

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
