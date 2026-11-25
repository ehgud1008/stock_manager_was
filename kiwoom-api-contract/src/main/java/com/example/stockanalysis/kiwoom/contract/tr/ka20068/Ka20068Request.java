package com.example.stockanalysis.kiwoom.contract.tr.ka20068;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 대차거래추이요청(종목별)(ka20068) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka20068Request {

    @JsonProperty("strt_dt")
    private String strtDt; // 시작일자

    @JsonProperty("end_dt")
    private String endDt; // 종료일자

    @JsonProperty("all_tp")
    private String allTp; // 전체구분

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

}
