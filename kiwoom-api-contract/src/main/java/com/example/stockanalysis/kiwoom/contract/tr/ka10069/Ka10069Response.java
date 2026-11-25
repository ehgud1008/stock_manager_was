package com.example.stockanalysis.kiwoom.contract.tr.ka10069;

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
 * 대차거래상위10종목요청(ka10069) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10069Response implements KiwoomResult {

    @JsonProperty("dbrt_trde_cntrcnt_sum")
    private String dbrtTrdeCntrcntSum; // 대차거래체결주수합

    @JsonProperty("dbrt_trde_rpy_sum")
    private String dbrtTrdeRpySum; // 대차거래상환주수합

    @JsonProperty("rmnd_sum")
    private String rmndSum; // 잔고주수합

    @JsonProperty("remn_amt_sum")
    private String remnAmtSum; // 잔고금액합

    @JsonProperty("dbrt_trde_cntrcnt_rt")
    private String dbrtTrdeCntrcntRt; // 대차거래체결주수비율

    @JsonProperty("dbrt_trde_rpy_rt")
    private String dbrtTrdeRpyRt; // 대차거래상환주수비율

    @JsonProperty("rmnd_rt")
    private String rmndRt; // 잔고주수비율

    @JsonProperty("remn_amt_rt")
    private String remnAmtRt; // 잔고금액비율

    @JsonProperty("dbrt_trde_upper_10stk")
    private List<DbrtTrdeUpper10stkItem> dbrtTrdeUpper10stk; // 대차거래상위10종목

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
    public static class DbrtTrdeUpper10stkItem {

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("dbrt_trde_cntrcnt")
        private String dbrtTrdeCntrcnt; // 대차거래체결주수

        @JsonProperty("dbrt_trde_rpy")
        private String dbrtTrdeRpy; // 대차거래상환주수

        @JsonProperty("rmnd")
        private String rmnd; // 잔고주수

        @JsonProperty("remn_amt")
        private String remnAmt; // 잔고금액
    }
}
