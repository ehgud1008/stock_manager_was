package com.example.stockanalysis.kiwoom.contract.tr.kt00002;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 일별추정예탁자산현황요청(kt00002) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt00002Request {

    @JsonProperty("start_dt")
    private String startDt; // 시작조회기간

    @JsonProperty("end_dt")
    private String endDt; // 종료조회기간

}
