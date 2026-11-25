package com.example.stockanalysis.kiwoom.contract.tr.usa20880;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 키움 거래 상위 종목(미국주식)(usa20880) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa20880Request {

    @JsonProperty("qry_tp")
    private String qryTp; // 조회구분

    @JsonProperty("dt_unit_tp")
    private String dtUnitTp; // 일,주,월단위구분

}
