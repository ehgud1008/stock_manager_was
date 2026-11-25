package com.example.stockanalysis.kiwoom.contract.tr.ka20068;

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
 * 대차거래추이요청(종목별)(ka20068) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka20068Response implements KiwoomResult {

    @JsonProperty("dbrt_trde_trnsn")
    private List<DbrtTrdeTrnsnItem> dbrtTrdeTrnsn; // 대차거래추이

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
    public static class DbrtTrdeTrnsnItem {

        @JsonProperty("dt")
        private String dt; // 일자

        @JsonProperty("dbrt_trde_cntrcnt")
        private String dbrtTrdeCntrcnt; // 대차거래체결주수

        @JsonProperty("dbrt_trde_rpy")
        private String dbrtTrdeRpy; // 대차거래상환주수

        @JsonProperty("dbrt_trde_irds")
        private String dbrtTrdeIrds; // 대차거래증감

        @JsonProperty("rmnd")
        private String rmnd; // 잔고주수

        @JsonProperty("remn_amt")
        private String remnAmt; // 잔고금액
    }
}
