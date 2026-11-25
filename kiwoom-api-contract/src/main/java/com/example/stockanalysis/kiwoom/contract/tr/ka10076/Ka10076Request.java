package com.example.stockanalysis.kiwoom.contract.tr.ka10076;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 체결요청(ka10076) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10076Request {

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("qry_tp")
    private String qryTp; // 조회구분

    @JsonProperty("sell_tp")
    private String sellTp; // 매도수구분

    @JsonProperty("ord_no")
    private String ordNo; // 주문번호

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
