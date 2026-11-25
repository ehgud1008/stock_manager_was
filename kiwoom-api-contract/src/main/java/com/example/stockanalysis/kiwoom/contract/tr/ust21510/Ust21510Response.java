package com.example.stockanalysis.kiwoom.contract.tr.ust21510;

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
 * 미국주식 당일 주문체결 확인(ust21510) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21510Response implements KiwoomResult {

    @JsonProperty("result_list")
    @JsonAlias({"result_lsit"})
    private List<ResultListItem> resultList; // 조회결과리스트

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
    public static class ResultListItem {

        @JsonProperty("ord_no")
        private String ordNo; // 주문번호

        @JsonProperty("orig_ord_no")
        private String origOrdNo; // 원주문번호

        @JsonProperty("stex_nm")
        private String stexNm; // 거래소명

        @JsonProperty("crnc_code")
        private String crncCode; // 통화코드

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("frgn_stk_nm")
        private String frgnStkNm; // 종목명

        @JsonProperty("frgn_trde_tp")
        private String frgnTrdeTp; // 매매구분

        @JsonProperty("frgn_trde_nm")
        private String frgnTrdeNm; // 매매구분명

        @JsonProperty("slby_tp")
        private String slbyTp; // 매도매수구분

        @JsonProperty("slby_tp_nm")
        private String slbyTpNm; // 매도매수구분명

        @JsonProperty("ord_qty")
        private String ordQty; // 주문수량

        @JsonProperty("ord_uv")
        private String ordUv; // 주문단가

        @JsonProperty("stop_pric")
        private String stopPric; // STOP가격

        @JsonProperty("cntr_qty")
        private String cntrQty; // 체결수량

        @JsonProperty("cntr_uv")
        private String cntrUv; // 체결단가

        @JsonProperty("cnfm_qty")
        private String cnfmQty; // 확인수량

        @JsonProperty("ord_remnq")
        private String ordRemnq; // 주문잔량

        @JsonProperty("ord_time")
        private String ordTime; // 주문시간

        @JsonProperty("ord_resp_time")
        private String ordRespTime; // 주문응답시간

        @JsonProperty("ord_stat")
        private String ordStat; // 주문상태명

        @JsonProperty("rsrv_tp")
        private String rsrvTp; // 예약주문구분

        @JsonProperty("natn_nm")
        private String natnNm; // 국가명

        @JsonProperty("cntr_time")
        private String cntrTime; // 체결시간
    }
}
