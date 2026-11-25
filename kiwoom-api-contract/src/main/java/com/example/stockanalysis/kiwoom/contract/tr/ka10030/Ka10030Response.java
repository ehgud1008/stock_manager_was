package com.example.stockanalysis.kiwoom.contract.tr.ka10030;

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
 * 당일거래량상위요청(ka10030) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10030Response implements KiwoomResult {

    @JsonProperty("tdy_trde_qty_upper")
    private List<TdyTrdeQtyUpperItem> tdyTrdeQtyUpper; // 당일거래량상위

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
    public static class TdyTrdeQtyUpperItem {

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

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("pred_rt")
        private String predRt; // 전일비

        @JsonProperty("trde_tern_rt")
        private String trdeTernRt; // 거래회전율

        @JsonProperty("trde_amt")
        private String trdeAmt; // 거래금액

        @JsonProperty("opmr_trde_qty")
        private String opmrTrdeQty; // 장중거래량

        @JsonProperty("opmr_pred_rt")
        private String opmrPredRt; // 장중전일비

        @JsonProperty("opmr_trde_rt")
        private String opmrTrdeRt; // 장중거래회전율

        @JsonProperty("opmr_trde_amt")
        private String opmrTrdeAmt; // 장중거래금액

        @JsonProperty("af_mkrt_trde_qty")
        private String afMkrtTrdeQty; // 장후거래량

        @JsonProperty("af_mkrt_pred_rt")
        private String afMkrtPredRt; // 장후전일비

        @JsonProperty("af_mkrt_trde_rt")
        private String afMkrtTrdeRt; // 장후거래회전율

        @JsonProperty("af_mkrt_trde_amt")
        private String afMkrtTrdeAmt; // 장후거래금액

        @JsonProperty("bf_mkrt_trde_qty")
        private String bfMkrtTrdeQty; // 장전거래량

        @JsonProperty("bf_mkrt_pred_rt")
        private String bfMkrtPredRt; // 장전전일비

        @JsonProperty("bf_mkrt_trde_rt")
        private String bfMkrtTrdeRt; // 장전거래회전율

        @JsonProperty("bf_mkrt_trde_amt")
        private String bfMkrtTrdeAmt; // 장전거래금액
    }
}
