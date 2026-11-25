package com.example.stockanalysis.kiwoom.contract.tr.ka10038;

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
 * 종목별증권사순위요청(ka10038) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10038Response implements KiwoomResult {

    @JsonProperty("rank_1")
    private String rank1; // 기간별 누적 매수량

    @JsonProperty("rank_2")
    private String rank2; // 기간별 누적 매도량

    @JsonProperty("rank_3")
    private String rank3; // 기간별 누적 순매수

    @JsonProperty("prid_trde_qty")
    private String pridTrdeQty; // 기간중거래량

    @JsonProperty("stk_sec_rank")
    private List<StkSecRankItem> stkSecRank; // 종목별증권사순위

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
    public static class StkSecRankItem {

        @JsonProperty("rank")
        private String rank; // 순위

        @JsonProperty("mmcm_nm")
        private String mmcmNm; // 회원사명

        @JsonProperty("buy_qty")
        private String buyQty; // 매수수량

        @JsonProperty("sell_qty")
        private String sellQty; // 매도수량

        @JsonProperty("acc_netprps_qty")
        private String accNetprpsQty; // 누적순매수수량
    }
}
