package com.example.stockanalysis.kiwoom.contract.tr.usa20570;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 가격대별주가(주식/업종)(usa20570) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa20570Request {

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

    @JsonProperty("stk_tp")
    private String stkTp; // 종목구분

    @JsonProperty("stk_cnd")
    private String stkCnd; // 종목조건

    @JsonProperty("inds_cd")
    private String indsCd; // 업종코드

    @JsonProperty("trde_qty_tp")
    private String trdeQtyTp; // 거래량조건

    @JsonProperty("pric_cnd1")
    private String pricCnd1; // 가격조건1

    @JsonProperty("pric_cnd2")
    private String pricCnd2; // 가격조건2

    @JsonProperty("trde_prica_cnd")
    private String trdePricaCnd; // 거래대금조건

}
