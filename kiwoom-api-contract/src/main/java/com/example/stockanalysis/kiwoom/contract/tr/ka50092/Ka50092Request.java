package com.example.stockanalysis.kiwoom.contract.tr.ka50092;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 금현물당일분봉차트조회요청(ka50092) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka50092Request {

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("tic_scope")
    private String ticScope; // 틱범위

}
