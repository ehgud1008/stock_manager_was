package com.example.stockanalysis.kiwoom.contract.tr.ka30002;

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
 * 거래원별ELW순매매상위요청(ka30002) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka30002Response implements KiwoomResult {

    @JsonProperty("trde_ori_elwnettrde_upper")
    private List<TrdeOriElwnettrdeUpperItem> trdeOriElwnettrdeUpper; // 거래원별ELW순매매상위

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
    public static class TrdeOriElwnettrdeUpperItem {

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("stkpc_flu")
        private String stkpcFlu; // 주가등락

        @JsonProperty("flu_rt")
        private String fluRt; // 등락율

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("netprps")
        private String netprps; // 순매수

        @JsonProperty("buy_trde_qty")
        private String buyTrdeQty; // 매수거래량

        @JsonProperty("sel_trde_qty")
        private String selTrdeQty; // 매도거래량
    }
}
