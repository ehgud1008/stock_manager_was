package com.example.stockanalysis.kiwoom.contract.tr.ka10043;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 거래원매물대분석요청(ka10043) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10043Request {

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("strt_dt")
    private String strtDt; // 시작일자

    @JsonProperty("end_dt")
    private String endDt; // 종료일자

    @JsonProperty("qry_dt_tp")
    private String qryDtTp; // 조회기간구분

    @JsonProperty("pot_tp")
    private String potTp; // 시점구분

    @JsonProperty("dt")
    private String dt; // 기간

    @JsonProperty("sort_base")
    private String sortBase; // 정렬기준

    @JsonProperty("mmcm_cd")
    private String mmcmCd; // 회원사코드

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
