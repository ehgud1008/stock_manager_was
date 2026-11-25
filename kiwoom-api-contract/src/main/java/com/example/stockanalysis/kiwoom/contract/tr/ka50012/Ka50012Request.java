package com.example.stockanalysis.kiwoom.contract.tr.ka50012;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 금현물일별추이(ka50012) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka50012Request {

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("base_dt")
    private String baseDt; // 기준일자

}
