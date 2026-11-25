package com.example.stockanalysis.kiwoom.contract.tr.ka10003;

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
 * 체결정보요청(ka10003) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10003Response implements KiwoomResult {

    @JsonProperty("cntr_infr")
    private List<CntrInfrItem> cntrInfr; // 체결정보

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
    public static class CntrInfrItem {

        @JsonProperty("tm")
        private String tm; // 시간

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("pre_rt")
        private String preRt; // 대비율

        @JsonProperty("pri_sel_bid_unit")
        private String priSelBidUnit; // 우선매도호가단위

        @JsonProperty("pri_buy_bid_unit")
        private String priBuyBidUnit; // 우선매수호가단위

        @JsonProperty("cntr_trde_qty")
        private String cntrTrdeQty; // 체결거래량

        @JsonProperty("sign")
        private String sign; // sign

        @JsonProperty("acc_trde_qty")
        private String accTrdeQty; // 누적거래량

        @JsonProperty("acc_trde_prica")
        private String accTrdePrica; // 누적거래대금

        @JsonProperty("cntr_str")
        private String cntrStr; // 체결강도

        @JsonProperty("stex_tp")
        private String stexTp; // 거래소구분
    }
}
