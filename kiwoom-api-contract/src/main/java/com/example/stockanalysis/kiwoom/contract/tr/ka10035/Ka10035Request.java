package com.example.stockanalysis.kiwoom.contract.tr.ka10035;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 외인연속순매매상위요청(ka10035) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10035Request {

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 시장구분

    @JsonProperty("trde_tp")
    private String trdeTp; // 매매구분

    @JsonProperty("base_dt_tp")
    private String baseDtTp; // 기준일구분

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
