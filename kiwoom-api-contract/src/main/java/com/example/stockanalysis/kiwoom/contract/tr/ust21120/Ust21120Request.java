package com.example.stockanalysis.kiwoom.contract.tr.ust21120;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 통화별 예수금 및 증권 평가금현황(ust21120) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21120Request {

    @JsonProperty("cmsn_incl_tp")
    private String cmsnInclTp; // 수수료포함구분

    @JsonProperty("exrt_tp")
    private String exrtTp; // 환율구분

}
