package com.example.stockanalysis.kiwoom.contract.tr.ka10065;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 장중투자자별매매상위요청(ka10065) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10065Request {

    @JsonProperty("trde_tp")
    private String trdeTp; // 매매구분

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 시장구분

    @JsonProperty("orgn_tp")
    private String orgnTp; // 기관구분

    @JsonProperty("amt_qty_tp")
    private String amtQtyTp; // 금액수량구분

}
