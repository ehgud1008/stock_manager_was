package com.example.stockanalysis.kiwoom.contract.tr.ka00198;

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
 * 실시간종목조회순위(ka00198) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka00198Response implements KiwoomResult {

    @JsonProperty("item_inq_rank")
    private List<ItemInqRankItem> itemInqRank; // 실시간종목조회순위

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
    public static class ItemInqRankItem {

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("bigd_rank")
        private String bigdRank; // 빅데이터 순위

        @JsonProperty("rank_chg")
        private String rankChg; // 순위 등락

        @JsonProperty("rank_chg_sign")
        private String rankChgSign; // 순위 등락 부호

        @JsonProperty("past_curr_prc")
        private String pastCurrPrc; // 과거 현재가

        @JsonProperty("base_comp_sign")
        private String baseCompSign; // 기준가 대비 부호

        @JsonProperty("base_comp_chgr")
        private String baseCompChgr; // 기준가 대비 등락율

        @JsonProperty("prev_base_sign")
        private String prevBaseSign; // 직전 기준 대비 부호

        @JsonProperty("prev_base_chgr")
        private String prevBaseChgr; // 직전 기준 대비 등락율

        @JsonProperty("dt")
        private String dt; // 일자

        @JsonProperty("tm")
        private String tm; // 시간

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드
    }
}
