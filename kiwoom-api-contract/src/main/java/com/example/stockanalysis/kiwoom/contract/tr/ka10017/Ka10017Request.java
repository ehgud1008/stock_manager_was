package com.example.stockanalysis.kiwoom.contract.tr.ka10017;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 상하한가요청(ka10017) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10017Request {

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 시장구분

    @JsonProperty("updown_tp")
    private String updownTp; // 상하한구분

    @JsonProperty("sort_tp")
    private String sortTp; // 정렬구분

    @JsonProperty("stk_cnd")
    private String stkCnd; // 종목조건

    @JsonProperty("trde_qty_tp")
    private String trdeQtyTp; // 거래량구분

    @JsonProperty("crd_cnd")
    private String crdCnd; // 신용조건

    @JsonProperty("trde_gold_tp")
    private String trdeGoldTp; // 매매금구분

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
