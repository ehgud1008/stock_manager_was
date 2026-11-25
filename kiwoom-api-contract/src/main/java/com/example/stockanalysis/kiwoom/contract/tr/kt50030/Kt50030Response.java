package com.example.stockanalysis.kiwoom.contract.tr.kt50030;

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
 * 금현물 주문체결전체조회(kt50030) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt50030Response implements KiwoomResult {

    @JsonProperty("acnt_ord_cntr_prst")
    private List<AcntOrdCntrPrstItem> acntOrdCntrPrst; // 계좌별주문체결현황

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
    public static class AcntOrdCntrPrstItem {

        @JsonProperty("stk_bond_tp")
        private String stkBondTp; // 주식채권구분

        @JsonProperty("ord_no")
        private String ordNo; // 주문번호

        @JsonProperty("stk_cd")
        private String stkCd; // 상품코드

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

        @JsonProperty("data_send_end_tp")
        private String dataSendEndTp; // 접수구분

        @JsonProperty("mrkt_deal_tp")
        private String mrktDealTp; // 시장구분

        @JsonProperty("rsrv_tp")
        private String rsrvTp; // 예약/반대여부

        @JsonProperty("orig_ord_no")
        private String origOrdNo; // 원주문번호

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("dcd_tp_nm")
        private String dcdTpNm; // 결제구분

        @JsonProperty("crd_deal_tp")
        private String crdDealTp; // 신용거래구분

        @JsonProperty("cntr_qty")
        private String cntrQty; // 체결수량

        @JsonProperty("cntr_uv")
        private String cntrUv; // 체결단가

        @JsonProperty("ord_remnq")
        private String ordRemnq; // 미체결수량

        @JsonProperty("comm_ord_tp")
        private String commOrdTp; // 통신구분

        @JsonProperty("mdfy_cncl_tp")
        private String mdfyCnclTp; // 정정취소구분

        @JsonProperty("dmst_stex_tp")
        private String dmstStexTp; // 국내거래소구분

        @JsonProperty("cond_uv")
        private String condUv; // 스톱가
    }
}
