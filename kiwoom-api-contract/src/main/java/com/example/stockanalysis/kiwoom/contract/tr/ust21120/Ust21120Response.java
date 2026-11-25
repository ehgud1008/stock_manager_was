package com.example.stockanalysis.kiwoom.contract.tr.ust21120;

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
 * 통화별 예수금 및 증권 평가금현황(ust21120) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21120Response implements KiwoomResult {

    @JsonProperty("won_entr")
    private String wonEntr; // 예수금

    @JsonProperty("aset_evlt_amt")
    private String asetEvltAmt; // 원화추정자산

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

        @JsonProperty("fx_entr")
        private String fxEntr; // 외화예수금

        @JsonProperty("evlt_amt")
        private String evltAmt; // 해외증권평가금

        @JsonProperty("crnc_rt")
        private String crncRt; // 기준환율

        @JsonProperty("chg_entr")
        private String chgEntr; // 환전예수금

        @JsonProperty("chg_evlt_amt")
        private String chgEvltAmt; // 환전평가금

        @JsonProperty("evlt_amt_wght")
        private String evltAmtWght; // 평가금액비중
    }
}
