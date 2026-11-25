package com.example.stockanalysis.kiwoom.contract.tr.ka10029;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 예상체결등락률상위요청(ka10029) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10029Request {

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 시장구분

    @JsonProperty("sort_tp")
    private String sortTp; // 정렬구분

    @JsonProperty("trde_qty_cnd")
    private String trdeQtyCnd; // 거래량조건

    @JsonProperty("stk_cnd")
    private String stkCnd; // 종목조건

    @JsonProperty("crd_cnd")
    private String crdCnd; // 신용조건

    @JsonProperty("pric_cnd")
    private String pricCnd; // 가격조건

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
