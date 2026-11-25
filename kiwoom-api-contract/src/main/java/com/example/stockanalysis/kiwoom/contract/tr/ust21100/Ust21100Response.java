package com.example.stockanalysis.kiwoom.contract.tr.ust21100;

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
 * 미국주식 거래내역(ust21100) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21100Response implements KiwoomResult {

    @JsonProperty("acnt_print")
    private String acntPrint; // 계좌번호

    @JsonProperty("sell_sum")
    private String sellSum; // 매도합계

    @JsonProperty("buy_sum")
    private String buySum; // 매수합계

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

        @JsonProperty("deal_dt")
        private String dealDt; // 거래일자

        @JsonProperty("deal_kind_nm")
        private String dealKindNm; // 거래종류명

        @JsonProperty("rmrk_nm")
        private String rmrkNm; // 적요명

        @JsonProperty("deal_amt")
        private String dealAmt; // 거래금액

        @JsonProperty("tax_tot_amt")
        private String taxTotAmt; // 소득/주민세

        @JsonProperty("exct_amt")
        private String exctAmt; // 정산금액

        @JsonProperty("uncl_ocr")
        private String unclOcr; // 미수(원/주)

        @JsonProperty("fc_uncl_ocr")
        private String fcUnclOcr; // 미수(외)

        @JsonProperty("entra_remn")
        private String entraRemn; // 예수금잔고

        @JsonProperty("deal_no")
        private String dealNo; // 거래번호

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("deal_qty")
        private String dealQty; // 거래수량

        @JsonProperty("fc_deal_tax")
        private String fcDealTax; // 거래세(외)

        @JsonProperty("frgn_pay_txam")
        private String frgnPayTxam; // 외국납부세액(외)

        @JsonProperty("rpym_sum")
        private String rpymSum; // 변제합

        @JsonProperty("fc_rpym_sum")
        private String fcRpymSum; // 변제합(외)

        @JsonProperty("fc_entra")
        private String fcEntra; // 외화예수금잔고

        @JsonProperty("mdia_nm")
        private String mdiaNm; // 메체구분명

        @JsonProperty("orig_deal_no")
        private String origDealNo; // 원거래번호

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("uv_exrt")
        private String uvExrt; // 거래단가/환율

        @JsonProperty("fc_cmsn")
        private String fcCmsn; // 수수료(외)

        @JsonProperty("fc_exct_amt")
        private String fcExctAmt; // 정산금액(외)

        @JsonProperty("dly_sum")
        private String dlySum; // 연체합

        @JsonProperty("fc_dly_sum")
        private String fcDlySum; // 연체합(외)

        @JsonProperty("vlbl_nowrm")
        private String vlblNowrm; // 유가금잔

        @JsonProperty("stex_nm")
        private String stexNm; // 거래소구분명

        @JsonProperty("fc_deal_amt")
        private String fcDealAmt; // 거래금액(외)

        @JsonProperty("proc_time")
        private String procTime; // 처리시간

        @JsonProperty("crnc_code")
        private String crncCode; // 통화코드
    }
}
