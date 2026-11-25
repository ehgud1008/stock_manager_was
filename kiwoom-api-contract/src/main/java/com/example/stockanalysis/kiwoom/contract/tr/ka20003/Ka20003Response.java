package com.example.stockanalysis.kiwoom.contract.tr.ka20003;

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
 * 전업종지수요청(ka20003) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka20003Response implements KiwoomResult {

    @JsonProperty("all_inds_idex")
    private List<AllIndsIdexItem> allIndsIdex; // 전업종지수

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
    public static class AllIndsIdexItem {

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("pre_sig")
        private String preSig; // 대비기호

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("flu_rt")
        private String fluRt; // 등락률

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("wght")
        private String wght; // 비중

        @JsonProperty("trde_prica")
        private String trdePrica; // 거래대금

        @JsonProperty("upl")
        private String upl; // 상한

        @JsonProperty("rising")
        private String rising; // 상승

        @JsonProperty("stdns")
        private String stdns; // 보합

        @JsonProperty("fall")
        private String fall; // 하락

        @JsonProperty("lst")
        private String lst; // 하한

        @JsonProperty("flo_stk_num")
        private String floStkNum; // 상장종목수
    }
}
