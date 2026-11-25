package com.example.stockanalysis.kiwoom.contract.tr.ka40002;

import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ETF종목정보요청(ka40002) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka40002Response implements KiwoomResult {

    @JsonProperty("stk_nm")
    private String stkNm; // 종목명

    @JsonProperty("etfobjt_idex_nm")
    private String etfobjtIdexNm; // ETF대상지수명

    @JsonProperty("wonju_pric")
    private String wonjuPric; // 원주가격

    @JsonProperty("etftxon_type")
    private String etftxonType; // ETF과세유형

    @JsonProperty("etntxon_type")
    private String etntxonType; // ETN과세유형

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
