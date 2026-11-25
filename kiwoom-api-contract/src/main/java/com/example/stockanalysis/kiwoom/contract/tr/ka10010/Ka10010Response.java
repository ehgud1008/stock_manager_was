package com.example.stockanalysis.kiwoom.contract.tr.ka10010;

import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 업종프로그램요청(ka10010) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10010Response implements KiwoomResult {

    @JsonProperty("dfrt_trst_sell_qty")
    private String dfrtTrstSellQty; // 차익위탁매도수량

    @JsonProperty("dfrt_trst_sell_amt")
    private String dfrtTrstSellAmt; // 차익위탁매도금액

    @JsonProperty("dfrt_trst_buy_qty")
    private String dfrtTrstBuyQty; // 차익위탁매수수량

    @JsonProperty("dfrt_trst_buy_amt")
    private String dfrtTrstBuyAmt; // 차익위탁매수금액

    @JsonProperty("dfrt_trst_netprps_qty")
    private String dfrtTrstNetprpsQty; // 차익위탁순매수수량

    @JsonProperty("dfrt_trst_netprps_amt")
    private String dfrtTrstNetprpsAmt; // 차익위탁순매수금액

    @JsonProperty("ndiffpro_trst_sell_qty")
    private String ndiffproTrstSellQty; // 비차익위탁매도수량

    @JsonProperty("ndiffpro_trst_sell_amt")
    private String ndiffproTrstSellAmt; // 비차익위탁매도금액

    @JsonProperty("ndiffpro_trst_buy_qty")
    private String ndiffproTrstBuyQty; // 비차익위탁매수수량

    @JsonProperty("ndiffpro_trst_buy_amt")
    private String ndiffproTrstBuyAmt; // 비차익위탁매수금액

    @JsonProperty("ndiffpro_trst_netprps_qty")
    private String ndiffproTrstNetprpsQty; // 비차익위탁순매수수량

    @JsonProperty("ndiffpro_trst_netprps_amt")
    private String ndiffproTrstNetprpsAmt; // 비차익위탁순매수금액

    @JsonProperty("all_dfrt_trst_sell_qty")
    private String allDfrtTrstSellQty; // 전체차익위탁매도수량

    @JsonProperty("all_dfrt_trst_sell_amt")
    private String allDfrtTrstSellAmt; // 전체차익위탁매도금액

    @JsonProperty("all_dfrt_trst_buy_qty")
    private String allDfrtTrstBuyQty; // 전체차익위탁매수수량

    @JsonProperty("all_dfrt_trst_buy_amt")
    private String allDfrtTrstBuyAmt; // 전체차익위탁매수금액

    @JsonProperty("all_dfrt_trst_netprps_qty")
    private String allDfrtTrstNetprpsQty; // 전체차익위탁순매수수량

    @JsonProperty("all_dfrt_trst_netprps_amt")
    private String allDfrtTrstNetprpsAmt; // 전체차익위탁순매수금액

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
