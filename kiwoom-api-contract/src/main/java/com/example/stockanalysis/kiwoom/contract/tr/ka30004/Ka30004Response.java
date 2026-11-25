package com.example.stockanalysis.kiwoom.contract.tr.ka30004;

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
 * ELW괴리율요청(ka30004) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka30004Response implements KiwoomResult {

    @JsonProperty("elwdispty_rt")
    private List<ElwdisptyRtItem> elwdisptyRt; // ELW괴리율

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
    public static class ElwdisptyRtItem {

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("isscomp_nm")
        private String isscompNm; // 발행사명

        @JsonProperty("sqnc")
        private String sqnc; // 회차

        @JsonProperty("base_aset_nm")
        private String baseAsetNm; // 기초자산명

        @JsonProperty("rght_tp")
        private String rghtTp; // 권리구분

        @JsonProperty("dispty_rt")
        private String disptyRt; // 괴리율

        @JsonProperty("basis")
        private String basis; // 베이시스

        @JsonProperty("srvive_dys")
        private String srviveDys; // 잔존일수

        @JsonProperty("theory_pric")
        private String theoryPric; // 이론가

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("pre_tp")
        private String preTp; // 대비구분

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("flu_rt")
        private String fluRt; // 등락율

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명
    }
}
