package com.example.stockanalysis.kiwoom.contract.tr.ka20005;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 업종분봉조회요청(ka20005) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka20005Request {

    @JsonProperty("inds_cd")
    private String indsCd; // 업종코드

    @JsonProperty("tic_scope")
    private String ticScope; // 틱범위

    @JsonProperty("base_dt")
    private String baseDt; // 기준일자

}
