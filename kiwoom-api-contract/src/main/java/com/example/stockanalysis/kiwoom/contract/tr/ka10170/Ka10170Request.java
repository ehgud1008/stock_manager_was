package com.example.stockanalysis.kiwoom.contract.tr.ka10170;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 당일매매일지요청(ka10170) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10170Request {

    @JsonProperty("base_dt")
    private String baseDt; // 기준일자

    @JsonProperty("ottks_tp")
    private String ottksTp; // 단주구분

    @JsonProperty("ch_crd_tp")
    private String chCrdTp; // 현금신용구분

}
