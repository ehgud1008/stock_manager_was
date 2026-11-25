package com.example.stockanalysis.kiwoom.contract.tr.ka10039;

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
 * 증권사별매매상위요청(ka10039) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10039Response implements KiwoomResult {

    @JsonProperty("sec_trde_upper")
    private List<SecTrdeUpperItem> secTrdeUpper; // 증권사별매매상위

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
    public static class SecTrdeUpperItem {

        @JsonProperty("rank")
        private String rank; // 순위

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("prid_stkpc_flu")
        private String pridStkpcFlu; // 기간중주가등락

        @JsonProperty("flu_rt")
        private String fluRt; // 등락율

        @JsonProperty("prid_trde_qty")
        private String pridTrdeQty; // 기간중거래량

        @JsonProperty("netprps")
        private String netprps; // 순매수

        @JsonProperty("buy_trde_qty")
        private String buyTrdeQty; // 매수거래량

        @JsonProperty("sel_trde_qty")
        private String selTrdeQty; // 매도거래량

        @JsonProperty("netprps_amt")
        private String netprpsAmt; // 순매수금액

        @JsonProperty("buy_amt")
        private String buyAmt; // 매수금액

        @JsonProperty("sell_amt")
        private String sellAmt; // 매도금액
    }
}
