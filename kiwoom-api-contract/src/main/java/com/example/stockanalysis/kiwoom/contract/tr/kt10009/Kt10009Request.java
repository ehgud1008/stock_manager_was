package com.example.stockanalysis.kiwoom.contract.tr.kt10009;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 신용 취소주문(kt10009) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt10009Request {

    @JsonProperty("dmst_stex_tp")
    private String dmstStexTp; // 국내거래소구분

    @JsonProperty("orig_ord_no")
    private String origOrdNo; // 원주문번호

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("cncl_qty")
    private String cnclQty; // 취소수량

}
