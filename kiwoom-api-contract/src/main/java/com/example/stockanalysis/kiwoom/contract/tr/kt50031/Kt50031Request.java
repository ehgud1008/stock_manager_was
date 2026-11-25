package com.example.stockanalysis.kiwoom.contract.tr.kt50031;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 금현물 주문체결조회(kt50031) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt50031Request {

    @JsonProperty("ord_dt")
    private String ordDt; // 주문일자

    @JsonProperty("qry_tp")
    private String qryTp; // 조회구분

    @JsonProperty("stk_bond_tp")
    private String stkBondTp; // 주식채권구분

    @JsonProperty("sell_tp")
    private String sellTp; // 매도수구분

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("fr_ord_no")
    private String frOrdNo; // 시작주문번호

    @JsonProperty("dmst_stex_tp")
    private String dmstStexTp; // 국내거래소구분

}
