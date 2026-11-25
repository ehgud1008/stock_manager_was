package com.example.stockanalysis.kiwoom.contract.tr.ka10045;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 종목별기관매매추이요청(ka10045) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10045Request {

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("strt_dt")
    private String strtDt; // 시작일자

    @JsonProperty("end_dt")
    private String endDt; // 종료일자

    @JsonProperty("orgn_prsm_unp_tp")
    private String orgnPrsmUnpTp; // 기관추정단가구분

    @JsonProperty("for_prsm_unp_tp")
    private String forPrsmUnpTp; // 외인추정단가구분

}
