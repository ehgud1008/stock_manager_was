package com.example.stockanalysis.kiwoom.contract.tr.ka90002;

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
 * 테마구성종목요청(ka90002) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka90002Response implements KiwoomResult {

    @JsonProperty("flu_rt")
    private String fluRt; // 등락률

    @JsonProperty("dt_prft_rt")
    private String dtPrftRt; // 기간수익률

    @JsonProperty("thema_comp_stk")
    private List<ThemaCompStkItem> themaCompStk; // 테마구성종목

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
    public static class ThemaCompStkItem {

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("flu_sig")
        private String fluSig; // 등락기호

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("flu_rt")
        private String fluRt; // 등락율

        @JsonProperty("acc_trde_qty")
        private String accTrdeQty; // 누적거래량

        @JsonProperty("sel_bid")
        private String selBid; // 매도호가

        @JsonProperty("sel_req")
        private String selReq; // 매도잔량

        @JsonProperty("buy_bid")
        private String buyBid; // 매수호가

        @JsonProperty("buy_req")
        private String buyReq; // 매수잔량

        @JsonProperty("dt_prft_rt_n")
        private String dtPrftRtN; // 기간수익률n
    }
}
