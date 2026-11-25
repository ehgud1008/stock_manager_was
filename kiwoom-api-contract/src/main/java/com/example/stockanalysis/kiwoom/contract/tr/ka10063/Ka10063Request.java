package com.example.stockanalysis.kiwoom.contract.tr.ka10063;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 장중투자자별매매요청(ka10063) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10063Request {

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 시장구분

    @JsonProperty("amt_qty_tp")
    private String amtQtyTp; // 금액수량구분

    @JsonProperty("invsr")
    private String invsr; // 투자자별

    @JsonProperty("frgn_all")
    private String frgnAll; // 외국계전체

    @JsonProperty("smtm_netprps_tp")
    private String smtmNetprpsTp; // 동시순매수구분

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
