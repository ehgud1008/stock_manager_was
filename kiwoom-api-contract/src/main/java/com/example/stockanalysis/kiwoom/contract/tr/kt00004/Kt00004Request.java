package com.example.stockanalysis.kiwoom.contract.tr.kt00004;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 계좌평가현황요청(kt00004) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt00004Request {

    @JsonProperty("qry_tp")
    private String qryTp; // 상장폐지조회구분

    @JsonProperty("dmst_stex_tp")
    private String dmstStexTp; // 국내거래소구분

}
