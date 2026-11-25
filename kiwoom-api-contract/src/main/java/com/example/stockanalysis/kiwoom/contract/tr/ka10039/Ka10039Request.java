package com.example.stockanalysis.kiwoom.contract.tr.ka10039;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 증권사별매매상위요청(ka10039) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10039Request {

    @JsonProperty("mmcm_cd")
    private String mmcmCd; // 회원사코드

    @JsonProperty("trde_qty_tp")
    private String trdeQtyTp; // 거래량구분

    @JsonProperty("trde_tp")
    private String trdeTp; // 매매구분

    @JsonProperty("dt")
    private String dt; // 기간

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
