package com.example.stockanalysis.kiwoom.contract.tr.ka90005;

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
 * 프로그램매매추이요청 시간대별(ka90005) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka90005Response implements KiwoomResult {

    @JsonProperty("prm_trde_trnsn")
    private List<PrmTrdeTrnsnItem> prmTrdeTrnsn; // 프로그램매매추이

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
    public static class PrmTrdeTrnsnItem {

        @JsonProperty("cntr_tm")
        private String cntrTm; // 체결시간

        @JsonProperty("dfrt_trde_sel")
        private String dfrtTrdeSel; // 차익거래매도

        @JsonProperty("dfrt_trde_buy")
        private String dfrtTrdeBuy; // 차익거래매수

        @JsonProperty("dfrt_trde_netprps")
        private String dfrtTrdeNetprps; // 차익거래순매수

        @JsonProperty("ndiffpro_trde_sel")
        private String ndiffproTrdeSel; // 비차익거래매도

        @JsonProperty("ndiffpro_trde_buy")
        private String ndiffproTrdeBuy; // 비차익거래매수

        @JsonProperty("ndiffpro_trde_netprps")
        private String ndiffproTrdeNetprps; // 비차익거래순매수

        @JsonProperty("dfrt_trde_sell_qty")
        private String dfrtTrdeSellQty; // 차익거래매도수량

        @JsonProperty("dfrt_trde_buy_qty")
        private String dfrtTrdeBuyQty; // 차익거래매수수량

        @JsonProperty("dfrt_trde_netprps_qty")
        private String dfrtTrdeNetprpsQty; // 차익거래순매수수량

        @JsonProperty("ndiffpro_trde_sell_qty")
        private String ndiffproTrdeSellQty; // 비차익거래매도수량

        @JsonProperty("ndiffpro_trde_buy_qty")
        private String ndiffproTrdeBuyQty; // 비차익거래매수수량

        @JsonProperty("ndiffpro_trde_netprps_qty")
        private String ndiffproTrdeNetprpsQty; // 비차익거래순매수수량

        @JsonProperty("all_sel")
        private String allSel; // 전체매도

        @JsonProperty("all_buy")
        private String allBuy; // 전체매수

        @JsonProperty("all_netprps")
        private String allNetprps; // 전체순매수

        @JsonProperty("kospi200")
        private String kospi200; // KOSPI200

        @JsonProperty("basis")
        private String basis; // BASIS
    }
}
