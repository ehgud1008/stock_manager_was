package com.example.stockanalysis.kiwoom.contract.tr.ka10059;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 종목별투자자기관별요청(ka10059) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10059Request {

    @JsonProperty("dt")
    private String dt; // 일자

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("amt_qty_tp")
    private String amtQtyTp; // 금액수량구분

    @JsonProperty("trde_tp")
    private String trdeTp; // 매매구분

    @JsonProperty("unit_tp")
    private String unitTp; // 단위구분

}
