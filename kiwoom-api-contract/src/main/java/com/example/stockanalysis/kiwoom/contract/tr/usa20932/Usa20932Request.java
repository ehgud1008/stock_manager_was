package com.example.stockanalysis.kiwoom.contract.tr.usa20932;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 가격급등락(관심종목)(usa20932) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa20932Request {

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소 구분

    @JsonProperty("stk_cd")
    private List<Map<String, Object>> stkCd; // 관심종목

    @JsonProperty("flu_tp")
    private String fluTp; // 정렬 구분

    @JsonProperty("tm_tp")
    private String tmTp; // 기준 구분

    @JsonProperty("tm")
    private String tm; // 분전 값

    @JsonProperty("stk_cnd")
    private String stkCnd; // 종목조건

    @JsonProperty("pric_cnd")
    private String pricCnd; // 가격조건

    @JsonProperty("trde_qty_tp")
    private String trdeQtyTp; // 거래량조건

    @JsonProperty("trde_prica_cnd")
    private String trdePricaCnd; // 거래대금조건

}
