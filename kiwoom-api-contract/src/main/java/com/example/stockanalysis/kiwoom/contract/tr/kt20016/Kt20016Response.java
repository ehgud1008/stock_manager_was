package com.example.stockanalysis.kiwoom.contract.tr.kt20016;

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
 * 신용융자 가능종목요청(kt20016) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt20016Response implements KiwoomResult {

    @JsonProperty("crd_loan_able")
    private String crdLoanAble; // 신용융자가능여부

    @JsonProperty("crd_loan_pos_stk")
    private List<CrdLoanPosStkItem> crdLoanPosStk; // 신용융자가능종목

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
    public static class CrdLoanPosStkItem {

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("crd_assr_rt")
        private String crdAssrRt; // 신용보증금율

        @JsonProperty("repl_pric")
        private String replPric; // 대용가

        @JsonProperty("pred_close_pric")
        private String predClosePric; // 전일종가

        @JsonProperty("crd_limit_over_yn")
        private String crdLimitOverYn; // 신용한도초과여부

        @JsonProperty("crd_limit_over_txt")
        private String crdLimitOverTxt; // 신용한도초과
    }
}
