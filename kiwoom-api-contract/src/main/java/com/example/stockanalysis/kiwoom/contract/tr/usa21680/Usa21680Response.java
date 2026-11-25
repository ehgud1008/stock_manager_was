package com.example.stockanalysis.kiwoom.contract.tr.usa21680;

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
 * 미국주식 월별계좌수익률현황(usa21680) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa21680Response implements KiwoomResult {

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

        @JsonProperty("base_dt")
        private String baseDt; // 기준일

        @JsonProperty("stk_evlta")
        private String stkEvlta; // 주식평가금

        @JsonProperty("pl_amt")
        private String plAmt; // 손익금액

        @JsonProperty("dvid_amt")
        private String dvidAmt; // 배당금액

        @JsonProperty("cmsn_tax")
        private String cmsnTax; // 수수료+세금

        @JsonProperty("acum_pl_amt")
        private String acumPlAmt; // 누적손익

        @JsonProperty("pymn_amt")
        private String pymnAmt; // 출금금액

        @JsonProperty("dast")
        private String dast; // 예탁자산

        @JsonProperty("dly_amt")
        private String dlyAmt; // 연체금액

        @JsonProperty("sell_amt")
        private String sellAmt; // 매도금액

        @JsonProperty("buy_amt")
        private String buyAmt; // 매수금액

        @JsonProperty("prft_rt")
        private String prftRt; // 수익률

        @JsonProperty("frgn_stk_outq_amt")
        private String frgnStkOutqAmt; // 출고금액

        @JsonProperty("frgn_stk_inq_amt")
        private String frgnStkInqAmt; // 입고금액

        @JsonProperty("ina_amt")
        private String inaAmt; // 입금금액

        @JsonProperty("exrt")
        private String exrt; // 환율
    }
}
