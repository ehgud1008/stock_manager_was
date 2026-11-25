package com.example.stockanalysis.kiwoom.contract.tr.ust20001;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 매도 주문(ust20001) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust20001Request {

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

    @JsonProperty("ord_qty")
    private String ordQty; // 주문수량

    @JsonProperty("ord_uv")
    private String ordUv; // 주문단가

    @JsonProperty("stop_pric")
    private String stopPric; // STOP가격

    @JsonProperty("trde_tp")
    private String trdeTp; // 매매구분

}
