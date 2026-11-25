package com.example.stockanalysis.kiwoom.contract.tr.kt00002;

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
 * 일별추정예탁자산현황요청(kt00002) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt00002Response implements KiwoomResult {

    @JsonProperty("daly_prsm_dpst_aset_amt_prst")
    private List<DalyPrsmDpstAsetAmtPrstItem> dalyPrsmDpstAsetAmtPrst; // 일별추정예탁자산현황

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
    public static class DalyPrsmDpstAsetAmtPrstItem {

        @JsonProperty("dt")
        private String dt; // 일자

        @JsonProperty("entr")
        private String entr; // 예수금

        @JsonProperty("grnt_use_amt")
        private String grntUseAmt; // 담보대출금

        @JsonProperty("crd_loan")
        private String crdLoan; // 신용융자금

        @JsonProperty("ls_grnt")
        private String lsGrnt; // 대주담보금

        @JsonProperty("repl_amt")
        private String replAmt; // 대용금

        @JsonProperty("prsm_dpst_aset_amt")
        private String prsmDpstAsetAmt; // 추정예탁자산

        @JsonProperty("prsm_dpst_aset_amt_bncr_skip")
        private String prsmDpstAsetAmtBncrSkip; // 추정예탁자산수익증권제외
    }
}
