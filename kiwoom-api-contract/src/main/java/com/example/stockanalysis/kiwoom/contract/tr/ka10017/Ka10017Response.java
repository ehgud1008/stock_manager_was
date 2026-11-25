package com.example.stockanalysis.kiwoom.contract.tr.ka10017;

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
 * 상하한가요청(ka10017) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10017Response implements KiwoomResult {

    @JsonProperty("updown_pric")
    private List<UpdownPricItem> updownPric; // 상하한가

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
    public static class UpdownPricItem {

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_infr")
        private String stkInfr; // 종목정보

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

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("pred_trde_qty")
        private String predTrdeQty; // 전일거래량

        @JsonProperty("sel_req")
        private String selReq; // 매도잔량

        @JsonProperty("sel_bid")
        private String selBid; // 매도호가

        @JsonProperty("buy_bid")
        private String buyBid; // 매수호가

        @JsonProperty("buy_req")
        private String buyReq; // 매수잔량

        @JsonProperty("cnt")
        private String cnt; // 횟수
    }
}
