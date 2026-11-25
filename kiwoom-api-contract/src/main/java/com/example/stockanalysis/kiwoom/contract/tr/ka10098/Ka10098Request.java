package com.example.stockanalysis.kiwoom.contract.tr.ka10098;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 시간외단일가등락율순위요청(ka10098) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10098Request {

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 시장구분

    @JsonProperty("sort_base")
    private String sortBase; // 정렬기준

    @JsonProperty("stk_cnd")
    private String stkCnd; // 종목조건

    @JsonProperty("trde_qty_cnd")
    private String trdeQtyCnd; // 거래량조건

    @JsonProperty("crd_cnd")
    private String crdCnd; // 신용조건

    @JsonProperty("trde_prica")
    private String trdePrica; // 거래대금

}
