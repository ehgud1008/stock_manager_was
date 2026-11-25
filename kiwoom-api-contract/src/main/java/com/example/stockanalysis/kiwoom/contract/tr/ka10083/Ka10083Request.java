package com.example.stockanalysis.kiwoom.contract.tr.ka10083;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 주식월봉차트조회요청(ka10083) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10083Request {

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("base_dt")
    private String baseDt; // 기준일자

    @JsonProperty("upd_stkpc_tp")
    private String updStkpcTp; // 수정주가구분

}
