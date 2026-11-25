package com.example.stockanalysis.kiwoom.contract.tr.usa24100;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 신고가/신저가(주식/업종)(usa24100) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa24100Request {

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

    @JsonProperty("stk_tp")
    private String stkTp; // 종목구분

    @JsonProperty("inds_cd")
    private String indsCd; // 업종코드

    @JsonProperty("stk_cnd")
    private String stkCnd; // 종목조건

    @JsonProperty("ntl_tp")
    private String ntlTp; // 신고가신저가구분

    @JsonProperty("high_low_tp")
    private String highLowTp; // 고저기준

    @JsonProperty("dt")
    private String dt; // 기간입력

    @JsonProperty("pric_cnd")
    private String pricCnd; // 가격조건

    @JsonProperty("trde_qty_tp")
    private String trdeQtyTp; // 거래량조건

    @JsonProperty("trde_prica_cnd")
    private String trdePricaCnd; // 거래대금조건

}
