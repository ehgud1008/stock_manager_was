package com.example.stockanalysis.kiwoom.contract.tr.ka10061;

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
 * 종목별투자자기관별합계요청(ka10061) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10061Response implements KiwoomResult {

    @JsonProperty("stk_invsr_orgn_tot")
    private List<StkInvsrOrgnTotItem> stkInvsrOrgnTot; // 종목별투자자기관별합계

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
    public static class StkInvsrOrgnTotItem {

        @JsonProperty("ind_invsr")
        private String indInvsr; // 개인투자자

        @JsonProperty("frgnr_invsr")
        private String frgnrInvsr; // 외국인투자자

        @JsonProperty("orgn")
        private String orgn; // 기관계

        @JsonProperty("fnnc_invt")
        private String fnncInvt; // 금융투자

        @JsonProperty("insrnc")
        private String insrnc; // 보험

        @JsonProperty("invtrt")
        private String invtrt; // 투신

        @JsonProperty("etc_fnnc")
        private String etcFnnc; // 기타금융

        @JsonProperty("bank")
        private String bank; // 은행

        @JsonProperty("penfnd_etc")
        private String penfndEtc; // 연기금등

        @JsonProperty("samo_fund")
        private String samoFund; // 사모펀드

        @JsonProperty("natn")
        private String natn; // 국가

        @JsonProperty("etc_corp")
        private String etcCorp; // 기타법인

        @JsonProperty("natfor")
        private String natfor; // 내외국인
    }
}
