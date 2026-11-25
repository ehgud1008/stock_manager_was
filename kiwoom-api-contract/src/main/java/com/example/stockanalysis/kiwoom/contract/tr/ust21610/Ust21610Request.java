package com.example.stockanalysis.kiwoom.contract.tr.ust21610;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 당일매매(ust21610) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21610Request {

    @JsonProperty("base_dt")
    private String baseDt; // 기준일자

    @JsonProperty("qry_tp")
    private String qryTp; // 조회구분

    @JsonProperty("fc_krw_tp")
    private String fcKrwTp; // 외화원화구분

}
