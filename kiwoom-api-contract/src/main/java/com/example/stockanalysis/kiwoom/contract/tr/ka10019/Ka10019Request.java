package com.example.stockanalysis.kiwoom.contract.tr.ka10019;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 가격급등락요청(ka10019) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10019Request {

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 시장구분

    @JsonProperty("flu_tp")
    private String fluTp; // 등락구분

    @JsonProperty("tm_tp")
    private String tmTp; // 시간구분

    @JsonProperty("tm")
    private String tm; // 시간

    @JsonProperty("trde_qty_tp")
    private String trdeQtyTp; // 거래량구분

    @JsonProperty("stk_cnd")
    private String stkCnd; // 종목조건

    @JsonProperty("crd_cnd")
    private String crdCnd; // 신용조건

    @JsonProperty("pric_cnd")
    private String pricCnd; // 가격조건

    @JsonProperty("updown_incls")
    private String updownIncls; // 상하한포함

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
