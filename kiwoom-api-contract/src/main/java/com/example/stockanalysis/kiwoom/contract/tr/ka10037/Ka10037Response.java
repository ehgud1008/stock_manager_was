package com.example.stockanalysis.kiwoom.contract.tr.ka10037;

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
 * 외국계창구매매상위요청(ka10037) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10037Response implements KiwoomResult {

    @JsonProperty("frgn_wicket_trde_upper")
    private List<FrgnWicketTrdeUpperItem> frgnWicketTrdeUpper; // 외국계창구매매상위

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
    public static class FrgnWicketTrdeUpperItem {

        @JsonProperty("rank")
        private String rank; // 순위

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
        private String fluRt; // 등락율

        @JsonProperty("sel_trde_qty")
        private String selTrdeQty; // 매도거래량

        @JsonProperty("buy_trde_qty")
        private String buyTrdeQty; // 매수거래량

        @JsonProperty("netprps_trde_qty")
        private String netprpsTrdeQty; // 순매수거래량

        @JsonProperty("netprps_prica")
        private String netprpsPrica; // 순매수대금

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("trde_prica")
        private String trdePrica; // 거래대금
    }
}
