package com.example.stockanalysis.kiwoom.contract.tr.ka90003;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 프로그램순매수상위50요청(ka90003) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka90003Request {

    @JsonProperty("trde_upper_tp")
    private String trdeUpperTp; // 매매상위구분

    @JsonProperty("amt_qty_tp")
    private String amtQtyTp; // 금액수량구분

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 시장구분

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
