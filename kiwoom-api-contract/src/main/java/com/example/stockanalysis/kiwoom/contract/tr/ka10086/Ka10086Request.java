package com.example.stockanalysis.kiwoom.contract.tr.ka10086;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 일별주가요청(ka10086) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10086Request {

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("qry_dt")
    private String qryDt; // 조회일자

    @JsonProperty("indc_tp")
    private String indcTp; // 표시구분

}
