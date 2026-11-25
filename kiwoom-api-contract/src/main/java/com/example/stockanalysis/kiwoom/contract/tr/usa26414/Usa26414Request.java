package com.example.stockanalysis.kiwoom.contract.tr.usa26414;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 연도별 등락률(ETF)(usa26414) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa26414Request {

    @JsonProperty("etf_cat1")
    private String etfCat1; // ETF카테고리코드1

    @JsonProperty("etf_cat2")
    private String etfCat2; // ETF카테고리코드2

}
