package com.example.stockanalysis.kiwoom.contract.tr.usa23000;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 업종별 기간별 수익률 조회(usa23000) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa23000Request {

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소 구분

    @JsonProperty("inds_cd")
    private String indsCd; // 업종 코드

}
