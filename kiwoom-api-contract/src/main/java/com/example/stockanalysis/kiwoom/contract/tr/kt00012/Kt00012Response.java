package com.example.stockanalysis.kiwoom.contract.tr.kt00012;

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
 * 신용보증금율별주문가능수량조회요청(kt00012) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt00012Response implements KiwoomResult {

    @JsonProperty("stk_assr_rt")
    private String stkAssrRt; // 종목보증금율

    @JsonProperty("stk_assr_rt_nm")
    private String stkAssrRtNm; // 종목보증금율명

    @JsonProperty("assr_30ord_alow_amt")
    private String assr30ordAlowAmt; // 보증금30%주문가능금액

    @JsonProperty("assr_30ord_alowq")
    private String assr30ordAlowq; // 보증금30%주문가능수량

    @JsonProperty("assr_30pred_reu_amt")
    private String assr30predReuAmt; // 보증금30%전일재사용금액

    @JsonProperty("assr_30tdy_reu_amt")
    private String assr30tdyReuAmt; // 보증금30%금일재사용금액

    @JsonProperty("assr_40ord_alow_amt")
    private String assr40ordAlowAmt; // 보증금40%주문가능금액

    @JsonProperty("assr_40ord_alowq")
    private String assr40ordAlowq; // 보증금40%주문가능수량

    @JsonProperty("assr_40pred_reu_amt")
    private String assr40predReuAmt; // 보증금40%전일재사용금액

    @JsonProperty("assr_40tdy_reu_amt")
    private String assr40tdyReuAmt; // 보증금40%금일재사용금액

    @JsonProperty("assr_50ord_alow_amt")
    private String assr50ordAlowAmt; // 보증금50%주문가능금액

    @JsonProperty("assr_50ord_alowq")
    private String assr50ordAlowq; // 보증금50%주문가능수량

    @JsonProperty("assr_50pred_reu_amt")
    private String assr50predReuAmt; // 보증금50%전일재사용금액

    @JsonProperty("assr_50tdy_reu_amt")
    private String assr50tdyReuAmt; // 보증금50%금일재사용금액

    @JsonProperty("assr_60ord_alow_amt")
    private String assr60ordAlowAmt; // 보증금60%주문가능금액

    @JsonProperty("assr_60ord_alowq")
    private String assr60ordAlowq; // 보증금60%주문가능수량

    @JsonProperty("assr_60pred_reu_amt")
    private String assr60predReuAmt; // 보증금60%전일재사용금액

    @JsonProperty("assr_60tdy_reu_amt")
    private String assr60tdyReuAmt; // 보증금60%금일재사용금액

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

    @JsonProperty("out_alowa")
    private String outAlowa; // 미수가능금액

    @JsonProperty("out_pos_qty")
    private String outPosQty; // 미수가능수량

    @JsonProperty("min_amt")
    private String minAmt; // 미수불가금액

    @JsonProperty("min_qty")
    private String minQty; // 미수불가수량

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
