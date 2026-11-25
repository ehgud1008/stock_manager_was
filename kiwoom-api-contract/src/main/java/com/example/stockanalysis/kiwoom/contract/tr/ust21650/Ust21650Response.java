package com.example.stockanalysis.kiwoom.contract.tr.ust21650;

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
 * 미국주식 기간별 수익률 현황(ust21650) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21650Response implements KiwoomResult {

    @JsonProperty("fr_entr")
    private String frEntr; // 기간초예수금

    @JsonProperty("fr_dfr_amt")
    private String frDfrAmt; // 기간초현금미수금

    @JsonProperty("fr_etc_loana")
    private String frEtcLoana; // 기간초기타대여금

    @JsonProperty("fr_fc_entr")
    private String frFcEntr; // 기간초외화예수금

    @JsonProperty("fr_fc_dfr_amt")
    private String frFcDfrAmt; // 기간초외화미수금

    @JsonProperty("fr_fc_etc_loana")
    private String frFcEtcLoana; // 기간초외화기타대여금

    @JsonProperty("fr_frgn_stk_evltv")
    private String frFrgnStkEvltv; // 기간초해외증권평가금

    @JsonProperty("fr_tot_evltv")
    private String frTotEvltv; // 기간초순자산액계

    @JsonProperty("to_entr")
    private String toEntr; // 기간말예수금

    @JsonProperty("to_dfr_amt")
    private String toDfrAmt; // 기간말현금미수금

    @JsonProperty("to_etc_loana")
    private String toEtcLoana; // 기간말기타대여금

    @JsonProperty("to_fc_entr")
    private String toFcEntr; // 기간말외화예수금

    @JsonProperty("to_fc_dfr_amt")
    private String toFcDfrAmt; // 기간말외화미수금

    @JsonProperty("to_fc_etc_loana")
    private String toFcEtcLoana; // 기간말외화기타대여금

    @JsonProperty("to_frgn_stk_evltv")
    private String toFrgnStkEvltv; // 기간말해외증권평가금

    @JsonProperty("to_tot_evltv")
    private String toTotEvltv; // 기간말순자산액계

    @JsonProperty("fc_rcpta")
    private String fcRcpta; // 기간내총외화입금

    @JsonProperty("fc_payma")
    private String fcPayma; // 기간내총외화출금

    @JsonProperty("chg_rcpta")
    private String chgRcpta; // 기간내외화매도

    @JsonProperty("chg_payma")
    private String chgPayma; // 기간내외화매수

    @JsonProperty("frgn_stk_inqa")
    private String frgnStkInqa; // 기간내해외증권입고

    @JsonProperty("frgn_stk_outqa")
    private String frgnStkOutqa; // 기간내해외증권출고

    @JsonProperty("invt_bsamt")
    private String invtBsamt; // 투자원금평잔

    @JsonProperty("evlt_profit")
    private String evltProfit; // 평가손익

    @JsonProperty("profit_rate")
    private String profitRate; // 수익율

    @JsonProperty("io_bsamt")
    private String ioBsamt; // 기간내총입출금고평잔

    @JsonProperty("tern_rt")
    private String ternRt; // 회전율

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
