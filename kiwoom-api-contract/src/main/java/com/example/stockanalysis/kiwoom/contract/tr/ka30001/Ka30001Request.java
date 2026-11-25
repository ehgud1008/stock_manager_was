package com.example.stockanalysis.kiwoom.contract.tr.ka30001;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ELW가격급등락요청(ka30001) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka30001Request {

    @JsonProperty("flu_tp")
    private String fluTp; // 등락구분

    @JsonProperty("tm_tp")
    private String tmTp; // 시간구분

    @JsonProperty("tm")
    private String tm; // 시간

    @JsonProperty("trde_qty_tp")
    private String trdeQtyTp; // 거래량구분

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
