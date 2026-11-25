package com.example.stockanalysis.kiwoom.contract.tr.kt50003;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 금현물 취소주문(kt50003) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt50003Request {

    @JsonProperty("orig_ord_no")
    private String origOrdNo; // 원주문번호

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("cncl_qty")
    private String cnclQty; // 취소수량

}
