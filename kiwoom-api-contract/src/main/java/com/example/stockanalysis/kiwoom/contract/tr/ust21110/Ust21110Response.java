package com.example.stockanalysis.kiwoom.contract.tr.ust21110;

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
 * 해외주식 예수금(ust21110) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21110Response implements KiwoomResult {

    @JsonProperty("krw_entra")
    private String krwEntra; // 원화예수금

    @JsonProperty("ch_uncla")
    private String chUncla; // 현금미수금

    @JsonProperty("etc_loana")
    private String etcLoana; // 기타대여금

    @JsonProperty("result_list")
    private List<ResultListItem> resultList; // 결과리스트

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
    public static class ResultListItem {

        @JsonProperty("crnc_code")
        private String crncCode; // 통화코드

        @JsonProperty("crnc_nm")
        private String crncNm; // 통화명

        @JsonProperty("fc_entra")
        private String fcEntra; // 외화예수금

        @JsonProperty("fc_pymn_alowa")
        private String fcPymnAlowa; // 외화출금가능금액

        @JsonProperty("futr_repl_profa")
        private String futrReplProfa; // 선물대용증거금

        @JsonProperty("fc_booka")
        private String fcBooka; // 외화장부금액

        @JsonProperty("fc_ord_alowa")
        private String fcOrdAlowa; // 외화주문가능금액

        @JsonProperty("futr_profa_booka")
        private String futrProfaBooka; // 선물증거금장부금액

        @JsonProperty("fc_ch_uncla")
        private String fcChUncla; // 외화현금미수금

        @JsonProperty("fc_etc_loana")
        private String fcEtcLoana; // 외화기타대여금
    }
}
