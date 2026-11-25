package com.example.stockanalysis.kiwoom.contract.tr.usa24210;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 잔량률급증(주식/업종)(usa24210) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa24210Request {

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소 구분

    @JsonProperty("inds_cd")
    private String indsCd; // 업종코드

    @JsonProperty("rt_tp")
    private String rtTp; // 비율 구분

    @JsonProperty("stk_tp")
    private String stkTp; // 종목 구분

    @JsonProperty("tm")
    private String tm; // xxx분전 설정

    @JsonProperty("stk_cnd")
    private String stkCnd; // 종목조건

    @JsonProperty("trde_qty_tp")
    private String trdeQtyTp; // 거래량조건

    @JsonProperty("pric_cnd")
    private String pricCnd; // 가격조건

    @JsonProperty("trde_prica_cnd")
    private String trdePricaCnd; // 거래대금조건

}
