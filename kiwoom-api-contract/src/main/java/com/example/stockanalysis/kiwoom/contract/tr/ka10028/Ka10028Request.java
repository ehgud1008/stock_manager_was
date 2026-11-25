package com.example.stockanalysis.kiwoom.contract.tr.ka10028;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 시가대비등락률요청(ka10028) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10028Request {

    @JsonProperty("sort_tp")
    private String sortTp; // 정렬구분

    @JsonProperty("trde_qty_cnd")
    private String trdeQtyCnd; // 거래량조건

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 시장구분

    @JsonProperty("updown_incls")
    private String updownIncls; // 상하한포함

    @JsonProperty("stk_cnd")
    private String stkCnd; // 종목조건

    @JsonProperty("crd_cnd")
    private String crdCnd; // 신용조건

    @JsonProperty("trde_prica_cnd")
    private String trdePricaCnd; // 거래대금조건

    @JsonProperty("flu_cnd")
    private String fluCnd; // 등락조건

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
