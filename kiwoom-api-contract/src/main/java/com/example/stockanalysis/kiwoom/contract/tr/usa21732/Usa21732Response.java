package com.example.stockanalysis.kiwoom.contract.tr.usa21732;

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
 * 미국주식 연도별종목수익률현황(usa21732) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa21732Response implements KiwoomResult {

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

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("frst_base_dt")
        private String frstBaseDt; // 최초기준일

        @JsonProperty("last_base_dt")
        private String lastBaseDt; // 최종기준일

        @JsonProperty("pl_amt")
        private String plAmt; // 매매수익

        @JsonProperty("prft_rt")
        private String prftRt; // 수익률

        @JsonProperty("frst_stk_evlta")
        private String frstStkEvlta; // 최초주식평가금

        @JsonProperty("last_stk_evlta")
        private String lastStkEvlta; // 최종주식평가금

        @JsonProperty("frst_qty")
        private String frstQty; // 최초수량

        @JsonProperty("last_qty")
        private String lastQty; // 최종수량

        @JsonProperty("buy_qty")
        private String buyQty; // 매수수량

        @JsonProperty("sell_qty")
        private String sellQty; // 매도수량

        @JsonProperty("buy_amt")
        private String buyAmt; // 매수금

        @JsonProperty("sell_amt")
        private String sellAmt; // 매도금

        @JsonProperty("frgn_stk_inq_amt")
        private String frgnStkInqAmt; // 입고평가금

        @JsonProperty("frgn_stk_outq_amt")
        private String frgnStkOutqAmt; // 출고평가금

        @JsonProperty("cmsn_tax")
        private String cmsnTax; // 수수료+세금

        @JsonProperty("dvid_amt")
        private String dvidAmt; // 배당금

        @JsonProperty("crnc_code")
        private String crncCode; // 통화코드
    }
}
