package com.example.stockanalysis.kiwoom.contract.tr.usa24110;

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
 * 미국주식 최고최저가대비 상승하락(주식/업종)(usa24110) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa24110Response implements KiwoomResult {

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

        @JsonProperty("flu_rt")
        private String fluRt; // 등락률

        @JsonProperty("oyr_hgst")
        private String oyrHgst; // 연중최고가

        @JsonProperty("oyr_lwst")
        private String oyrLwst; // 연중최저가

        @JsonProperty("52wk_hgst_pric")
        private String _52wkHgstPric; // 52주 최고가

        @JsonProperty("52wk_lwst_pric")
        private String _52wkLwstPric; // 52주 최저가

        @JsonProperty("hl_pre_sig")
        private String hlPreSig; // 최저가/최고가 대비기호

        @JsonProperty("hl_pre")
        private String hlPre; // 최저가/최고가대비

        @JsonProperty("pre_flu_rt")
        private String preFluRt; // 대비등락율

        @JsonProperty("base_dt")
        private String baseDt; // 최고/최저일시

        @JsonProperty("acc_trde_qty")
        private String accTrdeQty; // 누적거래량
    }
}
