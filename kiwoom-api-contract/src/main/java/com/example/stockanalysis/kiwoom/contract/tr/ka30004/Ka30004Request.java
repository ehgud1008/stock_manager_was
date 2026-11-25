package com.example.stockanalysis.kiwoom.contract.tr.ka30004;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ELW괴리율요청(ka30004) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka30004Request {

    @JsonProperty("isscomp_cd")
    private String isscompCd; // 발행사코드

    @JsonProperty("bsis_aset_cd")
    private String bsisAsetCd; // 기초자산코드

    @JsonProperty("rght_tp")
    private String rghtTp; // 권리구분

    @JsonProperty("lpcd")
    private String lpcd; // LP코드

    @JsonProperty("trde_end_elwskip")
    private String trdeEndElwskip; // 거래종료ELW제외

}
