package com.example.stockanalysis.kiwoom.contract.tr.ka10075;

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
 * 미체결요청(ka10075) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10075Response implements KiwoomResult {

    @JsonProperty("oso")
    private List<OsoItem> oso; // 미체결

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
    public static class OsoItem {

        @JsonProperty("acnt_no")
        private String acntNo; // 계좌번호

        @JsonProperty("ord_no")
        private String ordNo; // 주문번호

        @JsonProperty("mang_empno")
        private String mangEmpno; // 관리사번

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("tsk_tp")
        private String tskTp; // 업무구분

        @JsonProperty("ord_stt")
        private String ordStt; // 주문상태

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("ord_qty")
        private String ordQty; // 주문수량

        @JsonProperty("ord_pric")
        private String ordPric; // 주문가격

        @JsonProperty("oso_qty")
        private String osoQty; // 미체결수량

        @JsonProperty("cntr_tot_amt")
        private String cntrTotAmt; // 체결누계금액

        @JsonProperty("orig_ord_no")
        private String origOrdNo; // 원주문번호

        @JsonProperty("io_tp_nm")
        private String ioTpNm; // 주문구분

        @JsonProperty("trde_tp")
        private String trdeTp; // 매매구분

        @JsonProperty("tm")
        private String tm; // 시간

        @JsonProperty("cntr_no")
        private String cntrNo; // 체결번호

        @JsonProperty("cntr_pric")
        private String cntrPric; // 체결가

        @JsonProperty("cntr_qty")
        private String cntrQty; // 체결량

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("sel_bid")
        private String selBid; // 매도호가

        @JsonProperty("buy_bid")
        private String buyBid; // 매수호가

        @JsonProperty("unit_cntr_pric")
        private String unitCntrPric; // 단위체결가

        @JsonProperty("unit_cntr_qty")
        private String unitCntrQty; // 단위체결량

        @JsonProperty("tdy_trde_cmsn")
        private String tdyTrdeCmsn; // 당일매매수수료

        @JsonProperty("tdy_trde_tax")
        private String tdyTrdeTax; // 당일매매세금

        @JsonProperty("ind_invsr")
        private String indInvsr; // 개인투자자

        @JsonProperty("stex_tp")
        private String stexTp; // 거래소구분

        @JsonProperty("stex_tp_txt")
        private String stexTpTxt; // 거래소구분텍스트

        @JsonProperty("sor_yn")
        private String sorYn; // SOR 여부값

        @JsonProperty("stop_pric")
        private String stopPric; // 스톱가
    }
}
