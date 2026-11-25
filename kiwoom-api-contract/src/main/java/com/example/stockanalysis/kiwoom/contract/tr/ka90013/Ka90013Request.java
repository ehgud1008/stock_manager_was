package com.example.stockanalysis.kiwoom.contract.tr.ka90013;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 종목일별프로그램매매추이요청(ka90013) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka90013Request {

    @JsonProperty("amt_qty_tp")
    private String amtQtyTp; // 금액수량구분

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("date")
    private String date; // 날짜

}
