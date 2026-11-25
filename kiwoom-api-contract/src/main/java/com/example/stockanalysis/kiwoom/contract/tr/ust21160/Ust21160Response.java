package com.example.stockanalysis.kiwoom.contract.tr.ust21160;

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
 * 미국주식 예수금 상세(ust21160) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21160Response implements KiwoomResult {

    @JsonProperty("won_entr")
    private String wonEntr; // 원화 예수금

    @JsonProperty("won_dfr_amt")
    private String wonDfrAmt; // 미수금

    @JsonProperty("won_etc_loana")
    private String wonEtcLoana; // 기타 대여금

    @JsonProperty("krw_ord_set_amt")
    private String krwOrdSetAmt; // 해외원화주문설정금

    @JsonProperty("usd_exch_rate")
    private String usdExchRate; // 매도환율(USD)

    @JsonProperty("d0_setl_dt")
    private String d0SetlDt; // D0 국내결제일자

    @JsonProperty("d0_won_conv_alow_ch")
    private String d0WonConvAlowCh; // D0 원화환산추정인출가능금

    @JsonProperty("d0_usd_fx_entr")
    private String d0UsdFxEntr; // D0 외화예수금(USD)

    @JsonProperty("d1_setl_dt")
    private String d1SetlDt; // D1 국내결제일자

    @JsonProperty("d1_won_conv_alow_ch")
    private String d1WonConvAlowCh; // D1 원화환산추정인출가능금

    @JsonProperty("d1_usd_fx_entr")
    private String d1UsdFxEntr; // D1 외화예수금(USD)

    @JsonProperty("d1_usd_exct_amt")
    private String d1UsdExctAmt; // D1 해외정산금(USD)

    @JsonProperty("d1_usd_buy_excta")
    private String d1UsdBuyExcta; // D1 해외매수정산금(USD)

    @JsonProperty("d1_usd_sell_excta")
    private String d1UsdSellExcta; // D1 해외매도정산금(USD)

    @JsonProperty("d2_setl_dt")
    private String d2SetlDt; // D2 국내결제일자

    @JsonProperty("d2_won_conv_alow_ch")
    private String d2WonConvAlowCh; // D2 원화환산추정인출가능금

    @JsonProperty("d2_usd_fx_entr")
    private String d2UsdFxEntr; // D2 외화예수금(USD)

    @JsonProperty("d2_usd_exct_amt")
    private String d2UsdExctAmt; // D2 해외정산금(USD)

    @JsonProperty("d2_usd_buy_excta")
    private String d2UsdBuyExcta; // D2 해외매수정산금(USD)

    @JsonProperty("d2_usd_sell_excta")
    private String d2UsdSellExcta; // D2 해외매도정산금(USD)

    @JsonProperty("d3_setl_dt")
    private String d3SetlDt; // D3 국내결제일자

    @JsonProperty("d3_won_conv_alow_ch")
    private String d3WonConvAlowCh; // D3 원화환산추정인출가능금

    @JsonProperty("d3_usd_fx_entr")
    private String d3UsdFxEntr; // D3 외화예수금(USD)

    @JsonProperty("d3_usd_exct_amt")
    private String d3UsdExctAmt; // D3 해외정산금(USD)

    @JsonProperty("d3_usd_buy_excta")
    private String d3UsdBuyExcta; // D3 해외매수정산금(USD)

    @JsonProperty("d3_usd_sell_excta")
    private String d3UsdSellExcta; // D3 해외매도정산금(USD)

    @JsonProperty("d4_setl_dt")
    private String d4SetlDt; // D4 국내결제일자

    @JsonProperty("d4_won_conv_alow_ch")
    private String d4WonConvAlowCh; // D4 원화환산추정인출가능금

    @JsonProperty("d4_usd_fx_entr")
    private String d4UsdFxEntr; // D4 외화예수금(USD)

    @JsonProperty("d4_usd_exct_amt")
    private String d4UsdExctAmt; // D4 해외정산금(USD)

    @JsonProperty("d4_usd_buy_excta")
    private String d4UsdBuyExcta; // D4 해외매수정산금(USD)

    @JsonProperty("d4_usd_sell_excta")
    private String d4UsdSellExcta; // D4 해외매도정산금(USD)

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
