package com.example.stockanalysis.kiwoom.contract.tr.ust21150;

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
 * 미국주식 일별 주문체결내역(ust21150) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21150Response implements KiwoomResult {

    @JsonProperty("result_list")
    @JsonAlias({"result_lsit"})
    private List<ResultListItem> resultList; // 결과리스트

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

        @JsonProperty("crnc_code")
        private String crncCode; // 통화코드

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("isin_code")
        private String isinCode; // 국제표준코드

        @JsonProperty("frgn_trde_tp")
        private String frgnTrdeTp; // 매매구분

        @JsonProperty("ord_qty")
        private String ordQty; // 주문수량

        @JsonProperty("cntr_qty")
        private String cntrQty; // 체결수량

        @JsonProperty("mdfy_qty")
        private String mdfyQty; // 정정수량

        @JsonProperty("cncl_qty")
        private String cnclQty; // 취소수량

        @JsonProperty("frgn_msg_code")
        private String frgnMsgCode; // 해외메세지코드

        @JsonProperty("rsrv_tp")
        private String rsrvTp; // 예약구분

        @JsonProperty("oppo_trde_tp_nm")
        private String oppoTrdeTpNm; // 반대매매구분명

        @JsonProperty("comm_ord_tp_nm")
        private String commOrdTpNm; // 통신주문구분명

        @JsonProperty("ord_time")
        private String ordTime; // 주문시간

        @JsonProperty("crnc_nm")
        private String crncNm; // 통화코드명

        @JsonProperty("stex_nm")
        private String stexNm; // 거래소코드명

        @JsonProperty("frgn_stk_nm")
        private String frgnStkNm; // 종목명

        @JsonProperty("slby_tp_nm")
        private String slbyTpNm; // 매도매수구분명

        @JsonProperty("ord_uv")
        private String ordUv; // 주문단가

        @JsonProperty("stop_pric")
        private String stopPric; // STOP가격

        @JsonProperty("cntr_uv")
        private String cntrUv; // 체결단가

        @JsonProperty("mdfy_uv")
        private String mdfyUv; // 정정단가

        @JsonProperty("ord_remnq")
        private String ordRemnq; // 주문잔량

        @JsonProperty("ord_stat_nm")
        private String ordStatNm; // 주문상태명

        @JsonProperty("text1")
        private String text1; // 거부사유

        @JsonProperty("inpt_chnl_tp")
        private String inptChnlTp; // 입력매체구분명

        @JsonProperty("ord_resp_time")
        private String ordRespTime; // 주문응답수신시간

        @JsonProperty("cntr_time")
        private String cntrTime; // 체결시간
    }
}
