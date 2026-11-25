package com.example.stockanalysis.kiwoom.contract.tr.ka10015;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 일별거래상세요청(ka10015) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10015Request {

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("strt_dt")
    private String strtDt; // 시작일자

}
