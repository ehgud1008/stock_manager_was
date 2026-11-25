package com.example.stockanalysis.kiwoom.contract.tr.ka20004;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 업종틱차트조회요청(ka20004) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka20004Request {

    @JsonProperty("inds_cd")
    private String indsCd; // 업종코드

    @JsonProperty("tic_scope")
    private String ticScope; // 틱범위

}
