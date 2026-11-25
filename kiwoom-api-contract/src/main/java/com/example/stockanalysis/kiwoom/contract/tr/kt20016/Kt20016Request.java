package com.example.stockanalysis.kiwoom.contract.tr.kt20016;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 신용융자 가능종목요청(kt20016) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt20016Request {

    @JsonProperty("crd_stk_grde_tp")
    private String crdStkGrdeTp; // 신용종목등급구분

    @JsonProperty("mrkt_deal_tp")
    private String mrktDealTp; // 시장거래구분

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

}
