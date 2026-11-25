package com.example.stockanalysis.kiwoom.contract.tr.kt00009;

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
 * 계좌별주문체결현황요청(kt00009) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt00009Response implements KiwoomResult {

    @JsonProperty("sell_grntl_engg_amt")
    private String sellGrntlEnggAmt; // 매도약정금액

    @JsonProperty("buy_engg_amt")
    private String buyEnggAmt; // 매수약정금액

    @JsonProperty("engg_amt")
    private String enggAmt; // 약정금액

    @JsonProperty("acnt_ord_cntr_prst_array")
    private List<AcntOrdCntrPrstArrayItem> acntOrdCntrPrstArray; // 계좌별주문체결현황배열

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
    public static class AcntOrdCntrPrstArrayItem {

        @JsonProperty("stk_bond_tp")
        private String stkBondTp; // 주식채권구분

        @JsonProperty("ord_no")
        private String ordNo; // 주문번호

        @JsonProperty("stk_cd")
        private String stkCd; // 종목번호

        @JsonProperty("trde_tp")
        private String trdeTp; // 매매구분

        @JsonProperty("io_tp_nm")
        private String ioTpNm; // 주문유형구분

        @JsonProperty("ord_qty")
        private String ordQty; // 주문수량

        @JsonProperty("ord_uv")
        private String ordUv; // 주문단가

        @JsonProperty("cnfm_qty")
        private String cnfmQty; // 확인수량

        @JsonProperty("rsrv_oppo")
        private String rsrvOppo; // 예약/반대

        @JsonProperty("cntr_no")
        private String cntrNo; // 체결번호

        @JsonProperty("acpt_tp")
        private String acptTp; // 접수구분

        @JsonProperty("orig_ord_no")
        private String origOrdNo; // 원주문번호

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("setl_tp")
        private String setlTp; // 결제구분

        @JsonProperty("crd_deal_tp")
        private String crdDealTp; // 신용거래구분

        @JsonProperty("cntr_qty")
        private String cntrQty; // 체결수량

        @JsonProperty("cntr_uv")
        private String cntrUv; // 체결단가

        @JsonProperty("comm_ord_tp")
        private String commOrdTp; // 통신구분

        @JsonProperty("mdfy_cncl_tp")
        private String mdfyCnclTp; // 정정/취소구분

        @JsonProperty("cntr_tm")
        private String cntrTm; // 체결시간

        @JsonProperty("dmst_stex_tp")
        private String dmstStexTp; // 국내거래소구분

        @JsonProperty("cond_uv")
        private String condUv; // 스톱가
    }
}
