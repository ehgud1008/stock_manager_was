package com.example.stockanalysis.kiwoom.contract.tr.ka10065;

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
 * 장중투자자별매매상위요청(ka10065) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10065Response implements KiwoomResult {

    @JsonProperty("opmr_invsr_trde_upper")
    private List<OpmrInvsrTrdeUpperItem> opmrInvsrTrdeUpper; // 장중투자자별매매상위

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
    public static class OpmrInvsrTrdeUpperItem {

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("sel_qty")
        private String selQty; // 매도금액/매도량

        @JsonProperty("buy_qty")
        private String buyQty; // 매수금액/매수량

        @JsonProperty("netslmt")
        private String netslmt; // 순매수/순매도
    }
}
