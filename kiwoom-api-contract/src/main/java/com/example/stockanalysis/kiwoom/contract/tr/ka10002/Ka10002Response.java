package com.example.stockanalysis.kiwoom.contract.tr.ka10002;

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
 * 주식거래원요청(ka10002) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10002Response implements KiwoomResult {

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("stk_nm")
    private String stkNm; // 종목명

    @JsonProperty("cur_prc")
    private String curPrc; // 현재가

    @JsonProperty("flu_smbol")
    private String fluSmbol; // 등락부호

    @JsonProperty("base_pric")
    private String basePric; // 기준가

    @JsonProperty("pred_pre")
    private String predPre; // 전일대비

    @JsonProperty("flu_rt")
    private String fluRt; // 등락율

    @JsonProperty("sel_trde_ori_nm_1")
    private String selTrdeOriNm1; // 매도거래원명1

    @JsonProperty("sel_trde_ori_1")
    private String selTrdeOri1; // 매도거래원1

    @JsonProperty("sel_trde_qty_1")
    private String selTrdeQty1; // 매도거래량1

    @JsonProperty("buy_trde_ori_nm_1")
    private String buyTrdeOriNm1; // 매수거래원명1

    @JsonProperty("buy_trde_ori_1")
    private String buyTrdeOri1; // 매수거래원1

    @JsonProperty("buy_trde_qty_1")
    private String buyTrdeQty1; // 매수거래량1

    @JsonProperty("sel_trde_ori_nm_2")
    private String selTrdeOriNm2; // 매도거래원명2

    @JsonProperty("sel_trde_ori_2")
    private String selTrdeOri2; // 매도거래원2

    @JsonProperty("sel_trde_qty_2")
    private String selTrdeQty2; // 매도거래량2

    @JsonProperty("buy_trde_ori_nm_2")
    private String buyTrdeOriNm2; // 매수거래원명2

    @JsonProperty("buy_trde_ori_2")
    private String buyTrdeOri2; // 매수거래원2

    @JsonProperty("buy_trde_qty_2")
    private String buyTrdeQty2; // 매수거래량2

    @JsonProperty("sel_trde_ori_nm_3")
    private String selTrdeOriNm3; // 매도거래원명3

    @JsonProperty("sel_trde_ori_3")
    private String selTrdeOri3; // 매도거래원3

    @JsonProperty("sel_trde_qty_3")
    private String selTrdeQty3; // 매도거래량3

    @JsonProperty("buy_trde_ori_nm_3")
    private String buyTrdeOriNm3; // 매수거래원명3

    @JsonProperty("buy_trde_ori_3")
    private String buyTrdeOri3; // 매수거래원3

    @JsonProperty("buy_trde_qty_3")
    private String buyTrdeQty3; // 매수거래량3

    @JsonProperty("sel_trde_ori_nm_4")
    private String selTrdeOriNm4; // 매도거래원명4

    @JsonProperty("sel_trde_ori_4")
    private String selTrdeOri4; // 매도거래원4

    @JsonProperty("sel_trde_qty_4")
    private String selTrdeQty4; // 매도거래량4

    @JsonProperty("buy_trde_ori_nm_4")
    private String buyTrdeOriNm4; // 매수거래원명4

    @JsonProperty("buy_trde_ori_4")
    private String buyTrdeOri4; // 매수거래원4

    @JsonProperty("buy_trde_qty_4")
    private String buyTrdeQty4; // 매수거래량4

    @JsonProperty("sel_trde_ori_nm_5")
    private String selTrdeOriNm5; // 매도거래원명5

    @JsonProperty("sel_trde_ori_5")
    private String selTrdeOri5; // 매도거래원5

    @JsonProperty("sel_trde_qty_5")
    private String selTrdeQty5; // 매도거래량5

    @JsonProperty("buy_trde_ori_nm_5")
    private String buyTrdeOriNm5; // 매수거래원명5

    @JsonProperty("buy_trde_ori_5")
    private String buyTrdeOri5; // 매수거래원5

    @JsonProperty("buy_trde_qty_5")
    private String buyTrdeQty5; // 매수거래량5

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
