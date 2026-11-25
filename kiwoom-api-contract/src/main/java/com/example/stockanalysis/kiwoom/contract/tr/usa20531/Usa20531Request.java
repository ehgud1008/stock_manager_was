package com.example.stockanalysis.kiwoom.contract.tr.usa20531;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 당일 거래량 상위(ETF)(usa20531) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa20531Request {

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소 구분

    @JsonProperty("etf_cat1")
    private String etfCat1; // ETF카테고리코드1

    @JsonProperty("etf_cat2")
    private String etfCat2; // ETF카테고리코드2

    @JsonProperty("trde_qty_tp")
    private String trdeQtyTp; // 거래량조건

    @JsonProperty("qry_tp")
    private String qryTp; // 정렬기준 구분

    @JsonProperty("stk_cnd")
    private String stkCnd; // 종목필터 구분

    @JsonProperty("pric_cnd")
    private String pricCnd; // 검색가격대 구분

    @JsonProperty("trde_prica_cnd")
    private String trdePricaCnd; // 거래대금조건

}
