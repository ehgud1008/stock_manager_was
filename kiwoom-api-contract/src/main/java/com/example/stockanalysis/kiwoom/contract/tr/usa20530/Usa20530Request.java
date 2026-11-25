package com.example.stockanalysis.kiwoom.contract.tr.usa20530;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 당일 거래량 상위(주식/업종)(usa20530) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa20530Request {

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소 구분

    @JsonProperty("inds_cd")
    private String indsCd; // 업종코드

    @JsonProperty("stk_tp")
    private String stkTp; // 종목 구분

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
