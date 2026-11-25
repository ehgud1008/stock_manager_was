package com.example.stockanalysis.kiwoom.contract.tr.ka10022;

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
 * 잔량율급증요청(ka10022) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10022Response implements KiwoomResult {

    @JsonProperty("req_rt_sdnin")
    private List<ReqRtSdninItem> reqRtSdnin; // 잔량율급증

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
    public static class ReqRtSdninItem {

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

        @JsonProperty("int")
        private String _int; // 기준률

        @JsonProperty("now_rt")
        private String nowRt; // 현재비율

        @JsonProperty("sdnin_rt")
        private String sdninRt; // 급증률

        @JsonProperty("tot_sel_req")
        private String totSelReq; // 총매도잔량

        @JsonProperty("tot_buy_req")
        private String totBuyReq; // 총매수잔량
    }
}
