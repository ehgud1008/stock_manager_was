package com.example.stockanalysis.kiwoom.contract.tr.ka10043;

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
 * 거래원매물대분석요청(ka10043) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10043Response implements KiwoomResult {

    @JsonProperty("trde_ori_prps_anly")
    private List<TrdeOriPrpsAnlyItem> trdeOriPrpsAnly; // 거래원매물대분석

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
    public static class TrdeOriPrpsAnlyItem {

        @JsonProperty("dt")
        private String dt; // 일자

        @JsonProperty("close_pric")
        private String closePric; // 종가

        @JsonProperty("pre_sig")
        private String preSig; // 대비기호

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("sel_qty")
        private String selQty; // 매도량

        @JsonProperty("buy_qty")
        private String buyQty; // 매수량

        @JsonProperty("netprps_qty")
        private String netprpsQty; // 순매수수량

        @JsonProperty("trde_qty_sum")
        private String trdeQtySum; // 거래량합

        @JsonProperty("trde_wght")
        private String trdeWght; // 거래비중
    }
}
