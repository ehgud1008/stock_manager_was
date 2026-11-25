package com.example.stockanalysis.kiwoom.contract.tr.ka10054;

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
 * 변동성완화장치발동종목요청(ka10054) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10054Response implements KiwoomResult {

    @JsonProperty("motn_stk")
    private List<MotnStkItem> motnStk; // 발동종목

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
    public static class MotnStkItem {

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("acc_trde_qty")
        private String accTrdeQty; // 누적거래량

        @JsonProperty("motn_pric")
        private String motnPric; // 발동가격

        @JsonProperty("dynm_dispty_rt")
        private String dynmDisptyRt; // 동적괴리율

        @JsonProperty("trde_cntr_proc_time")
        private String trdeCntrProcTime; // 매매체결처리시각

        @JsonProperty("virelis_time")
        private String virelisTime; // VI해제시각

        @JsonProperty("viaplc_tp")
        private String viaplcTp; // VI적용구분

        @JsonProperty("dynm_stdpc")
        private String dynmStdpc; // 동적기준가격

        @JsonProperty("static_stdpc")
        private String staticStdpc; // 정적기준가격

        @JsonProperty("static_dispty_rt")
        private String staticDisptyRt; // 정적괴리율

        @JsonProperty("open_pric_pre_flu_rt")
        private String openPricPreFluRt; // 시가대비등락률

        @JsonProperty("vimotn_cnt")
        private String vimotnCnt; // VI발동횟수

        @JsonProperty("stex_tp")
        private String stexTp; // 거래소구분
    }
}
