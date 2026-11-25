package com.example.stockanalysis.kiwoom.contract.tr.ka10062;

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
 * 동일순매매순위요청(ka10062) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10062Response implements KiwoomResult {

    @JsonProperty("eql_nettrde_rank")
    private List<EqlNettrdeRankItem> eqlNettrdeRank; // 동일순매매순위

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
    public static class EqlNettrdeRankItem {

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("rank")
        private String rank; // 순위

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("pre_sig")
        private String preSig; // 대비기호

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("flu_rt")
        private String fluRt; // 등락율

        @JsonProperty("acc_trde_qty")
        private String accTrdeQty; // 거래량

        @JsonProperty("orgn_nettrde_qty")
        private String orgnNettrdeQty; // 기관순매매수량

        @JsonProperty("orgn_nettrde_amt")
        private String orgnNettrdeAmt; // 기관순매매금액

        @JsonProperty("orgn_nettrde_avg_pric")
        private String orgnNettrdeAvgPric; // 기관순매매평균가

        @JsonProperty("for_nettrde_qty")
        private String forNettrdeQty; // 외인순매매수량

        @JsonProperty("for_nettrde_amt")
        private String forNettrdeAmt; // 외인순매매금액

        @JsonProperty("for_nettrde_avg_pric")
        private String forNettrdeAvgPric; // 외인순매매평균가

        @JsonProperty("nettrde_qty")
        private String nettrdeQty; // 순매매수량

        @JsonProperty("nettrde_amt")
        private String nettrdeAmt; // 순매매금액
    }
}
