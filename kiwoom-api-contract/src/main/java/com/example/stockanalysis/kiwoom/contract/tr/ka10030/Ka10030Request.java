package com.example.stockanalysis.kiwoom.contract.tr.ka10030;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 당일거래량상위요청(ka10030) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10030Request {

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 시장구분

    @JsonProperty("sort_tp")
    private String sortTp; // 정렬구분

    @JsonProperty("mang_stk_incls")
    private String mangStkIncls; // 관리종목포함

    @JsonProperty("crd_tp")
    private String crdTp; // 신용구분

    @JsonProperty("trde_qty_tp")
    private String trdeQtyTp; // 거래량구분

    @JsonProperty("pric_tp")
    private String pricTp; // 가격구분

    @JsonProperty("trde_prica_tp")
    private String trdePricaTp; // 거래대금구분

    @JsonProperty("mrkt_open_tp")
    private String mrktOpenTp; // 장운영구분

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
