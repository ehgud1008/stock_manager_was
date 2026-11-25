package com.example.stockanalysis.kiwoom.contract.tr.ka20006;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 업종일봉조회요청(ka20006) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka20006Request {

    @JsonProperty("inds_cd")
    private String indsCd; // 업종코드

    @JsonProperty("base_dt")
    private String baseDt; // 기준일자

}
