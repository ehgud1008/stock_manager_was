package com.example.stockanalysis.kiwoom.contract.tr.ka50091;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 금현물당일틱차트조회요청(ka50091) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka50091Request {

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("tic_scope")
    private String ticScope; // 틱범위

}
