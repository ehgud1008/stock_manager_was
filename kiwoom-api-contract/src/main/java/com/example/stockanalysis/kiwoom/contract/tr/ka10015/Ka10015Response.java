package com.example.stockanalysis.kiwoom.contract.tr.ka10015;

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
 * 일별거래상세요청(ka10015) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10015Response implements KiwoomResult {

    @JsonProperty("daly_trde_dtl")
    private List<DalyTrdeDtlItem> dalyTrdeDtl; // 일별거래상세

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
    public static class DalyTrdeDtlItem {

        @JsonProperty("dt")
        private String dt; // 일자

        @JsonProperty("close_pric")
        private String closePric; // 종가

        @JsonProperty("pred_pre_sig")
        private String predPreSig; // 전일대비기호

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("flu_rt")
        private String fluRt; // 등락율

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("trde_prica")
        private String trdePrica; // 거래대금

        @JsonProperty("bf_mkrt_trde_qty")
        private String bfMkrtTrdeQty; // 장전거래량

        @JsonProperty("bf_mkrt_trde_wght")
        private String bfMkrtTrdeWght; // 장전거래비중

        @JsonProperty("opmr_trde_qty")
        private String opmrTrdeQty; // 장중거래량

        @JsonProperty("opmr_trde_wght")
        private String opmrTrdeWght; // 장중거래비중

        @JsonProperty("af_mkrt_trde_qty")
        private String afMkrtTrdeQty; // 장후거래량

        @JsonProperty("af_mkrt_trde_wght")
        private String afMkrtTrdeWght; // 장후거래비중

        @JsonProperty("tot_3")
        private String tot3; // 합계3

        @JsonProperty("prid_trde_qty")
        private String pridTrdeQty; // 기간중거래량

        @JsonProperty("cntr_str")
        private String cntrStr; // 체결강도

        @JsonProperty("for_poss")
        private String forPoss; // 외인보유

        @JsonProperty("for_wght")
        private String forWght; // 외인비중

        @JsonProperty("for_netprps")
        private String forNetprps; // 외인순매수

        @JsonProperty("orgn_netprps")
        private String orgnNetprps; // 기관순매수

        @JsonProperty("ind_netprps")
        private String indNetprps; // 개인순매수

        @JsonProperty("frgn")
        private String frgn; // 외국계

        @JsonProperty("crd_remn_rt")
        private String crdRemnRt; // 신용잔고율

        @JsonProperty("prm")
        private String prm; // 프로그램

        @JsonProperty("bf_mkrt_trde_prica")
        private String bfMkrtTrdePrica; // 장전거래대금

        @JsonProperty("bf_mkrt_trde_prica_wght")
        private String bfMkrtTrdePricaWght; // 장전거래대금비중

        @JsonProperty("opmr_trde_prica")
        private String opmrTrdePrica; // 장중거래대금

        @JsonProperty("opmr_trde_prica_wght")
        private String opmrTrdePricaWght; // 장중거래대금비중

        @JsonProperty("af_mkrt_trde_prica")
        private String afMkrtTrdePrica; // 장후거래대금

        @JsonProperty("af_mkrt_trde_prica_wght")
        private String afMkrtTrdePricaWght; // 장후거래대금비중
    }
}
