package com.example.stockanalysis.kiwoom.contract.tr.ka10019;

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
 * 가격급등락요청(ka10019) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10019Response implements KiwoomResult {

    @JsonProperty("pric_jmpflu")
    private List<PricJmpfluItem> pricJmpflu; // 가격급등락

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
    public static class PricJmpfluItem {

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_cls")
        private String stkCls; // 종목분류

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("pred_pre_sig")
        private String predPreSig; // 전일대비기호

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("flu_rt")
        private String fluRt; // 등락률

        @JsonProperty("base_pric")
        private String basePric; // 기준가

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("base_pre")
        private String basePre; // 기준대비

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("jmp_rt")
        private String jmpRt; // 급등률
    }
}
