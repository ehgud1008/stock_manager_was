package com.example.stockanalysis.kiwoom.contract.tr.ust31302;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 환전 신청(ust31302) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust31302Request {

    @JsonProperty("exch_tp")
    private String exchTp; // 환전구분

    @JsonProperty("fc_exmn_amt")
    private String fcExmnAmt; // 매도통화기준 환전금액

}
