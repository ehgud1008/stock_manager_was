package com.example.stockanalysis.kiwoom.contract.tr.ka50087;

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
 * 금현물예상체결(ka50087) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka50087Response implements KiwoomResult {

    @JsonProperty("gold_expt_exec")
    private List<GoldExptExecItem> goldExptExec; // 금현물예상체결

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
    public static class GoldExptExecItem {

        @JsonProperty("exp_cntr_pric")
        private String expCntrPric; // 예상 체결가

        @JsonProperty("exp_pred_pre")
        private String expPredPre; // 예상 체결가 전일대비

        @JsonProperty("exp_flu_rt")
        private String expFluRt; // 예상 체결가 등락율

        @JsonProperty("exp_acc_trde_qty")
        private String expAccTrdeQty; // 예상 체결 수량(누적)

        @JsonProperty("exp_cntr_trde_qty")
        private String expCntrTrdeQty; // 예상 체결 수량

        @JsonProperty("exp_tm")
        private String expTm; // 예상 체결 시간

        @JsonProperty("exp_pre_sig")
        private String expPreSig; // 예상 체결가 전일대비기호

        @JsonProperty("stex_tp")
        private String stexTp; // 거래소 구분
    }
}
