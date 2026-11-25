package com.example.stockanalysis.kiwoom.contract.tr.ka10072;

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
 * 일자별종목별실현손익요청_일자(ka10072) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10072Response implements KiwoomResult {

    @JsonProperty("dt_stk_div_rlzt_pl")
    private List<DtStkDivRlztPlItem> dtStkDivRlztPl; // 일자별종목별실현손익

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
    public static class DtStkDivRlztPlItem {

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("cntr_qty")
        private String cntrQty; // 체결량

        @JsonProperty("buy_uv")
        private String buyUv; // 매입단가

        @JsonProperty("cntr_pric")
        private String cntrPric; // 체결가

        @JsonProperty("tdy_sel_pl")
        private String tdySelPl; // 당일매도손익

        @JsonProperty("pl_rt")
        private String plRt; // 손익율

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("tdy_trde_cmsn")
        private String tdyTrdeCmsn; // 당일매매수수료

        @JsonProperty("tdy_trde_tax")
        private String tdyTrdeTax; // 당일매매세금

        @JsonProperty("wthd_alowa")
        private String wthdAlowa; // 인출가능금액

        @JsonProperty("loan_dt")
        private String loanDt; // 대출일

        @JsonProperty("crd_tp")
        private String crdTp; // 신용구분

        @JsonProperty("stk_cd_1")
        private String stkCd1; // 종목코드1

        @JsonProperty("tdy_sel_pl_1")
        private String tdySelPl1; // 당일매도손익1
    }
}
