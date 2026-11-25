package com.example.stockanalysis.kiwoom.contract.tr.ka10027;

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
 * 전일대비등락률상위요청(ka10027) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10027Response implements KiwoomResult {

    @JsonProperty("pred_pre_flu_rt_upper")
    private List<PredPreFluRtUpperItem> predPreFluRtUpper; // 전일대비등락률상위

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
    public static class PredPreFluRtUpperItem {

        @JsonProperty("stk_cls")
        private String stkCls; // 종목분류

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

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

        @JsonProperty("sel_req")
        private String selReq; // 매도잔량

        @JsonProperty("buy_req")
        private String buyReq; // 매수잔량

        @JsonProperty("now_trde_qty")
        private String nowTrdeQty; // 현재거래량

        @JsonProperty("cntr_str")
        private String cntrStr; // 체결강도

        @JsonProperty("cnt")
        private String cnt; // 횟수
    }
}
