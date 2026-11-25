package com.example.stockanalysis.kiwoom.contract.tr.ka01690;

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
 * 일별잔고수익률(ka01690) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka01690Response implements KiwoomResult {

    @JsonProperty("dt")
    private String dt; // 일자

    @JsonProperty("tot_buy_amt")
    private String totBuyAmt; // 총 매입가

    @JsonProperty("tot_evlt_amt")
    private String totEvltAmt; // 총 평가금액

    @JsonProperty("tot_evltv_prft")
    private String totEvltvPrft; // 총 평가손익

    @JsonProperty("tot_prft_rt")
    private String totPrftRt; // 수익률

    @JsonProperty("dbst_bal")
    private String dbstBal; // 예수금

    @JsonProperty("day_stk_asst")
    private String dayStkAsst; // 추정자산

    @JsonProperty("buy_wght")
    private String buyWght; // 현금비중

    @JsonProperty("day_bal_rt")
    private List<DayBalRtItem> dayBalRt; // 일별잔고수익률

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
    public static class DayBalRtItem {

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("rmnd_qty")
        private String rmndQty; // 보유 수량

        @JsonProperty("buy_uv")
        private String buyUv; // 매입 단가

        @JsonProperty("buy_wght")
        private String buyWght; // 매수비중

        @JsonProperty("evltv_prft")
        private String evltvPrft; // 평가손익

        @JsonProperty("prft_rt")
        private String prftRt; // 수익률

        @JsonProperty("evlt_amt")
        private String evltAmt; // 평가금액

        @JsonProperty("evlt_wght")
        private String evltWght; // 평가비중
    }
}
