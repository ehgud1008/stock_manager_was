package com.example.stockanalysis.kiwoom.contract.tr.kt50002;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 금현물 정정주문(kt50002) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt50002Request {

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("orig_ord_no")
    private String origOrdNo; // 원주문번호

    @JsonProperty("mdfy_qty")
    private String mdfyQty; // 정정수량

    @JsonProperty("mdfy_uv")
    private String mdfyUv; // 정정단가

}
