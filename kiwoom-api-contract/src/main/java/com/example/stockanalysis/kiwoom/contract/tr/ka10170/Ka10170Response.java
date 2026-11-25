package com.example.stockanalysis.kiwoom.contract.tr.ka10170;

import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 당일매매일지요청(ka10170) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10170Response implements KiwoomResult {

    @JsonProperty("tot_sell_amt")
    private String totSellAmt; // 총매도금액

    @JsonProperty("tot_buy_amt")
    private String totBuyAmt; // 총매수금액

    @JsonProperty("tot_cmsn_tax")
    private String totCmsnTax; // 총수수료_세금

    @JsonProperty("tot_exct_amt")
    private String totExctAmt; // 총정산금액

    @JsonProperty("tot_pl_amt")
    private String totPlAmt; // 총손익금액

    @JsonProperty("tot_prft_rt")
    private String totPrftRt; // 총수익률

    @JsonProperty("tdy_trde_diary")
    private List<TdyTrdeDiaryItem> tdyTrdeDiary; // 당일매매일지

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지


    @Getter
    @Setter
    @Builder
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TdyTrdeDiaryItem {

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("buy_avg_pric")
        private String buyAvgPric; // 매수평균가

        @JsonProperty("buy_qty")
        private String buyQty; // 매수수량

        @JsonProperty("sel_avg_pric")
        private String selAvgPric; // 매도평균가

        @JsonProperty("sell_qty")
        private String sellQty; // 매도수량

        @JsonProperty("cmsn_alm_tax")
        private String cmsnAlmTax; // 수수료_제세금

        @JsonProperty("pl_amt")
        private String plAmt; // 손익금액

        @JsonProperty("sell_amt")
        private String sellAmt; // 매도금액

        @JsonProperty("buy_amt")
        private String buyAmt; // 매수금액

        @JsonProperty("prft_rt")
        private String prftRt; // 수익률

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드
    }
}
