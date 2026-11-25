package com.example.stockanalysis.kiwoom.contract.tr.ka10008;

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
 * 주식외국인종목별매매동향(ka10008) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10008Response implements KiwoomResult {

    @JsonProperty("stk_frgnr")
    private List<StkFrgnrItem> stkFrgnr; // 주식외국인

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
    public static class StkFrgnrItem {

        @JsonProperty("dt")
        private String dt; // 일자

        @JsonProperty("close_pric")
        private String closePric; // 종가

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("chg_qty")
        private String chgQty; // 변동수량

        @JsonProperty("poss_stkcnt")
        private String possStkcnt; // 보유주식수

        @JsonProperty("wght")
        private String wght; // 비중

        @JsonProperty("gain_pos_stkcnt")
        private String gainPosStkcnt; // 취득가능주식수

        @JsonProperty("frgnr_limit")
        private String frgnrLimit; // 외국인한도

        @JsonProperty("frgnr_limit_irds")
        private String frgnrLimitIrds; // 외국인한도증감

        @JsonProperty("limit_exh_rt")
        private String limitExhRt; // 한도소진률
    }
}
