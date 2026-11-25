package com.example.stockanalysis.kiwoom.contract.tr.ka10018;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 고저가근접요청(ka10018) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10018Request {

    @JsonProperty("high_low_tp")
    private String highLowTp; // 고저구분

    @JsonProperty("alacc_rt")
    private String alaccRt; // 근접율

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 시장구분

    @JsonProperty("trde_qty_tp")
    private String trdeQtyTp; // 거래량구분

    @JsonProperty("stk_cnd")
    private String stkCnd; // 종목조건

    @JsonProperty("crd_cnd")
    private String crdCnd; // 신용조건

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
