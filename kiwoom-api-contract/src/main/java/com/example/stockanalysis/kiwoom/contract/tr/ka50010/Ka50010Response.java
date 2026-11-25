package com.example.stockanalysis.kiwoom.contract.tr.ka50010;

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
 * 금현물체결추이(ka50010) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka50010Response implements KiwoomResult {

    @JsonProperty("gold_cntr")
    private List<GoldCntrItem> goldCntr; // 금현물체결추이

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
    public static class GoldCntrItem {

        @JsonProperty("cntr_pric")
        private String cntrPric; // 체결가

        @JsonProperty("pred_pre")
        private String predPre; // 전일 대비

        @JsonProperty("flu_rt")
        private String fluRt; // 등락율

        @JsonProperty("trde_qty")
        private String trdeQty; // 누적 거래량

        @JsonProperty("acc_trde_prica")
        private String accTrdePrica; // 누적 거래대금

        @JsonProperty("cntr_trde_qty")
        private String cntrTrdeQty; // 거래량(체결량)

        @JsonProperty("tm")
        private String tm; // 체결시간

        @JsonProperty("pre_sig")
        private String preSig; // 전일대비기호

        @JsonProperty("pri_sel_bid_unit")
        private String priSelBidUnit; // 매도호가

        @JsonProperty("pri_buy_bid_unit")
        private String priBuyBidUnit; // 매수호가

        @JsonProperty("trde_pre")
        private String trdePre; // 전일 거래량 대비 비율

        @JsonProperty("trde_tern_rt")
        private String trdeTernRt; // 전일 거래량 대비 순간 거래량 비율

        @JsonProperty("cntr_str")
        private String cntrStr; // 체결강도
    }
}
