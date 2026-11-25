package com.example.stockanalysis.kiwoom.contract.tr.usa26411;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 연도별 업종별 종목등락률(usa26411) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa26411Request {

    @JsonProperty("inds_cd")
    private String indsCd; // 업종코드

    @JsonProperty("srch_yr")
    private String srchYr; // 조회연도

}
