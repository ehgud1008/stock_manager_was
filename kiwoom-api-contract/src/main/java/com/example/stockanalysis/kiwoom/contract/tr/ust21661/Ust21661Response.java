package com.example.stockanalysis.kiwoom.contract.tr.ust21661;

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
 * 미국주식 월별 실현손익(ust21661) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21661Response implements KiwoomResult {

    @JsonProperty("tot_buy_amt")
    private String totBuyAmt; // 총매수금액

    @JsonProperty("tot_sell_amt")
    private String totSellAmt; // 총매도금액

    @JsonProperty("tot_pl_amt")
    private String totPlAmt; // 총실현손익금액

    @JsonProperty("tot_cmsn")
    private String totCmsn; // 수수료합

    @JsonProperty("tot_tax")
    private String totTax; // 세금합

    @JsonProperty("result_list")
    private List<ResultListItem> resultList; // 결과리스트

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
    public static class ResultListItem {

        @JsonProperty("trde_dt")
        private String trdeDt; // 매매일자

        @JsonProperty("buy_amt")
        private String buyAmt; // 매수금액

        @JsonProperty("sell_amt")
        private String sellAmt; // 매도금액

        @JsonProperty("pl_amt")
        private String plAmt; // 실현손익금액

        @JsonProperty("cmsn")
        private String cmsn; // 수수료

        @JsonProperty("tax")
        private String tax; // 세금
    }
}
