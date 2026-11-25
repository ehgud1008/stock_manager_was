package com.example.stockanalysis.kiwoom.contract.tr.kt50000;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 금현물 매수주문(kt50000) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt50000Request {

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("ord_qty")
    private String ordQty; // 주문수량

    @JsonProperty("ord_uv")
    private String ordUv; // 주문단가

    @JsonProperty("trde_tp")
    private String trdeTp; // 매매구분

}
