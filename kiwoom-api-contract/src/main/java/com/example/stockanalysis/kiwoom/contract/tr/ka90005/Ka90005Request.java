package com.example.stockanalysis.kiwoom.contract.tr.ka90005;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 프로그램매매추이요청 시간대별(ka90005) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka90005Request {

    @JsonProperty("date")
    private String date; // 날짜

    @JsonProperty("amt_qty_tp")
    private String amtQtyTp; // 금액수량구분

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 시장구분

    @JsonProperty("min_tic_tp")
    private String minTicTp; // 분틱구분

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
