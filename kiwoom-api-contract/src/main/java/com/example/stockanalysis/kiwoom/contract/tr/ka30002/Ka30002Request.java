package com.example.stockanalysis.kiwoom.contract.tr.ka30002;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 거래원별ELW순매매상위요청(ka30002) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka30002Request {

    @JsonProperty("isscomp_cd")
    private String isscompCd; // 발행사코드

    @JsonProperty("trde_qty_tp")
    private String trdeQtyTp; // 거래량구분

    @JsonProperty("trde_tp")
    private String trdeTp; // 매매구분

    @JsonProperty("dt")
    private String dt; // 기간

    @JsonProperty("trde_end_elwskip")
    private String trdeEndElwskip; // 거래종료ELW제외

}
