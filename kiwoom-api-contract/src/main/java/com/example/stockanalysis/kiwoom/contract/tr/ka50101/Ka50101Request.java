package com.example.stockanalysis.kiwoom.contract.tr.ka50101;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 금현물 호가(ka50101) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka50101Request {

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("tic_scope")
    private String ticScope; // 틱범위

}
