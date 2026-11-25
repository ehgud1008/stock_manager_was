package com.example.stockanalysis.kiwoom.contract.tr.ust21121;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 해외증권 원장 평가금액현황(ust21121) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21121Request {

    @JsonProperty("cmsn_incl_tp")
    private String cmsnInclTp; // 수수료포함구분

    @JsonProperty("exrt_tp")
    private String exrtTp; // 환율구분

}
