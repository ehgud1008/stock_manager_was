package com.example.stockanalysis.kiwoom.contract.tr.kt50031;

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
 * 금현물 주문체결조회(kt50031) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt50031Response implements KiwoomResult {

    @JsonProperty("acnt_ord_cntr_prps_dtl")
    private List<AcntOrdCntrPrpsDtlItem> acntOrdCntrPrpsDtl; // 계좌별주문체결내역상세

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
    public static class AcntOrdCntrPrpsDtlItem {

        @JsonProperty("ord_no")
        private String ordNo; // 주문번호

        @JsonProperty("stk_cd")
        private String stkCd; // 종목번호

        @JsonProperty("trde_tp")
        private String trdeTp; // 매매구분

        @JsonProperty("crd_tp")
        private String crdTp; // 신용구분

        @JsonProperty("ord_qty")
        private String ordQty; // 주문수량

        @JsonProperty("ord_uv")
        private String ordUv; // 주문단가

        @JsonProperty("cnfm_qty")
        private String cnfmQty; // 확인수량

        @JsonProperty("acpt_tp")
        private String acptTp; // 접수구분

        @JsonProperty("rsrv_tp")
        private String rsrvTp; // 반대여부

        @JsonProperty("ord_tm")
        private String ordTm; // 주문시간

        @JsonProperty("ori_ord")
        private String oriOrd; // 원주문

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("io_tp_nm")
        private String ioTpNm; // 주문구분

        @JsonProperty("loan_dt")
        private String loanDt; // 대출일

        @JsonProperty("cntr_qty")
        private String cntrQty; // 체결수량

        @JsonProperty("cntr_uv")
        private String cntrUv; // 체결단가

        @JsonProperty("ord_remnq")
        private String ordRemnq; // 주문잔량

        @JsonProperty("comm_ord_tp")
        private String commOrdTp; // 통신구분

        @JsonProperty("mdfy_cncl")
        private String mdfyCncl; // 정정취소

        @JsonProperty("cnfm_tm")
        private String cnfmTm; // 확인시간

        @JsonProperty("dmst_stex_tp")
        private String dmstStexTp; // 국내거래소구분

        @JsonProperty("cond_uv")
        private String condUv; // 스톱가
    }
}
