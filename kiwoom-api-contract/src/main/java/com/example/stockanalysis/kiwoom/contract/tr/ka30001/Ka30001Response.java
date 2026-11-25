package com.example.stockanalysis.kiwoom.contract.tr.ka30001;

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
 * ELW가격급등락요청(ka30001) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka30001Response implements KiwoomResult {

    @JsonProperty("base_pric_tm")
    private String basePricTm; // 기준가시간

    @JsonProperty("elwpric_jmpflu")
    private List<ElwpricJmpfluItem> elwpricJmpflu; // ELW가격급등락

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
    public static class ElwpricJmpfluItem {

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("rank")
        private String rank; // 순위

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("pre_sig")
        private String preSig; // 대비기호

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("trde_end_elwbase_pric")
        private String trdeEndElwbasePric; // 거래종료ELW기준가

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("base_pre")
        private String basePre; // 기준대비

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("jmp_rt")
        private String jmpRt; // 급등율
    }
}
