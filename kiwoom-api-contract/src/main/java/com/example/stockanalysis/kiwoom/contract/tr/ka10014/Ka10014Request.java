package com.example.stockanalysis.kiwoom.contract.tr.ka10014;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 공매도추이요청(ka10014) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10014Request {

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("tm_tp")
    private String tmTp; // 시간구분

    @JsonProperty("strt_dt")
    private String strtDt; // 시작일자

    @JsonProperty("end_dt")
    private String endDt; // 종료일자

}
