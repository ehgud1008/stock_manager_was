package com.example.stockanalysis.kiwoom.contract.tr.ka10040;

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
 * 당일주요거래원요청(ka10040) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10040Response implements KiwoomResult {

    @JsonProperty("sel_trde_ori_irds_1")
    private String selTrdeOriIrds1; // 매도거래원별증감1

    @JsonProperty("sel_trde_ori_qty_1")
    private String selTrdeOriQty1; // 매도거래원수량1

    @JsonProperty("sel_trde_ori_1")
    private String selTrdeOri1; // 매도거래원1

    @JsonProperty("sel_trde_ori_cd_1")
    private String selTrdeOriCd1; // 매도거래원코드1

    @JsonProperty("buy_trde_ori_1")
    private String buyTrdeOri1; // 매수거래원1

    @JsonProperty("buy_trde_ori_cd_1")
    private String buyTrdeOriCd1; // 매수거래원코드1

    @JsonProperty("buy_trde_ori_qty_1")
    private String buyTrdeOriQty1; // 매수거래원수량1

    @JsonProperty("buy_trde_ori_irds_1")
    private String buyTrdeOriIrds1; // 매수거래원별증감1

    @JsonProperty("sel_trde_ori_irds_2")
    private String selTrdeOriIrds2; // 매도거래원별증감2

    @JsonProperty("sel_trde_ori_qty_2")
    private String selTrdeOriQty2; // 매도거래원수량2

    @JsonProperty("sel_trde_ori_2")
    private String selTrdeOri2; // 매도거래원2

    @JsonProperty("sel_trde_ori_cd_2")
    private String selTrdeOriCd2; // 매도거래원코드2

    @JsonProperty("buy_trde_ori_2")
    private String buyTrdeOri2; // 매수거래원2

    @JsonProperty("buy_trde_ori_cd_2")
    private String buyTrdeOriCd2; // 매수거래원코드2

    @JsonProperty("buy_trde_ori_qty_2")
    private String buyTrdeOriQty2; // 매수거래원수량2

    @JsonProperty("buy_trde_ori_irds_2")
    private String buyTrdeOriIrds2; // 매수거래원별증감2

    @JsonProperty("sel_trde_ori_irds_3")
    private String selTrdeOriIrds3; // 매도거래원별증감3

    @JsonProperty("sel_trde_ori_qty_3")
    private String selTrdeOriQty3; // 매도거래원수량3

    @JsonProperty("sel_trde_ori_3")
    private String selTrdeOri3; // 매도거래원3

    @JsonProperty("sel_trde_ori_cd_3")
    private String selTrdeOriCd3; // 매도거래원코드3

    @JsonProperty("buy_trde_ori_3")
    private String buyTrdeOri3; // 매수거래원3

    @JsonProperty("buy_trde_ori_cd_3")
    private String buyTrdeOriCd3; // 매수거래원코드3

    @JsonProperty("buy_trde_ori_qty_3")
    private String buyTrdeOriQty3; // 매수거래원수량3

    @JsonProperty("buy_trde_ori_irds_3")
    private String buyTrdeOriIrds3; // 매수거래원별증감3

    @JsonProperty("sel_trde_ori_irds_4")
    private String selTrdeOriIrds4; // 매도거래원별증감4

    @JsonProperty("sel_trde_ori_qty_4")
    private String selTrdeOriQty4; // 매도거래원수량4

    @JsonProperty("sel_trde_ori_4")
    private String selTrdeOri4; // 매도거래원4

    @JsonProperty("sel_trde_ori_cd_4")
    private String selTrdeOriCd4; // 매도거래원코드4

    @JsonProperty("buy_trde_ori_4")
    private String buyTrdeOri4; // 매수거래원4

    @JsonProperty("buy_trde_ori_cd_4")
    private String buyTrdeOriCd4; // 매수거래원코드4

    @JsonProperty("buy_trde_ori_qty_4")
    private String buyTrdeOriQty4; // 매수거래원수량4

    @JsonProperty("buy_trde_ori_irds_4")
    private String buyTrdeOriIrds4; // 매수거래원별증감4

    @JsonProperty("sel_trde_ori_irds_5")
    private String selTrdeOriIrds5; // 매도거래원별증감5

    @JsonProperty("sel_trde_ori_qty_5")
    private String selTrdeOriQty5; // 매도거래원수량5

    @JsonProperty("sel_trde_ori_5")
    private String selTrdeOri5; // 매도거래원5

    @JsonProperty("sel_trde_ori_cd_5")
    private String selTrdeOriCd5; // 매도거래원코드5

    @JsonProperty("buy_trde_ori_5")
    private String buyTrdeOri5; // 매수거래원5

    @JsonProperty("buy_trde_ori_cd_5")
    private String buyTrdeOriCd5; // 매수거래원코드5

    @JsonProperty("buy_trde_ori_qty_5")
    private String buyTrdeOriQty5; // 매수거래원수량5

    @JsonProperty("buy_trde_ori_irds_5")
    private String buyTrdeOriIrds5; // 매수거래원별증감5

    @JsonProperty("frgn_sel_prsm_sum_chang")
    private String frgnSelPrsmSumChang; // 외국계매도추정합변동

    @JsonProperty("frgn_sel_prsm_sum")
    private String frgnSelPrsmSum; // 외국계매도추정합

    @JsonProperty("frgn_buy_prsm_sum")
    private String frgnBuyPrsmSum; // 외국계매수추정합

    @JsonProperty("frgn_buy_prsm_sum_chang")
    private String frgnBuyPrsmSumChang; // 외국계매수추정합변동

    @JsonProperty("tdy_main_trde_ori")
    private List<TdyMainTrdeOriItem> tdyMainTrdeOri; // 당일주요거래원

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
    public static class TdyMainTrdeOriItem {

        @JsonProperty("sel_scesn_tm")
        private String selScesnTm; // 매도이탈시간

        @JsonProperty("sell_qty")
        private String sellQty; // 매도수량

        @JsonProperty("sel_upper_scesn_ori")
        private String selUpperScesnOri; // 매도상위이탈원

        @JsonProperty("buy_scesn_tm")
        private String buyScesnTm; // 매수이탈시간

        @JsonProperty("buy_qty")
        private String buyQty; // 매수수량

        @JsonProperty("buy_upper_scesn_ori")
        private String buyUpperScesnOri; // 매수상위이탈원

        @JsonProperty("qry_dt")
        private String qryDt; // 조회일자

        @JsonProperty("qry_tm")
        private String qryTm; // 조회시간
    }
}
