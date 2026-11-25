package com.example.stockanalysis.kiwoom.contract.tr.ka20009;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 업종현재가일별요청(ka20009) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka20009Request {

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 시장구분

    @JsonProperty("inds_cd")
    private String indsCd; // 업종코드

}
