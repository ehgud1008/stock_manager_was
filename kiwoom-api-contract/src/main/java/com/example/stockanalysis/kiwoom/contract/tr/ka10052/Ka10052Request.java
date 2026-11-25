package com.example.stockanalysis.kiwoom.contract.tr.ka10052;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 거래원순간거래량요청(ka10052) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10052Request {

    @JsonProperty("mmcm_cd")
    private String mmcmCd; // 회원사코드

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 시장구분

    @JsonProperty("qty_tp")
    private String qtyTp; // 수량구분

    @JsonProperty("pric_tp")
    private String pricTp; // 가격구분

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
