package com.example.stockanalysis.kiwoom.contract.tr.usa24200;

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
 * 미국주식 호가잔량상위(주식/업종)(usa24200) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa24200Response implements KiwoomResult {

    @JsonProperty("result_list")
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

        @JsonProperty("rank")
        private String rank; // 순위

        @JsonProperty("mgn_type")
        private String mgnType; // 증거금률

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

        @JsonProperty("pred_pre_sig")
        private String predPreSig; // 전일대비기호

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("acc_trde_qty")
        private String accTrdeQty; // 거래량

        @JsonProperty("tot_sel_req")
        private String totSelReq; // 매도호가총잔량

        @JsonProperty("tot_buy_req")
        private String totBuyReq; // 매수호가총잔량

        @JsonProperty("buy_req")
        private String buyReq; // 순매수잔량

        @JsonProperty("buy_rt")
        private String buyRt; // 매수비율

        @JsonProperty("sel_req")
        private String selReq; // 순매도잔량

        @JsonProperty("sel_rt")
        private String selRt; // 매도비율

        @JsonProperty("cntr_tm")
        private String cntrTm; // 시간
    }
}
