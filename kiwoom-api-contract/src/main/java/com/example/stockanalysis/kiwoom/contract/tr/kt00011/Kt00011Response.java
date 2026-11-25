package com.example.stockanalysis.kiwoom.contract.tr.kt00011;

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
 * 증거금율별주문가능수량조회요청(kt00011) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt00011Response implements KiwoomResult {

    @JsonProperty("stk_profa_rt")
    private String stkProfaRt; // 종목증거금율

    @JsonProperty("profa_rt")
    private String profaRt; // 계좌증거금율

    @JsonProperty("aplc_rt")
    private String aplcRt; // 적용증거금율

    @JsonProperty("profa_20ord_alow_amt")
    private String profa20ordAlowAmt; // 증거금20%주문가능금액

    @JsonProperty("profa_20ord_alowq")
    private String profa20ordAlowq; // 증거금20%주문가능수량

    @JsonProperty("profa_20pred_reu_amt")
    private String profa20predReuAmt; // 증거금20%전일재사용금액

    @JsonProperty("profa_20tdy_reu_amt")
    private String profa20tdyReuAmt; // 증거금20%금일재사용금액

    @JsonProperty("profa_30ord_alow_amt")
    private String profa30ordAlowAmt; // 증거금30%주문가능금액

    @JsonProperty("profa_30ord_alowq")
    private String profa30ordAlowq; // 증거금30%주문가능수량

    @JsonProperty("profa_30pred_reu_amt")
    private String profa30predReuAmt; // 증거금30%전일재사용금액

    @JsonProperty("profa_30tdy_reu_amt")
    private String profa30tdyReuAmt; // 증거금30%금일재사용금액

    @JsonProperty("profa_40ord_alow_amt")
    private String profa40ordAlowAmt; // 증거금40%주문가능금액

    @JsonProperty("profa_40ord_alowq")
    private String profa40ordAlowq; // 증거금40%주문가능수량

    @JsonProperty("profa_40pred_reu_amt")
    private String profa40predReuAmt; // 증거금40전일재사용금액

    @JsonProperty("profa_40tdy_reu_amt")
    private String profa40tdyReuAmt; // 증거금40%금일재사용금액

    @JsonProperty("profa_50ord_alow_amt")
    private String profa50ordAlowAmt; // 증거금50%주문가능금액

    @JsonProperty("profa_50ord_alowq")
    private String profa50ordAlowq; // 증거금50%주문가능수량

    @JsonProperty("profa_50pred_reu_amt")
    private String profa50predReuAmt; // 증거금50%전일재사용금액

    @JsonProperty("profa_50tdy_reu_amt")
    private String profa50tdyReuAmt; // 증거금50%금일재사용금액

    @JsonProperty("profa_60ord_alow_amt")
    private String profa60ordAlowAmt; // 증거금60%주문가능금액

    @JsonProperty("profa_60ord_alowq")
    private String profa60ordAlowq; // 증거금60%주문가능수량

    @JsonProperty("profa_60pred_reu_amt")
    private String profa60predReuAmt; // 증거금60%전일재사용금액

    @JsonProperty("profa_60tdy_reu_amt")
    private String profa60tdyReuAmt; // 증거금60%금일재사용금액

    @JsonProperty("profa_100ord_alow_amt")
    private String profa100ordAlowAmt; // 증거금100%주문가능금액

    @JsonProperty("profa_100ord_alowq")
    private String profa100ordAlowq; // 증거금100%주문가능수량

    @JsonProperty("profa_100pred_reu_amt")
    private String profa100predReuAmt; // 증거금100%전일재사용금액

    @JsonProperty("profa_100tdy_reu_amt")
    private String profa100tdyReuAmt; // 증거금100%금일재사용금액

    @JsonProperty("min_ord_alow_amt")
    private String minOrdAlowAmt; // 미수불가주문가능금액

    @JsonProperty("min_ord_alowq")
    private String minOrdAlowq; // 미수불가주문가능수량

    @JsonProperty("min_pred_reu_amt")
    private String minPredReuAmt; // 미수불가전일재사용금액

    @JsonProperty("min_tdy_reu_amt")
    private String minTdyReuAmt; // 미수불가금일재사용금액

    @JsonProperty("entr")
    private String entr; // 예수금

    @JsonProperty("repl_amt")
    private String replAmt; // 대용금

    @JsonProperty("uncla")
    private String uncla; // 미수금

    @JsonProperty("ord_pos_repl")
    private String ordPosRepl; // 주문가능대용

    @JsonProperty("ord_alowa")
    private String ordAlowa; // 주문가능현금

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
