package com.example.stockanalysis.kiwoom.contract.tr.ka40001;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ETF수익율요청(ka40001) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka40001Request {

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("etfobjt_idex_cd")
    private String etfobjtIdexCd; // ETF대상지수코드

    @JsonProperty("dt")
    private String dt; // 기간

}
