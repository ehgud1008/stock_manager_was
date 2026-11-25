package com.example.stockanalysis.kiwoom.contract.tr.kt00013;

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
 * 증거금세부내역조회요청(kt00013) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt00013Response implements KiwoomResult {

    @JsonProperty("tdy_reu_objt_amt")
    private String tdyReuObjtAmt; // 금일재사용대상금액

    @JsonProperty("tdy_reu_use_amt")
    private String tdyReuUseAmt; // 금일재사용사용금액

    @JsonProperty("tdy_reu_alowa")
    private String tdyReuAlowa; // 금일재사용가능금액

    @JsonProperty("tdy_reu_lmtt_amt")
    private String tdyReuLmttAmt; // 금일재사용제한금액

    @JsonProperty("tdy_reu_alowa_fin")
    private String tdyReuAlowaFin; // 금일재사용가능금액최종

    @JsonProperty("pred_reu_objt_amt")
    private String predReuObjtAmt; // 전일재사용대상금액

    @JsonProperty("pred_reu_use_amt")
    private String predReuUseAmt; // 전일재사용사용금액

    @JsonProperty("pred_reu_alowa")
    private String predReuAlowa; // 전일재사용가능금액

    @JsonProperty("pred_reu_lmtt_amt")
    private String predReuLmttAmt; // 전일재사용제한금액

    @JsonProperty("pred_reu_alowa_fin")
    private String predReuAlowaFin; // 전일재사용가능금액최종

    @JsonProperty("ch_amt")
    private String chAmt; // 현금금액

    @JsonProperty("ch_profa")
    private String chProfa; // 현금증거금

    @JsonProperty("use_pos_ch")
    private String usePosCh; // 사용가능현금

    @JsonProperty("ch_use_lmtt_amt")
    private String chUseLmttAmt; // 현금사용제한금액

    @JsonProperty("use_pos_ch_fin")
    private String usePosChFin; // 사용가능현금최종

    @JsonProperty("repl_amt_amt")
    private String replAmtAmt; // 대용금액

    @JsonProperty("repl_profa")
    private String replProfa; // 대용증거금

    @JsonProperty("use_pos_repl")
    private String usePosRepl; // 사용가능대용

    @JsonProperty("repl_use_lmtt_amt")
    private String replUseLmttAmt; // 대용사용제한금액

    @JsonProperty("use_pos_repl_fin")
    private String usePosReplFin; // 사용가능대용최종

    @JsonProperty("crd_grnta_ch")
    private String crdGrntaCh; // 신용보증금현금

    @JsonProperty("crd_grnta_repl")
    private String crdGrntaRepl; // 신용보증금대용

    @JsonProperty("crd_grnt_ch")
    private String crdGrntCh; // 신용담보금현금

    @JsonProperty("crd_grnt_repl")
    private String crdGrntRepl; // 신용담보금대용

    @JsonProperty("uncla")
    private String uncla; // 미수금

    @JsonProperty("ls_grnt_reu_gold")
    private String lsGrntReuGold; // 대주담보금재사용금

    @JsonProperty("20ord_alow_amt")
    private String _20ordAlowAmt; // 20%주문가능금액

    @JsonProperty("30ord_alow_amt")
    private String _30ordAlowAmt; // 30%주문가능금액

    @JsonProperty("40ord_alow_amt")
    private String _40ordAlowAmt; // 40%주문가능금액

    @JsonProperty("50ord_alow_amt")
    private String _50ordAlowAmt; // 50%주문가능금액

    @JsonProperty("60ord_alow_amt")
    private String _60ordAlowAmt; // 60%주문가능금액

    @JsonProperty("100ord_alow_amt")
    private String _100ordAlowAmt; // 100%주문가능금액

    @JsonProperty("tdy_crd_rpya_loss_amt")
    private String tdyCrdRpyaLossAmt; // 금일신용상환손실금액

    @JsonProperty("pred_crd_rpya_loss_amt")
    private String predCrdRpyaLossAmt; // 전일신용상환손실금액

    @JsonProperty("tdy_ls_rpya_loss_repl_profa")
    private String tdyLsRpyaLossReplProfa; // 금일대주상환손실대용증거금

    @JsonProperty("pred_ls_rpya_loss_repl_profa")
    private String predLsRpyaLossReplProfa; // 전일대주상환손실대용증거금

    @JsonProperty("evlt_repl_amt_spg_use_skip")
    private String evltReplAmtSpgUseSkip; // 평가대용금(현물사용제외)

    @JsonProperty("evlt_repl_rt")
    private String evltReplRt; // 평가대용비율

    @JsonProperty("crd_repl_profa")
    private String crdReplProfa; // 신용대용증거금

    @JsonProperty("ch_ord_repl_profa")
    private String chOrdReplProfa; // 현금주문대용증거금

    @JsonProperty("crd_ord_repl_profa")
    private String crdOrdReplProfa; // 신용주문대용증거금

    @JsonProperty("crd_repl_conv_gold")
    private String crdReplConvGold; // 신용대용환산금

    @JsonProperty("repl_alowa")
    private String replAlowa; // 대용가능금액(현금제한)

    @JsonProperty("repl_alowa_2")
    private String replAlowa2; // 대용가능금액2(신용제한)

    @JsonProperty("ch_repl_lck_gold")
    private String chReplLckGold; // 현금대용부족금

    @JsonProperty("crd_repl_lck_gold")
    private String crdReplLckGold; // 신용대용부족금

    @JsonProperty("ch_ord_alow_repla")
    private String chOrdAlowRepla; // 현금주문가능대용금

    @JsonProperty("crd_ord_alow_repla")
    private String crdOrdAlowRepla; // 신용주문가능대용금

    @JsonProperty("d2vexct_entr")
    private String d2vexctEntr; // D2가정산예수금

    @JsonProperty("d2ch_ord_alow_amt")
    private String d2chOrdAlowAmt; // D2현금주문가능금액

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
