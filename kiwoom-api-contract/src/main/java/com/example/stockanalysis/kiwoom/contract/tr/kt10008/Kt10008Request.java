package com.example.stockanalysis.kiwoom.contract.tr.kt10008;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 신용 정정주문(kt10008) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt10008Request {

    @JsonProperty("dmst_stex_tp")
    private String dmstStexTp; // 국내거래소구분

    @JsonProperty("orig_ord_no")
    private String origOrdNo; // 원주문번호

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("mdfy_qty")
    private String mdfyQty; // 정정수량

    @JsonProperty("mdfy_uv")
    private String mdfyUv; // 정정단가

    @JsonProperty("mdfy_cond_uv")
    private String mdfyCondUv; // 정정조건단가

}
