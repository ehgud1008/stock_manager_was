package com.example.stockanalysis.kiwoom.contract.tr.ust21650;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 기간별 수익률 현황(ust21650) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21650Request {

    @JsonProperty("fr_dt")
    private String frDt; // 조회시작일자

    @JsonProperty("to_dt")
    private String toDt; // 조회종료일자

}
