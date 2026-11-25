package com.example.stockanalysis.kiwoom.contract.tr.ka10077;

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
 * 당일실현손익상세요청(ka10077) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10077Response implements KiwoomResult {

    @JsonProperty("tdy_rlzt_pl")
    private String tdyRlztPl; // 당일실현손익

    @JsonProperty("tdy_rlzt_pl_dtl")
    private List<TdyRlztPlDtlItem> tdyRlztPlDtl; // 당일실현손익상세

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
    public static class TdyRlztPlDtlItem {

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

        @JsonProperty("tdy_trde_cmsn")
        private String tdyTrdeCmsn; // 당일매매수수료

        @JsonProperty("tdy_trde_tax")
        private String tdyTrdeTax; // 당일매매세금

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드
    }
}
