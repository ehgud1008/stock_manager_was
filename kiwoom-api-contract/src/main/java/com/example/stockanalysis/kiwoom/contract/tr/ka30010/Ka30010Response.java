package com.example.stockanalysis.kiwoom.contract.tr.ka30010;

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
 * ELW잔량순위요청(ka30010) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka30010Response implements KiwoomResult {

    @JsonProperty("elwreq_rank")
    private List<ElwreqRankItem> elwreqRank; // ELW잔량순위

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
    public static class ElwreqRankItem {

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
        private String fluRt; // 등락률

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("sel_req")
        private String selReq; // 매도잔량

        @JsonProperty("buy_req")
        private String buyReq; // 매수잔량

        @JsonProperty("netprps_req")
        private String netprpsReq; // 순매수잔량

        @JsonProperty("trde_prica")
        private String trdePrica; // 거래대금
    }
}
