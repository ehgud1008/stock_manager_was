package com.example.stockanalysis.kiwoom.contract.tr.kt00016;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 일별계좌수익률상세현황요청(kt00016) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt00016Request {

    @JsonProperty("fr_dt")
    private String frDt; // 평가시작일

    @JsonProperty("to_dt")
    private String toDt; // 평가종료일

}
