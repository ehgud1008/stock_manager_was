package com.example.stockanalysis.kiwoom.contract.tr.usa20940;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 누적 등락률 상위(주식/업종)(usa20940) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa20940Request {

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소 구분

    @JsonProperty("inds_cd")
    private String indsCd; // 업종코드

    @JsonProperty("stk_tp")
    private String stkTp; // 종목구분

    @JsonProperty("sort_tp")
    private String sortTp; // 정렬기준구분

    @JsonProperty("pric_cnd1")
    private String pricCnd1; // 가격1 구분

    @JsonProperty("pric_cnd2")
    private String pricCnd2; // 가격2 구분

    @JsonProperty("base_dt")
    private String baseDt; // 기준일자

    @JsonProperty("stk_cnd")
    private String stkCnd; // 종목조건

    @JsonProperty("trde_qty_tp")
    private String trdeQtyTp; // 거래량조건

    @JsonProperty("pric_cnd")
    private String pricCnd; // 검색가격대 구분

    @JsonProperty("trde_prica_cnd")
    private String trdePricaCnd; // 거래대금조건

}
