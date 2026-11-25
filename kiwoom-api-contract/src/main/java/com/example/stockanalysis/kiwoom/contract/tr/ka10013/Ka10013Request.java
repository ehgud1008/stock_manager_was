package com.example.stockanalysis.kiwoom.contract.tr.ka10013;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 신용매매동향요청(ka10013) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10013Request {

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("dt")
    private String dt; // 일자

    @JsonProperty("qry_tp")
    private String qryTp; // 조회구분

}
