package com.example.stockanalysis.kiwoom.contract.tr.usa20970;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 고가/저가 접근(주식/업종)(usa20970) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa20970Request {

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

    @JsonProperty("inds_cd")
    private String indsCd; // 업종코드

    @JsonProperty("stk_tp")
    private String stkTp; // 종목구분

    @JsonProperty("high_low_tp")
    private String highLowTp; // 고가,저가구분

    @JsonProperty("alacc_rt")
    private String alaccRt; // 근접률

    @JsonProperty("stk_cnd")
    private String stkCnd; // 종목조건

    @JsonProperty("pric_cnd_st")
    private String pricCndSt; // 가격조건 시작

    @JsonProperty("pric_cnd_ed")
    private String pricCndEd; // 가격조건 끝

    @JsonProperty("trde_pric_cnd_st")
    private String trdePricCndSt; // 거래대금조건

    @JsonProperty("trde_qty_cnd_fr")
    private String trdeQtyCndFr; // 거래량조건

}
