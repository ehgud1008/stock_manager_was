package com.example.stockanalysis.kiwoom.contract.tr.ka20001;

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
 * 업종현재가요청(ka20001) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka20001Response implements KiwoomResult {

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

    @JsonProperty("trde_prica")
    private String trdePrica; // 거래대금

    @JsonProperty("trde_frmatn_stk_num")
    private String trdeFrmatnStkNum; // 거래형성종목수

    @JsonProperty("trde_frmatn_rt")
    private String trdeFrmatnRt; // 거래형성비율

    @JsonProperty("open_pric")
    private String openPric; // 시가

    @JsonProperty("high_pric")
    private String highPric; // 고가

    @JsonProperty("low_pric")
    private String lowPric; // 저가

    @JsonProperty("upl")
    private String upl; // 상한

    @JsonProperty("rising")
    private String rising; // 상승

    @JsonProperty("stdns")
    private String stdns; // 보합

    @JsonProperty("fall")
    private String fall; // 하락

    @JsonProperty("lst")
    private String lst; // 하한

    @JsonProperty("52wk_hgst_pric")
    private String _52wkHgstPric; // 52주최고가

    @JsonProperty("52wk_hgst_pric_dt")
    private String _52wkHgstPricDt; // 52주최고가일

    @JsonProperty("52wk_hgst_pric_pre_rt")
    private String _52wkHgstPricPreRt; // 52주최고가대비율

    @JsonProperty("52wk_lwst_pric")
    private String _52wkLwstPric; // 52주최저가

    @JsonProperty("52wk_lwst_pric_dt")
    private String _52wkLwstPricDt; // 52주최저가일

    @JsonProperty("52wk_lwst_pric_pre_rt")
    private String _52wkLwstPricPreRt; // 52주최저가대비율

    @JsonProperty("inds_cur_prc_tm")
    private List<IndsCurPrcTmItem> indsCurPrcTm; // 업종현재가_시간별

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
    public static class IndsCurPrcTmItem {

        @JsonProperty("tm_n")
        private String tmN; // 시간n

        @JsonProperty("cur_prc_n")
        private String curPrcN; // 현재가n

        @JsonProperty("pred_pre_sig_n")
        private String predPreSigN; // 전일대비기호n

        @JsonProperty("pred_pre_n")
        private String predPreN; // 전일대비n

        @JsonProperty("flu_rt_n")
        private String fluRtN; // 등락률n

        @JsonProperty("trde_qty_n")
        private String trdeQtyN; // 거래량n

        @JsonProperty("acc_trde_qty_n")
        private String accTrdeQtyN; // 누적거래량n
    }
}
