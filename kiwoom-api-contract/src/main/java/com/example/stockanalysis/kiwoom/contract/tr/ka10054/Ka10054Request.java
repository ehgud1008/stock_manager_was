package com.example.stockanalysis.kiwoom.contract.tr.ka10054;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 변동성완화장치발동종목요청(ka10054) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10054Request {

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 시장구분

    @JsonProperty("bf_mkrt_tp")
    private String bfMkrtTp; // 장전구분

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("motn_tp")
    private String motnTp; // 발동구분

    @JsonProperty("skip_stk")
    private String skipStk; // 제외종목

    @JsonProperty("trde_qty_tp")
    private String trdeQtyTp; // 거래량구분

    @JsonProperty("min_trde_qty")
    private String minTrdeQty; // 최소거래량

    @JsonProperty("max_trde_qty")
    private String maxTrdeQty; // 최대거래량

    @JsonProperty("trde_prica_tp")
    private String trdePricaTp; // 거래대금구분

    @JsonProperty("min_trde_prica")
    private String minTrdePrica; // 최소거래대금

    @JsonProperty("max_trde_prica")
    private String maxTrdePrica; // 최대거래대금

    @JsonProperty("motn_drc")
    private String motnDrc; // 발동방향

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
