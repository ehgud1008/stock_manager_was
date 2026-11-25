package com.example.stockanalysis.kiwoom.contract.tr.ka40007;

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
 * ETF시간대별체결요청(ka40007) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka40007Response implements KiwoomResult {

    @JsonProperty("stk_cls")
    private String stkCls; // 종목분류

    @JsonProperty("stk_nm")
    private String stkNm; // 종목명

    @JsonProperty("etfobjt_idex_nm")
    private String etfobjtIdexNm; // ETF대상지수명

    @JsonProperty("etfobjt_idex_cd")
    private String etfobjtIdexCd; // ETF대상지수코드

    @JsonProperty("objt_idex_pre_rt")
    private String objtIdexPreRt; // 대상지수대비율

    @JsonProperty("wonju_pric")
    private String wonjuPric; // 원주가격

    @JsonProperty("etftisl_cntr_array")
    private List<EtftislCntrArrayItem> etftislCntrArray; // ETF시간대별체결배열

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
    public static class EtftislCntrArrayItem {

        @JsonProperty("cntr_tm")
        private String cntrTm; // 체결시간

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("pre_sig")
        private String preSig; // 대비기호

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("stex_tp")
        private String stexTp; // 거래소구분
    }
}
