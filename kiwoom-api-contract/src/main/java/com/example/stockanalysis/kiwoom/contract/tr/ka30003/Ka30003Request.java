package com.example.stockanalysis.kiwoom.contract.tr.ka30003;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ELWLP보유일별추이요청(ka30003) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka30003Request {

    @JsonProperty("bsis_aset_cd")
    private String bsisAsetCd; // 기초자산코드

    @JsonProperty("base_dt")
    private String baseDt; // 기준일자

}
