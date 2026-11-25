package com.example.stockanalysis.kiwoom.contract.tr.ka10024;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 거래량갱신요청(ka10024) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10024Request {

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 시장구분

    @JsonProperty("cycle_tp")
    private String cycleTp; // 주기구분

    @JsonProperty("trde_qty_tp")
    private String trdeQtyTp; // 거래량구분

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
