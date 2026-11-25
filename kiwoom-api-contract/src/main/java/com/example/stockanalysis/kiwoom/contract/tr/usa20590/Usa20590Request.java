package com.example.stockanalysis.kiwoom.contract.tr.usa20590;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 일별주가(usa20590) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa20590Request {

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("base_dt")
    private String baseDt; // 기준일자

}
