package com.example.stockanalysis.kiwoom.contract.tr.ust21050;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 원장 미체결(ust21050) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21050Request {

    @JsonProperty("ord_dt")
    private String ordDt; // 주문일자

    @JsonProperty("slby_tp")
    private String slbyTp; // 매도매수구분

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

}
