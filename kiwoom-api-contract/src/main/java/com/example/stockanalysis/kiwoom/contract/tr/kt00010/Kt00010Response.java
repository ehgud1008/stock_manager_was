package com.example.stockanalysis.kiwoom.contract.tr.kt00010;

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
 * 주문인출가능금액요청(kt00010) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt00010Response implements KiwoomResult {

    @JsonProperty("profa_20ord_alow_amt")
    private String profa20ordAlowAmt; // 증거금20%주문가능금액

    @JsonProperty("profa_20ord_alowq")
    private String profa20ordAlowq; // 증거금20%주문가능수량

    @JsonProperty("profa_30ord_alow_amt")
    private String profa30ordAlowAmt; // 증거금30%주문가능금액

    @JsonProperty("profa_30ord_alowq")
    private String profa30ordAlowq; // 증거금30%주문가능수량

    @JsonProperty("profa_40ord_alow_amt")
    private String profa40ordAlowAmt; // 증거금40%주문가능금액

    @JsonProperty("profa_40ord_alowq")
    private String profa40ordAlowq; // 증거금40%주문가능수량

    @JsonProperty("profa_50ord_alow_amt")
    private String profa50ordAlowAmt; // 증거금50%주문가능금액

    @JsonProperty("profa_50ord_alowq")
    private String profa50ordAlowq; // 증거금50%주문가능수량

    @JsonProperty("profa_60ord_alow_amt")
    private String profa60ordAlowAmt; // 증거금60%주문가능금액

    @JsonProperty("profa_60ord_alowq")
    private String profa60ordAlowq; // 증거금60%주문가능수량

    @JsonProperty("profa_rdex_60ord_alow_amt")
    private String profaRdex60ordAlowAmt; // 증거금감면60%주문가능금

    @JsonProperty("profa_rdex_60ord_alowq")
    private String profaRdex60ordAlowq; // 증거금감면60%주문가능수

    @JsonProperty("profa_100ord_alow_amt")
    private String profa100ordAlowAmt; // 증거금100%주문가능금액

    @JsonProperty("profa_100ord_alowq")
    private String profa100ordAlowq; // 증거금100%주문가능수량

    @JsonProperty("pred_reu_alowa")
    private String predReuAlowa; // 전일재사용가능금액

    @JsonProperty("tdy_reu_alowa")
    private String tdyReuAlowa; // 금일재사용가능금액

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

    @JsonProperty("wthd_alowa")
    private String wthdAlowa; // 인출가능금액

    @JsonProperty("nxdy_wthd_alowa")
    private String nxdyWthdAlowa; // 익일인출가능금액

    @JsonProperty("pur_amt")
    private String purAmt; // 매입금액

    @JsonProperty("cmsn")
    private String cmsn; // 수수료

    @JsonProperty("pur_exct_amt")
    private String purExctAmt; // 매입정산금

    @JsonProperty("d2entra")
    private String d2entra; // D2추정예수금

    @JsonProperty("profa_rdex_aplc_tp")
    private String profaRdexAplcTp; // 증거금감면적용구분

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
