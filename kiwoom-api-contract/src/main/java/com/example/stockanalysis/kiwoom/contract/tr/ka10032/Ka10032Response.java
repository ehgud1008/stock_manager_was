package com.example.stockanalysis.kiwoom.contract.tr.ka10032;

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
 * 거래대금상위요청(ka10032) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10032Response implements KiwoomResult {

    @JsonProperty("trde_prica_upper")
    private List<TrdePricaUpperItem> trdePricaUpper; // 거래대금상위

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
    public static class TrdePricaUpperItem {

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("now_rank")
        private String nowRank; // 현재순위

        @JsonProperty("pred_rank")
        private String predRank; // 전일순위

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("pred_pre_sig")
        private String predPreSig; // 전일대비기호

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("flu_rt")
        private String fluRt; // 등락률

        @JsonProperty("sel_bid")
        private String selBid; // 매도호가

        @JsonProperty("buy_bid")
        private String buyBid; // 매수호가

        @JsonProperty("now_trde_qty")
        private String nowTrdeQty; // 현재거래량

        @JsonProperty("pred_trde_qty")
        private String predTrdeQty; // 전일거래량

        @JsonProperty("trde_prica")
        private String trdePrica; // 거래대금
    }
}
