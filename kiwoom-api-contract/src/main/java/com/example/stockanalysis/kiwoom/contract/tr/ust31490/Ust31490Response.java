package com.example.stockanalysis.kiwoom.contract.tr.ust31490;

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
 * 미국주식 주문가능수량(종목/증거금률별)(ust31490) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust31490Response implements KiwoomResult {

    @JsonProperty("stk_profa_rt")
    private String stkProfaRt; // 종목증거금율

    @JsonProperty("profa_rt")
    private String profaRt; // 계좌증거금율

    @JsonProperty("aplc_rt")
    private String aplcRt; // 적용증거금율

    @JsonProperty("krw_ord_rqst_yn")
    private String krwOrdRqstYn; // 원화주문신청여부

    @JsonProperty("krw_ord_alowa_50")
    private String krwOrdAlowa50; // 증거금50%종목 원화주문가능금액

    @JsonProperty("krw_ord_alowq_50")
    private String krwOrdAlowq50; // 증거금50%종목 원화주문가능수량

    @JsonProperty("ord_alowa_50")
    private String ordAlowa50; // 증거금50%종목 주문가능금액

    @JsonProperty("ord_alowq_50")
    private String ordAlowq50; // 증거금50%종목 주문가능수량

    @JsonProperty("pred_rebuy_alowa_50")
    private String predRebuyAlowa50; // 증거금50%종목 전일재사용금액

    @JsonProperty("tdy_rebuy_alowa_50")
    private String tdyRebuyAlowa50; // 증거금50%종목 금일재사용금액

    @JsonProperty("krw_ord_alowa_100")
    private String krwOrdAlowa100; // 증거금100%종목원화주문가능금액

    @JsonProperty("krw_ord_alowq_100")
    private String krwOrdAlowq100; // 증거금100%종목원화주문가능수량

    @JsonProperty("ord_alowa_100")
    private String ordAlowa100; // 증거금100%종목 주문가능금액

    @JsonProperty("ord_alowq_100")
    private String ordAlowq100; // 증거금100%종목 주문가능수량

    @JsonProperty("pred_rebuy_alowa_100")
    private String predRebuyAlowa100; // 증거금100%종목 전일재사용금액

    @JsonProperty("tdy_rebuy_alowa_100")
    private String tdyRebuyAlowa100; // 증거금100%종목 금일재사용금액

    @JsonProperty("min_krw_ord_alowa")
    private String minKrwOrdAlowa; // 미수불가 원화주문가능금액

    @JsonProperty("min_krw_ord_alowq")
    private String minKrwOrdAlowq; // 미수불가 원화주문가능수량

    @JsonProperty("min_ord_alowa")
    private String minOrdAlowa; // 미수불가 주문가능금액

    @JsonProperty("min_ord_alowq")
    private String minOrdAlowq; // 미수불가 주문가능수량

    @JsonProperty("min_pred_rebuy_alowa")
    private String minPredRebuyAlowa; // 미수불가 전일재사용금액

    @JsonProperty("min_tdy_rebuy_alowa")
    private String minTdyRebuyAlowa; // 미수불가 금일재사용금액

    @JsonProperty("krw_entra")
    private String krwEntra; // 원화예수금

    @JsonProperty("fc_entra")
    private String fcEntra; // 외화예수금

    @JsonProperty("fc_uncl_amt")
    private String fcUnclAmt; // 외화미수금

    @JsonProperty("ord_alowa")
    private String ordAlowa; // 주문가능현금

    @JsonProperty("krw_ord_set_amt")
    private String krwOrdSetAmt; // 해외원화주문설정금

    @JsonProperty("krw_ord_evlt_amt")
    private String krwOrdEvltAmt; // 해외원화주문평가금

    @JsonProperty("crnc_code")
    private String crncCode; // 통화

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
