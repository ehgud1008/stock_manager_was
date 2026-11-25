package com.example.stockanalysis.kiwoom.contract.tr.usa20910;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 전일대비 등락률상위(주식/업종)(usa20910) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa20910Request {

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

    @JsonProperty("inds_cd")
    private String indsCd; // 업종코드

    @JsonProperty("inds_cls_tp")
    private String indsClsTp; // 미국업종구분

    @JsonProperty("sort_tp")
    private String sortTp; // 정렬기준

    @JsonProperty("stk_tp")
    private String stkTp; // 종목구분

    @JsonProperty("stk_cnd")
    private String stkCnd; // 종목조건

    @JsonProperty("pric_cnd")
    private String pricCnd; // 가격조건

    @JsonProperty("trde_prica_cnd")
    private String trdePricaCnd; // 거래대금조건

    @JsonProperty("trde_qty_tp")
    private String trdeQtyTp; // 거래량조건

}
