package com.example.stockanalysis.kiwoom.contract.tr.kt00018;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 계좌평가잔고내역요청(kt00018) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt00018Request {

    @JsonProperty("qry_tp")
    private String qryTp; // 조회구분

    @JsonProperty("dmst_stex_tp")
    private String dmstStexTp; // 국내거래소구분

}
