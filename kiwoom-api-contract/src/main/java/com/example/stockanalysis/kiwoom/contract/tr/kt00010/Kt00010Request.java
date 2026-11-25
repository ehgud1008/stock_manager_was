package com.example.stockanalysis.kiwoom.contract.tr.kt00010;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 주문인출가능금액요청(kt00010) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt00010Request {

    @JsonProperty("io_amt")
    private String ioAmt; // 입출금액

    @JsonProperty("stk_cd")
    private String stkCd; // 종목번호

    @JsonProperty("trde_tp")
    private String trdeTp; // 매매구분

    @JsonProperty("trde_qty")
    private String trdeQty; // 매매수량

    @JsonProperty("uv")
    private String uv; // 매수가격

    @JsonProperty("exp_buy_unp")
    private String expBuyUnp; // 예상매수단가

}
