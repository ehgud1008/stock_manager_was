package com.example.stockanalysis.kiwoom.contract.tr.ka40004;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ETF전체시세요청(ka40004) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka40004Request {

    @JsonProperty("txon_type")
    private String txonType; // 과세유형

    @JsonProperty("navpre")
    private String navpre; // NAV대비

    @JsonProperty("mngmcomp")
    private String mngmcomp; // 운용사

    @JsonProperty("txon_yn")
    private String txonYn; // 과세여부

    @JsonProperty("trace_idex")
    private String traceIdex; // 추적지수

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
