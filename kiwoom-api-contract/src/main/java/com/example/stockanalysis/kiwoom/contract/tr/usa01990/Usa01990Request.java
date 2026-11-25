package com.example.stockanalysis.kiwoom.contract.tr.usa01990;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 관심종목 등록 상위(usa01990) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa01990Request {

    @JsonProperty("dt_unit_tp")
    private String dtUnitTp; // 일,주,월단위구분

    @JsonProperty("stk_tp")
    private String stkTp; // 시장구분

}
