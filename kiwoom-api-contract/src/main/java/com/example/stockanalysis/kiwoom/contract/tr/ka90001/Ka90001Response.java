package com.example.stockanalysis.kiwoom.contract.tr.ka90001;

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
 * 테마그룹별요청(ka90001) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka90001Response implements KiwoomResult {

    @JsonProperty("thema_grp")
    private List<ThemaGrpItem> themaGrp; // 테마그룹별

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
    public static class ThemaGrpItem {

        @JsonProperty("thema_grp_cd")
        private String themaGrpCd; // 테마그룹코드

        @JsonProperty("thema_nm")
        private String themaNm; // 테마명

        @JsonProperty("stk_num")
        private String stkNum; // 종목수

        @JsonProperty("flu_sig")
        private String fluSig; // 등락기호

        @JsonProperty("flu_rt")
        private String fluRt; // 등락율

        @JsonProperty("rising_stk_num")
        private String risingStkNum; // 상승종목수

        @JsonProperty("fall_stk_num")
        private String fallStkNum; // 하락종목수

        @JsonProperty("dt_prft_rt")
        private String dtPrftRt; // 기간수익률

        @JsonProperty("main_stk")
        private String mainStk; // 주요종목
    }
}
